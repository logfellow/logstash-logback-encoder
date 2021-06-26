/**
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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.TargetLengthBasedClassNameAbbreviator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.Context;
import net.logstash.logback.composite.FormattedTimestampJsonProvider;
import net.logstash.logback.decorate.JsonFactoryDecorator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;
import net.logstash.logback.fieldnames.LogstashCommonFieldNames;
import net.logstash.logback.fieldnames.ShortenedFieldNames;

import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LogstashEncoderTest {
    
    private static final Logger LOG = LoggerFactory.getLogger(LogstashEncoderTest.class);

    private static final JsonFactory FACTORY = new MappingJsonFactory().enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);
    private static final ObjectMapper MAPPER = new ObjectMapper(FACTORY);
    private final LogstashEncoder encoder = new LogstashEncoder();
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    
    @Test
    public void basicsAreIncluded_logback12() throws Exception {
        final long timestamp = System.currentTimeMillis();
        
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getTimeStamp()).thenReturn(timestamp);
        
        encoder.start();
        byte[] encoded = encoder.encode(event);
        
        JsonNode node = MAPPER.readTree(encoded);
        
        verifyBasics(timestamp, node);
    }

    protected void verifyBasics(final long timestamp, JsonNode node) {
        assertThat(node.get("@timestamp").textValue()).isEqualTo(DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(TimeZone.getDefault().toZoneId()).format(Instant.ofEpochMilli(timestamp)));
        assertThat(node.get("@version").textValue()).isEqualTo("1");
        assertThat(node.get("logger_name").textValue()).isEqualTo("LoggerName");
        assertThat(node.get("thread_name").textValue()).isEqualTo("ThreadName");
        assertThat(node.get("message").textValue()).isEqualTo("My message");
        assertThat(node.get("level").textValue()).isEqualTo("ERROR");
        assertThat(node.get("level_value").intValue()).isEqualTo(40000);
    }
    
    @Test
    public void basicsAreIncludedWithShortenedNames() throws Exception {
        final long timestamp = System.currentTimeMillis();
        
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        
        when(event.getTimeStamp()).thenReturn(timestamp);
        encoder.setFieldNames(new ShortenedFieldNames());
        encoder.start();
        byte[] encoded = encoder.encode(event);
        
        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("@timestamp").textValue()).isEqualTo(DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(TimeZone.getDefault().toZoneId()).format(Instant.ofEpochMilli(timestamp)));
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
        
        encoder.setJsonGeneratorDecorator(new JsonGeneratorDecorator() {
            
            @Override
            public JsonGenerator decorate(JsonGenerator generator) {
                return generator.useDefaultPrettyPrinter();
            }
        });
        
        encoder.start();
        final long timestamp = System.currentTimeMillis();
        
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getTimeStamp()).thenReturn(timestamp);
        
        byte[] encoded = encoder.encode(event);
        
        String output = new String(encoded, StandardCharsets.UTF_8);
        
        assertThat(output).isEqualTo(String.format(
                "{%n"
                + "  @timestamp : \"" + DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(TimeZone.getDefault().toZoneId()).format(Instant.ofEpochMilli(timestamp)) + "\",%n"
                + "  @version : \"1\",%n"
                + "  message : \"My message\",%n"
                + "  logger_name : \"LoggerName\",%n"
                + "  thread_name : \"ThreadName\",%n"
                + "  level : \"ERROR\",%n"
                + "  level_value : 40000%n"
                + "}%n"));
    }
    

    @Test
    public void loggerNameIsShortenedProperly() throws Exception {
        final long timestamp = System.currentTimeMillis();
        final int length = 36;
        final String shortenedLoggerName = new TargetLengthBasedClassNameAbbreviator(length).abbreviate(DateTimeFormatter.class.getCanonicalName());

        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getLoggerName()).thenReturn(DateTimeFormatter.class.getCanonicalName());

        when(event.getTimeStamp()).thenReturn(timestamp);
        encoder.setFieldNames(new ShortenedFieldNames());
        encoder.setShortenedLoggerNameLength(length);
        encoder.start();
        byte[] encoded = encoder.encode(event);
        
        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("@timestamp").textValue()).isEqualTo(DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(TimeZone.getDefault().toZoneId()).format(Instant.ofEpochMilli(timestamp)));
        assertThat(node.get("@version").textValue()).isEqualTo("1");
        assertThat(node.get("logger").textValue()).isEqualTo(shortenedLoggerName);
        assertThat(node.get("thread").textValue()).isEqualTo("ThreadName");
        assertThat(node.get("message").textValue()).isEqualTo("My message");
        assertThat(node.get("level").textValue()).isEqualTo("ERROR");
        assertThat(node.get("levelVal").intValue()).isEqualTo(40000);
    }
    
    @Test
    public void includingThrowableProxyIncludesStackTrace() throws Exception {
        IThrowableProxy throwableProxy = new ThrowableProxy(new Exception("My goodness"));
        
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getThrowableProxy()).thenReturn(throwableProxy);
        
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
        
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getMDCPropertyMap()).thenReturn(mdcMap);
        
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
        
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getMDCPropertyMap()).thenReturn(mdcMap);
        
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
        
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getMDCPropertyMap()).thenReturn(mdcMap);
        
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
        
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);

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
        
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getMDCPropertyMap()).thenReturn(mdcMap);
        
        encoder.getFieldNames().setMdc("mdc");
        encoder.start();
        byte[] encoded = encoder.encode(event);
        
        JsonNode node = MAPPER.readTree(encoded);
        
        assertThat(node.get("mdc").get("thing_one").textValue()).isEqualTo("One");
        assertThat(node.get("mdc").get("thing_two").textValue()).isEqualTo("Three");
    }
    
    @Test
    public void nullMDCDoesNotCauseEverythingToBlowUp() {
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getMDCPropertyMap()).thenReturn(null);
        
        encoder.start();
        encoder.encode(event);
    }
    
    @Test
    public void callerDataIsIncluded() throws Exception {
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getMDCPropertyMap()).thenReturn(Collections.emptyMap());
        final StackTraceElement[] stackTraceElements = { new StackTraceElement("caller_class", "method_name", "file_name", 12345) };
        when(event.getCallerData()).thenReturn(stackTraceElements);
        
        encoder.setIncludeCallerInfo(true);
        
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
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getMDCPropertyMap()).thenReturn(Collections.emptyMap());
        final StackTraceElement[] stackTraceElements = { new StackTraceElement("caller_class", "method_name", "file_name", 12345) };
        when(event.getCallerData()).thenReturn(stackTraceElements);
        
        encoder.setIncludeCallerInfo(true);
        encoder.getFieldNames().setCaller("caller");
        encoder.start();
        byte[] encoded = encoder.encode(event);
        
        JsonNode node = MAPPER.readTree(encoded);
        
        assertThat(node.get("caller").get("caller_class_name").textValue()).isEqualTo(stackTraceElements[0].getClassName());
        assertThat(node.get("caller").get("caller_method_name").textValue()).isEqualTo(stackTraceElements[0].getMethodName());
        assertThat(node.get("caller").get("caller_file_name").textValue()).isEqualTo(stackTraceElements[0].getFileName());
        assertThat(node.get("caller").get("caller_line_number").intValue()).isEqualTo(stackTraceElements[0].getLineNumber());
    }
    
    @Test
    public void callerDataIsNotIncludedIfSwitchedOff() throws Exception {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLoggerName()).thenReturn("LoggerName");
        when(event.getThreadName()).thenReturn("ThreadName");
        when(event.getFormattedMessage()).thenReturn("My message");
        when(event.getLevel()).thenReturn(Level.ERROR);
        when(event.getMDCPropertyMap()).thenReturn(Collections.emptyMap());
        encoder.setIncludeCallerInfo(false);
        
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
        
        final Context context = mock(Context.class);
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
        
        final Context context = mock(Context.class);

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
        
        final Context context = mock(Context.class);
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
        Marker marker = MarkerFactory.getMarker("hoosh");
        ILoggingEvent event = mockBasicILoggingEvent(Level.INFO);
        when(event.getMarker()).thenReturn(marker);
        
        encoder.start();
        byte[] encoded = encoder.encode(event);
        
        JsonNode node = MAPPER.readTree(encoded);
        
        assertJsonArray(node.findValue("tags"), "hoosh");
    }
    
    @Test
    public void markerReferencesAreIncludedAsTags() throws Exception {
        Marker marker = MarkerFactory.getMarker("bees");
        marker.add(MarkerFactory.getMarker("knees"));
        ILoggingEvent event = mockBasicILoggingEvent(Level.INFO);
        when(event.getMarker()).thenReturn(marker);
        
        encoder.start();
        byte[] encoded = encoder.encode(event);
        
        JsonNode node = MAPPER.readTree(encoded);
        
        assertJsonArray(node.findValue("tags"), "bees", "knees");
    }
    
    @Test
    public void nullMarkerIsIgnored() throws Exception {
        ILoggingEvent event = mockBasicILoggingEvent(Level.INFO);
        when(event.getMarker()).thenReturn(null);
        
        encoder.start();
        byte[] encoded = encoder.encode(event);
        
        JsonNode node = MAPPER.readTree(encoded);
        
        assertThat(node.findValue("tags")).isNull();
    }

    @Test
    public void markerNoneIncluded() throws Exception {
        Marker marker = MarkerFactory.getMarker("bees");
        marker.add(MarkerFactory.getMarker("knees"));
        ILoggingEvent event = mockBasicILoggingEvent(Level.INFO);
        when(event.getMarker()).thenReturn(marker);

        encoder.setIncludeTags(false);
        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.findValue("tags")).isNull();
    }

    @Test
    public void testAppendJsonMessage() throws Exception {
        Object[] argArray = new Object[] { 1, Collections.singletonMap("hello", Collections.singletonMap("hello", "world")) };
        Marker marker = append("json_message", argArray);
        ILoggingEvent event = mockBasicILoggingEvent(Level.INFO);
        when(event.getMarker()).thenReturn(marker);
        when(event.getArgumentArray()).thenReturn(argArray);
        
        encoder.start();
        byte[] encoded = encoder.encode(event);
        
        JsonNode node = MAPPER.readTree(encoded);
        
        assertThat(MAPPER.convertValue(argArray, JsonNode.class).equals(node.get("json_message"))).isEqualTo(true);
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
        assertTrue(node.get("roles").equals(parse("[\"customerorder\", \"auth\"]")));
        assertTrue(node.get("buildinfo").equals(parse("{ \"version\" : \"Version 0.1.0-SNAPSHOT\", \"lastcommit\" : \"75473700d5befa953c45f630c6d9105413c16fe1\"}")));
    }
    
    @Test
    public void customTimeZone() throws Exception {
        final long timestamp = System.currentTimeMillis();
        
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getTimeStamp()).thenReturn(timestamp);
        
        encoder.setTimeZone("UTC");
        encoder.start();
        byte[] encoded = encoder.encode(event);
        
        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("@timestamp").textValue()).isEqualTo(DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(TimeZone.getTimeZone("UTC").toZoneId()).format(Instant.ofEpochMilli(timestamp)));
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
        ILoggingEvent event = mockBasicILoggingEvent(Level.INFO);
        
        Map<String, Object> contextMap = new HashMap<>();
        contextMap.put("duration", 1200);
        contextMap.put("remoteResponse", "OK");
        contextMap.put("extra", Collections.singletonMap("extraEntry", "extraValue"));
        
        Object[] argList = new Object[] {
                "firstParamThatShouldBeIgnored",
                Collections.singletonMap("ignoredMapEntry", "whatever"),
        };
        
        when(event.getArgumentArray()).thenReturn(argList);
        
        Marker marker = appendEntries(contextMap);
        when(event.getMarker()).thenReturn(marker);
        
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
         * The configuration suppresses the version field,
         * make sure it doesn't appear.
         */
        assertThat(node.get("@version")).isNull();
        assertThat(node.get(LogstashCommonFieldNames.IGNORE_FIELD_INDICATOR)).isNull();
        
        assertThat(node.get("appname").textValue()).isEqualTo("damnGodWebservice");
        assertThat(node.get("appendedName").textValue()).isEqualTo("appendedValue");
        assertThat(node.get("myMdcKey")).isNull();
        assertThat(node.get("logger").textValue()).isEqualTo(LogstashEncoderTest.class.getName());
        assertTrue(node.get("roles").equals(parse("[\"customerorder\", \"auth\"]")));
        assertTrue(node.get("buildinfo").equals(parse("{ \"version\" : \"Version 0.1.0-SNAPSHOT\", \"lastcommit\" : \"75473700d5befa953c45f630c6d9105413c16fe1\"}")));
    }
    
    @Test
    public void testCustomFields() {
        String customFields = "{\"foo\":\"bar\"}";
        encoder.setCustomFields(customFields);
        assertThat(encoder.getCustomFields()).isEqualTo(customFields);
        
    }
    
    @Test
    public void unixTimestampAsNumber() throws Exception {
        final long timestamp = System.currentTimeMillis();
        
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getTimeStamp()).thenReturn(timestamp);
        
        encoder.setTimestampPattern(FormattedTimestampJsonProvider.UNIX_TIMESTAMP_AS_NUMBER);
        encoder.start();
        byte[] encoded = encoder.encode(event);
        
        JsonNode node = MAPPER.readTree(encoded);
        
        assertThat(node.get("@timestamp").numberValue()).isEqualTo(timestamp);
    }    
    
    @Test
    public void unixTimestampAsString() throws Exception {
        final long timestamp = System.currentTimeMillis();
        
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getTimeStamp()).thenReturn(timestamp);
        
        encoder.setTimestampPattern(FormattedTimestampJsonProvider.UNIX_TIMESTAMP_AS_STRING);
        encoder.start();
        byte[] encoded = encoder.encode(event);
        
        JsonNode node = MAPPER.readTree(encoded);
        
        assertThat(node.get("@timestamp").textValue()).isEqualTo(Long.toString(timestamp));
    }    
    
    @Test
    public void testMessageSplitEnabled() throws Exception {
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getFormattedMessage()).thenReturn(buildMultiLineMessage("###"));
        encoder.setMessageSplitRegex("#+");
        assertEquals("#+", encoder.getMessageSplitRegex());

        encoder.start();
        JsonNode node = MAPPER.readTree(encoder.encode(event));
        encoder.stop();

        assertJsonArray(node.path("message"), "line1", "line2", "line3");
    }

    @Test
    public void testMessageSplitDisabled() throws Exception {
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getFormattedMessage()).thenReturn(buildMultiLineMessage(System.lineSeparator()));
        encoder.setMessageSplitRegex(null);
        assertNull(encoder.getMessageSplitRegex());

        encoder.start();
        JsonNode node = MAPPER.readTree(encoder.encode(event));
        encoder.stop();

        assertTrue(node.path("message").isTextual());
        assertEquals(node.path("message").textValue(), buildMultiLineMessage(System.lineSeparator()));
    }

    @Test
    public void testMessageSplitDisabledByDefault() throws Exception {
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getFormattedMessage()).thenReturn(buildMultiLineMessage(System.lineSeparator()));
        assertNull(encoder.getMessageSplitRegex());

        encoder.start();
        JsonNode node = MAPPER.readTree(encoder.encode(event));
        encoder.stop();

        assertTrue(node.path("message").isTextual());
        assertEquals(node.path("message").textValue(), buildMultiLineMessage(System.lineSeparator()));
    }

    private void assertJsonArray(JsonNode jsonNode, String... expected) {
        assertNotNull(jsonNode);
        assertTrue(jsonNode.isArray());

        String[] values = new String[jsonNode.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = jsonNode.get(i).asText();
        }
        assertArrayEquals(expected, values);
    }
    
    private ILoggingEvent mockBasicILoggingEvent(Level level) {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLoggerName()).thenReturn("LoggerName");
        when(event.getThreadName()).thenReturn("ThreadName");
        when(event.getFormattedMessage()).thenReturn("My message");
        when(event.getLevel()).thenReturn(level);
        return event;
    }
    
    private static String buildMultiLineMessage(String lineSeparator) {
        return String.join(lineSeparator, "line1", "line2", "line3");
    }
}
