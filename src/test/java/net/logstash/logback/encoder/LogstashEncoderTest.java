/*
 * Copyright 2013-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.logstash.logback.encoder;

import static net.logstash.logback.marker.Markers.append;
import static net.logstash.logback.marker.Markers.appendEntries;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import net.logstash.logback.composite.AbstractFormattedTimestampJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders;
import net.logstash.logback.composite.loggingevent.mdc.BooleanMdcEntryWriter;
import net.logstash.logback.composite.loggingevent.mdc.DoubleMdcEntryWriter;
import net.logstash.logback.composite.loggingevent.mdc.LongMdcEntryWriter;
import net.logstash.logback.decorate.JsonFactoryDecorator;
import net.logstash.logback.fieldnames.LogstashCommonFieldNames;
import net.logstash.logback.fieldnames.ShortenedFieldNames;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.pattern.TargetLengthBasedClassNameAbbreviator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.Context;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.event.KeyValuePair;
import org.slf4j.helpers.NOPMDCAdapter;

public class LogstashEncoderTest {

    private static final JsonFactory FACTORY = new MappingJsonFactory().enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);
    private static final ObjectMapper MAPPER = new ObjectMapper(FACTORY);
    private final LogstashEncoder encoder = new LogstashEncoder();

    private Instant now;

    @BeforeEach
    public void setup() {
        now = Instant.now();
    }

    @Test
    public void basicsAreIncluded_logback12() throws Exception {
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);

        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        verifyBasics(now, node);
    }

    protected void verifyBasics(Instant timestamp, JsonNode node) {
        assertThat(node.get("@timestamp").textValue()).isEqualTo(
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(TimeZone.getDefault().toZoneId()).format(timestamp));
        assertThat(node.get("@version").textValue()).isEqualTo("1");
        assertThat(node.get("logger_name").textValue()).isEqualTo("LoggerName");
        assertThat(node.get("thread_name").textValue()).isEqualTo("ThreadName");
        assertThat(node.get("message").textValue()).isEqualTo("My message");
        assertThat(node.get("level").textValue()).isEqualTo("ERROR");
        assertThat(node.get("level_value").intValue()).isEqualTo(40000);
    }

    @Test
    public void basicsAreIncludedWithShortenedNames() throws Exception {
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);

        encoder.setFieldNames(new ShortenedFieldNames());
        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("@timestamp").textValue()).isEqualTo(
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(TimeZone.getDefault().toZoneId()).format(now));
        assertThat(node.get("@version").textValue()).isEqualTo("1");
        assertThat(node.get("logger").textValue()).isEqualTo("LoggerName");
        assertThat(node.get("thread").textValue()).isEqualTo("ThreadName");
        assertThat(node.get("message").textValue()).isEqualTo("My message");
        assertThat(node.get("level").textValue()).isEqualTo("ERROR");
        assertThat(node.get("levelVal").intValue()).isEqualTo(40000);
    }

    @Test
    public void customDecorators() {
        encoder.stop();
        encoder.setJsonFactoryDecorator(new JsonFactoryDecorator() {

            @Override
            public JsonFactory decorate(JsonFactory factory) {
                return factory.disable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
            }
        });

        encoder.setJsonGeneratorDecorator(JsonGenerator::useDefaultPrettyPrinter);

        encoder.start();

        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);

        byte[] encoded = encoder.encode(event);

        String output = new String(encoded, StandardCharsets.UTF_8);

        assertThat(output).isEqualTo(String.format("{%n" + "  @timestamp : \""
                + DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(TimeZone.getDefault().toZoneId()).format(now)
                + "\",%n" + "  @version : \"1\",%n" + "  message : \"My message\",%n"
                + "  logger_name : \"LoggerName\",%n" + "  thread_name : \"ThreadName\",%n" + "  level : \"ERROR\",%n"
                + "  level_value : 40000%n" + "}%n"));
    }

    @Test
    public void loggerNameIsShortenedProperly() throws Exception {
        final int length = 36;
        final String shortenedLoggerName = new TargetLengthBasedClassNameAbbreviator(length)
                .abbreviate(DateTimeFormatter.class.getCanonicalName());

        LoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        event.setLoggerName(DateTimeFormatter.class.getCanonicalName());

        encoder.setFieldNames(new ShortenedFieldNames());
        encoder.setShortenedLoggerNameLength(length);
        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("@timestamp").textValue()).isEqualTo(
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(TimeZone.getDefault().toZoneId()).format(now));
        assertThat(node.get("@version").textValue()).isEqualTo("1");
        assertThat(node.get("logger").textValue()).isEqualTo(shortenedLoggerName);
        assertThat(node.get("thread").textValue()).isEqualTo("ThreadName");
        assertThat(node.get("message").textValue()).isEqualTo("My message");
        assertThat(node.get("level").textValue()).isEqualTo("ERROR");
        assertThat(node.get("levelVal").intValue()).isEqualTo(40000);
    }

    @Test
    public void includingThrowableProxyIncludesStackTrace() throws Exception {
        ThrowableProxy throwableProxy = new ThrowableProxy(new Exception("My goodness"));

        LoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        event.setThrowableProxy(throwableProxy);

        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("stack_trace").textValue()).isEqualTo(ThrowableProxyUtil.asString(throwableProxy));
    }

    @Test
    public void mdcAllIncluded() throws Exception {
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("thing_one", "One");
        mdcMap.put("thing_two", "Three");

        LoggingEvent event = mockBasicILoggingEvent(Level.ERROR, "My message", mdcMap);

        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("thing_one").textValue()).isEqualTo("One");
        assertThat(node.get("thing_two").textValue()).isEqualTo("Three");
    }

    @Test
    public void mdcSomeIncluded() throws Exception {
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("thing_one", "One");
        mdcMap.put("thing_two", "Three");

        LoggingEvent event = mockBasicILoggingEvent(Level.ERROR, "My message", mdcMap);

        encoder.addIncludeMdcKeyName("thing_one");

        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("thing_one").textValue()).isEqualTo("One");
        assertThat(node.get("thing_two")).isNull();
    }

    @Test
    public void mdcSomeExcluded() throws Exception {
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("thing_one", "One");
        mdcMap.put("thing_two", "Three");

        LoggingEvent event = mockBasicILoggingEvent(Level.ERROR, "My message", mdcMap);
        encoder.addExcludeMdcKeyName("thing_two");

        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("thing_one").textValue()).isEqualTo("One");
        assertThat(node.get("thing_two")).isNull();
    }

    @Test
    public void mdcNoneIncluded() throws Exception {
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("thing_one", "One");
        mdcMap.put("thing_two", "Three");

        LoggingEvent event = mockBasicILoggingEvent(Level.ERROR, "My message", mdcMap);

        encoder.setIncludeMdc(false);
        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("thing_one")).isNull();
        assertThat(node.get("thing_two")).isNull();
    }

    @Test
    public void propertiesInMDCAreIncludedInSubObject() throws Exception {
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("thing_one", "One");
        mdcMap.put("thing_two", "Three");

        LoggingEvent event = mockBasicILoggingEvent(Level.ERROR, "My message", mdcMap);

        encoder.getFieldNames().setMdc("mdc");
        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("mdc").get("thing_one").textValue()).isEqualTo("One");
        assertThat(node.get("mdc").get("thing_two").textValue()).isEqualTo("Three");
    }

    @Test
    public void nullMDCDoesNotCauseEverythingToBlowUp() {
        LoggingEvent event = mockBasicILoggingEvent(Level.ERROR, "My message", null);
        LoggerContext lc = new LoggerContext();
        lc.setMDCAdapter(new NOPMDCAdapter());
        event.setLoggerContext(lc);

        encoder.start();
        assertThatCode(() -> encoder.encode(event)).doesNotThrowAnyException();
    }

    @Test
    public void mdcEntryWriters() throws Exception {
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("long", "4711");
        mdcMap.put("double", "2.71828");
        mdcMap.put("bool", "true");
        mdcMap.put("default", "string");

        LoggingEvent event = mockBasicILoggingEvent(Level.ERROR, "My message", mdcMap);

        encoder.addMdcEntryWriter(new LongMdcEntryWriter());
        encoder.addMdcEntryWriter(new DoubleMdcEntryWriter());
        encoder.addMdcEntryWriter(new BooleanMdcEntryWriter());
        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("long").longValue()).isEqualTo(4711L);
        assertThat(node.get("double").doubleValue()).isEqualTo(2.71828);
        assertThat(node.get("bool").booleanValue()).isTrue();
        assertThat(node.get("default").textValue()).isEqualTo("string");
    }

    @Test
    public void kvpAllIncluded() throws Exception {
        List<KeyValuePair> kvp = new ArrayList<>();
        kvp.add(new KeyValuePair("thing_one", "One"));
        kvp.add(new KeyValuePair("thing_two", "Three"));

        LoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        event.setKeyValuePairs(kvp);

        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("thing_one").textValue()).isEqualTo("One");
        assertThat(node.get("thing_two").textValue()).isEqualTo("Three");
    }

    @Test
    public void kvpSomeIncluded() throws Exception {
        List<KeyValuePair> kvp = new ArrayList<>();
        kvp.add(new KeyValuePair("thing_one", "One"));
        kvp.add(new KeyValuePair("thing_two", "Three"));

        LoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        event.setKeyValuePairs(kvp);

        encoder.addIncludeKeyValueKeyName("thing_one");

        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("thing_one").textValue()).isEqualTo("One");
        assertThat(node.get("thing_two")).isNull();
    }

    @Test
    public void kvpSomeExcluded() throws Exception {
        List<KeyValuePair> kvp = new ArrayList<>();
        kvp.add(new KeyValuePair("thing_one", "One"));
        kvp.add(new KeyValuePair("thing_two", "Three"));

        LoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        event.setKeyValuePairs(kvp);

        encoder.addExcludeKeyValueKeyName("thing_two");

        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("thing_one").textValue()).isEqualTo("One");
        assertThat(node.get("thing_two")).isNull();
    }

    @Test
    public void kvpNoneIncluded() throws Exception {
        List<KeyValuePair> kvp = new ArrayList<>();
        kvp.add(new KeyValuePair("thing_one", "One"));
        kvp.add(new KeyValuePair("thing_two", "Three"));

        LoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        event.setKeyValuePairs(kvp);

        encoder.setIncludeKeyValuePairs(false);
        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("thing_one")).isNull();
        assertThat(node.get("thing_two")).isNull();
    }

    @Test
    public void propertiesInKVPAreIncludedInSubObject() throws Exception {
        List<KeyValuePair> kvp = new ArrayList<>();
        kvp.add(new KeyValuePair("thing_one", "One"));
        kvp.add(new KeyValuePair("thing_two", "Three"));

        LoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        event.setKeyValuePairs(kvp);

        encoder.getFieldNames().setKeyValuePair("kvp");
        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("kvp").get("thing_one").textValue()).isEqualTo("One");
        assertThat(node.get("kvp").get("thing_two").textValue()).isEqualTo("Three");
    }

    @Test
    public void nullKVPDoesNotCauseEverythingToBlowUp() {
        LoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        event.setKeyValuePairs(null);

        encoder.start();
        assertThatCode(() -> encoder.encode(event)).doesNotThrowAnyException();
    }

    @Test
    public void callerDataIsIncluded() throws Exception {
        LoggingEvent event = mockBasicILoggingEvent(Level.ERROR);

        StackTraceElement[] stackTraceElements = {
                new StackTraceElement("caller_class", "method_name", "file_name", 12345) };
        event.setCallerData(stackTraceElements);

        encoder.setIncludeCallerData(true);

        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("caller_class_name").textValue()).isEqualTo(stackTraceElements[0].getClassName());
        assertThat(node.get("caller_method_name").textValue()).isEqualTo(stackTraceElements[0].getMethodName());
        assertThat(node.get("caller_file_name").textValue()).isEqualTo(stackTraceElements[0].getFileName());
        assertThat(node.get("caller_line_number").intValue()).isEqualTo(stackTraceElements[0].getLineNumber());
    }

    @Test
    public void callerDataIsIncludedInSubObject() throws Exception {
        LoggingEvent event = mockBasicILoggingEvent(Level.ERROR);

        StackTraceElement[] stackTraceElements = {
                new StackTraceElement("caller_class", "method_name", "file_name", 12345) };
        event.setCallerData(stackTraceElements);

        encoder.setIncludeCallerData(true);
        encoder.getFieldNames().setCaller("caller");
        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("caller").get("caller_class_name").textValue())
                .isEqualTo(stackTraceElements[0].getClassName());
        assertThat(node.get("caller").get("caller_method_name").textValue())
                .isEqualTo(stackTraceElements[0].getMethodName());
        assertThat(node.get("caller").get("caller_file_name").textValue())
                .isEqualTo(stackTraceElements[0].getFileName());
        assertThat(node.get("caller").get("caller_line_number").intValue())
                .isEqualTo(stackTraceElements[0].getLineNumber());
    }

    @Test
    public void callerDataIsNotIncludedIfSwitchedOff() throws Exception {
        LoggingEvent event = mockBasicILoggingEvent(Level.ERROR);

        StackTraceElement[] stackTraceElements = {
                new StackTraceElement("caller_class", "method_name", "file_name", 12345) };
        event.setCallerData(stackTraceElements);

        encoder.setIncludeCallerData(false);
        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);
        assertThat(node.get("caller_class_name")).isNull();
        assertThat(node.get("caller_method_name")).isNull();
        assertThat(node.get("caller_file_name")).isNull();
        assertThat(node.get("caller_line_number")).isNull();
    }

    @Test
    public void propertiesInContextAreIncluded() throws Exception {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put("thing_one", "One");
        propertyMap.put("thing_two", "Three");

        Context context = mock(Context.class);
        when(context.getCopyOfPropertyMap()).thenReturn(propertyMap);

        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);

        encoder.setContext(context);
        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("thing_one").textValue()).isEqualTo("One");
        assertThat(node.get("thing_two").textValue()).isEqualTo("Three");
    }

    @Test
    public void propertiesInContextAreNotIncludedIfSwitchedOff() throws Exception {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put("thing_one", "One");
        propertyMap.put("thing_two", "Three");

        Context context = mock(Context.class);
        when(context.getCopyOfPropertyMap()).thenReturn(propertyMap);

        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        encoder.setIncludeContext(false);
        encoder.setContext(context);
        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("thing_one")).isNull();
        assertThat(node.get("thing_two")).isNull();
    }

    @Test
    public void propertiesInContextAreIncludedInSubObject() throws Exception {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put("thing_one", "One");
        propertyMap.put("thing_two", "Three");

        Context context = mock(Context.class);
        when(context.getCopyOfPropertyMap()).thenReturn(propertyMap);

        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);

        encoder.getFieldNames().setContext("context");
        encoder.setContext(context);
        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("context").get("thing_one").textValue()).isEqualTo("One");
        assertThat(node.get("context").get("thing_two").textValue()).isEqualTo("Three");
    }

    @Test
    public void markerIncludesItselfAsTag() throws Exception {
        Marker marker = MarkerFactory.getDetachedMarker("hoosh");
        LoggingEvent event = mockBasicILoggingEvent(Level.INFO);
        addMarker(event, marker);

        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertJsonArray(node.findValue("tags"), "hoosh");
    }

    @Test
    public void markerReferencesAreIncludedAsTags() throws Exception {
        Marker marker = MarkerFactory.getDetachedMarker("bees");
        marker.add(MarkerFactory.getMarker("knees"));
        LoggingEvent event = mockBasicILoggingEvent(Level.INFO);
        addMarker(event, marker);

        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertJsonArray(node.findValue("tags"), "bees", "knees");
    }

    @Test
    public void nullMarkerIsIgnored() throws Exception {
        ILoggingEvent event = mockBasicILoggingEvent(Level.INFO);

        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.findValue("tags")).isNull();
    }

    @Test
    public void markerNoneIncluded() throws Exception {
        Marker marker = MarkerFactory.getDetachedMarker("bees");
        marker.add(MarkerFactory.getDetachedMarker("knees"));
        LoggingEvent event = mockBasicILoggingEvent(Level.INFO);
        addMarker(event, marker);

        encoder.setIncludeTags(false);
        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.findValue("tags")).isNull();
    }

    @Test
    public void testAppendJsonMessage() throws Exception {
        Object[] argArray = new Object[] {
                1,
                Collections.singletonMap("hello", Collections.singletonMap("hello", "world")) };
        Marker marker = append("json_message", argArray);
        LoggingEvent event = mockBasicILoggingEvent(Level.INFO);
        addMarker(event, marker);
        event.setArgumentArray(argArray);

        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(MAPPER.convertValue(argArray, JsonNode.class)).isEqualTo(node.get("json_message"));
    }

    @Test
    public void includeJsonChunk() throws Exception {
        String customFields = "{\"appname\":\"damnGodWebservice\",\"roles\":[\"customerorder\", \"auth\"], \"buildinfo\": { \"version\" : \"Version 0.1.0-SNAPSHOT\", \"lastcommit\" : \"75473700d5befa953c45f630c6d9105413c16fe1\"} }";
        ILoggingEvent event = mockBasicILoggingEvent(Level.INFO);

        encoder.setCustomFields(customFields);
        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("appname").textValue()).isEqualTo("damnGodWebservice");
        assertThat(node.get("roles")).isEqualTo((parse("[\"customerorder\", \"auth\"]")));
        assertThat(node.get("buildinfo")).isEqualTo(parse(
                "{ \"version\" : \"Version 0.1.0-SNAPSHOT\", \"lastcommit\" : \"75473700d5befa953c45f630c6d9105413c16fe1\"}"));
    }

    @Test
    public void customTimeZone() throws Exception {
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);

        encoder.setTimeZone("UTC");
        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("@timestamp").textValue()).isEqualTo(
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(TimeZone.getTimeZone("UTC").toZoneId()).format(now));
        assertThat(node.get("@version").textValue()).isEqualTo("1");
        assertThat(node.get("logger_name").textValue()).isEqualTo("LoggerName");
        assertThat(node.get("thread_name").textValue()).isEqualTo("ThreadName");
        assertThat(node.get("message").textValue()).isEqualTo("My message");
        assertThat(node.get("level").textValue()).isEqualTo("ERROR");
        assertThat(node.get("level_value").intValue()).isEqualTo(40000);
    }

    public JsonNode parse(String string) throws IOException {
        return FACTORY.createParser(string).readValueAsTree();
    }

    @Test
    public void testAppendEntries() throws Exception {
        LoggingEvent event = mockBasicILoggingEvent(Level.INFO);

        Map<String, Object> contextMap = new HashMap<>();
        contextMap.put("duration", 1200);
        contextMap.put("remoteResponse", "OK");
        contextMap.put("extra", Collections.singletonMap("extraEntry", "extraValue"));

        Object[] argList = new Object[] {
                "firstParamThatShouldBeIgnored",
                Collections.singletonMap("ignoredMapEntry", "whatever"), };

        event.setArgumentArray(argList);

        Marker marker = appendEntries(contextMap);
        addMarker(event, marker);

        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);
        assertThat(node.get("duration")).isNotNull();
        assertThat(node.get("duration").intValue()).isEqualTo(1200);
        assertThat(node.get("remoteResponse")).isNotNull();
        assertThat(node.get("remoteResponse").textValue()).isEqualTo("OK");
        assertThat(node.get("extra")).isNotNull();
        assertThat(node.get("extra").get("extraEntry")).isNotNull();
        assertThat(node.get("extra").get("extraEntry").textValue()).isEqualTo("extraValue");

        assertThat(node.get("ignoredMapEntry")).as("The second map from the end should be ignored").isNull();
    }

    @Test
    public void testEncoderConfiguration() throws Exception {
        Logger LOG = LoggerFactory.getLogger(LogstashEncoderTest.class);

        File tempFile = new File(System.getProperty("java.io.tmpdir"), "test.log");
        // Empty the log file
        PrintWriter writer = new PrintWriter(tempFile);
        writer.print("");
        writer.close();
        MDC.put("myMdcKey", "myMdcValue");
        LOG.info(append("appendedName", "appendedValue").and(append("n1", 2)), "Testing info logging.");
        MDC.remove("myMdcKey");

        List<String> lines = Files.linesOf(tempFile, StandardCharsets.UTF_8);
        JsonNode node = MAPPER.readTree(lines.get(0).getBytes(StandardCharsets.UTF_8));

        /*
         * The configuration suppresses the version field, make sure it doesn't appear.
         */
        assertThat(node.get("@version")).isNull();
        assertThat(node.get(LogstashCommonFieldNames.IGNORE_FIELD_INDICATOR)).isNull();

        assertThat(node.get("appname").textValue()).isEqualTo("damnGodWebservice");
        assertThat(node.get("appendedName").textValue()).isEqualTo("appendedValue");
        assertThat(node.get("myMdcKey")).isNull();
        assertThat(node.get("logger").textValue()).isEqualTo(LogstashEncoderTest.class.getName());
        assertThat(node.get("roles")).isEqualTo(parse("[\"customerorder\", \"auth\"]"));
        assertThat(node.get("buildinfo")).isEqualTo(parse(
                "{ \"version\" : \"Version 0.1.0-SNAPSHOT\", \"lastcommit\" : \"75473700d5befa953c45f630c6d9105413c16fe1\"}"));
    }

    @Test
    public void testEncoderConfigurationOnFluentApi() throws Exception {
        Logger LOG = LoggerFactory.getLogger(LogstashEncoderTest.class);

        File tempFile = new File(System.getProperty("java.io.tmpdir"), "test.log");
        // Empty the log file
        PrintWriter writer = new PrintWriter(tempFile);
        writer.print("");
        writer.close();
        LOG.atInfo()
                .addMarker(append("appendedName", "appendedValue").and(append("n1", 2)))
                .addKeyValue("myKvpKey", "myKvpValue")
                .log("Testing info logging.");

        List<String> lines = Files.linesOf(tempFile, StandardCharsets.UTF_8);
        JsonNode node = MAPPER.readTree(lines.get(0).getBytes(StandardCharsets.UTF_8));

        /*
         * The configuration suppresses the version field, make sure it doesn't appear.
         */
        assertThat(node.get("@version")).isNull();
        assertThat(node.get(LogstashCommonFieldNames.IGNORE_FIELD_INDICATOR)).isNull();

        assertThat(node.get("appname").textValue()).isEqualTo("damnGodWebservice");
        assertThat(node.get("appendedName").textValue()).isEqualTo("appendedValue");
        assertThat(node.get("myKvpKey").textValue()).isEqualTo("myKvpValue");
        assertThat(node.get("logger").textValue()).isEqualTo(LogstashEncoderTest.class.getName());
        assertThat(node.get("roles")).isEqualTo(parse("[\"customerorder\", \"auth\"]"));
        assertThat(node.get("buildinfo")).isEqualTo(parse(
                "{ \"version\" : \"Version 0.1.0-SNAPSHOT\", \"lastcommit\" : \"75473700d5befa953c45f630c6d9105413c16fe1\"}"));
    }

    @Test
    public void testCustomFields() {
        String customFields = "{\"foo\":\"bar\"}";
        encoder.setCustomFields(customFields);
        assertThat(encoder.getCustomFields()).isEqualTo(customFields);

    }

    @Test
    public void unixTimestampAsNumber() throws Exception {
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);

        encoder.setTimestampPattern(AbstractFormattedTimestampJsonProvider.UNIX_TIMESTAMP_AS_NUMBER);
        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("@timestamp").numberValue()).isEqualTo(event.getTimeStamp());
    }

    @Test
    public void unixTimestampAsString() throws Exception {
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);

        encoder.setTimestampPattern(AbstractFormattedTimestampJsonProvider.UNIX_TIMESTAMP_AS_STRING);
        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("@timestamp").textValue()).isEqualTo(Long.toString(event.getTimeStamp()));
    }

    @Test
    public void testMessageSplitEnabled() throws Exception {
        LoggingEvent event = mockBasicILoggingEvent(Level.ERROR, buildMultiLineMessage("###"));
        encoder.setMessageSplitRegex("#+");
        assertThat(encoder.getMessageSplitRegex()).isEqualTo("#+");

        encoder.start();
        JsonNode node = MAPPER.readTree(encoder.encode(event));
        encoder.stop();

        assertJsonArray(node.path("message"), "line1", "line2", "line3");
    }

    @Test
    public void testMessageSplitDisabled() throws Exception {
        String message = buildMultiLineMessage(System.lineSeparator());

        LoggingEvent event = mockBasicILoggingEvent(Level.ERROR, message);
        encoder.setMessageSplitRegex(null);
        assertThat(encoder.getMessageSplitRegex()).isNull();

        encoder.start();
        JsonNode node = MAPPER.readTree(encoder.encode(event));
        encoder.stop();

        assertThat(node.path("message").isTextual()).isTrue();
        assertThat(node.path("message").textValue()).isEqualTo(message);
    }

    @Test
    public void testMessageSplitDisabledByDefault() throws Exception {
        String message = buildMultiLineMessage(System.lineSeparator());

        LoggingEvent event = mockBasicILoggingEvent(Level.ERROR, message);
        assertThat(encoder.getMessageSplitRegex()).isNull();

        encoder.start();
        JsonNode node = MAPPER.readTree(encoder.encode(event));
        encoder.stop();

        assertThat(node.path("message").isTextual()).isTrue();
        assertThat(node.path("message").textValue()).isEqualTo(message);
    }

    @Test
    public void testJsonProvidersCannotBeChanged() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> encoder.setProviders(new LoggingEventJsonProviders()));
    }

    private void assertJsonArray(JsonNode jsonNode, String... expected) {
        assertThat(jsonNode).isNotNull();
        assertThat(jsonNode.isArray()).isTrue();

        String[] values = new String[jsonNode.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = jsonNode.get(i).asText();
        }
        assertThat(values).containsExactly(expected);
    }

    private LoggingEvent mockBasicILoggingEvent(Level level) {
        return mockBasicILoggingEvent(level, "My message");
    }

    private LoggingEvent mockBasicILoggingEvent(Level level, String message) {
        return mockBasicILoggingEvent(level, message, Collections.emptyMap());
    }

    private LoggingEvent mockBasicILoggingEvent(Level level, String message, Map<String, String> mDCPropertyMap) {
        LoggingEvent event = new LoggingEvent();
        event.setLoggerName("LoggerName");
        event.setThreadName("ThreadName");
        event.setMessage(message);
        event.setLevel(level);
        event.setMDCPropertyMap(mDCPropertyMap);

        event.setInstant(now);

        return spy(event);
    }

    private void addMarker(LoggingEvent event, Marker marker) {
        event.addMarker(marker);
    }

    private static String buildMultiLineMessage(String lineSeparator) {
        return String.join(lineSeparator, "line1", "line2", "line3");
    }
}
