/*
 * Copyright 2013-2021 the original author or authors.
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
package net.logstash.logback;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import net.logstash.logback.appender.AsyncDisruptorAppender;
import net.logstash.logback.appender.LoggingEventAsyncDisruptorAppender;
import net.logstash.logback.appender.listener.AppenderListener;
import net.logstash.logback.argument.StructuredArguments;
import net.logstash.logback.composite.ContextJsonProvider;
import net.logstash.logback.composite.GlobalCustomFieldsJsonProvider;
import net.logstash.logback.composite.JsonProvider;
import net.logstash.logback.composite.LogstashVersionJsonProvider;
import net.logstash.logback.composite.loggingevent.ArgumentsJsonProvider;
import net.logstash.logback.composite.loggingevent.CallerDataJsonProvider;
import net.logstash.logback.composite.loggingevent.ContextNameJsonProvider;
import net.logstash.logback.composite.loggingevent.LogLevelJsonProvider;
import net.logstash.logback.composite.loggingevent.LogLevelValueJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggerNameJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggingEventFormattedTimestampJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggingEventNestedJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggingEventPatternJsonProvider;
import net.logstash.logback.composite.loggingevent.LogstashMarkersJsonProvider;
import net.logstash.logback.composite.loggingevent.MdcJsonProvider;
import net.logstash.logback.composite.loggingevent.MessageJsonProvider;
import net.logstash.logback.composite.loggingevent.RawMessageJsonProvider;
import net.logstash.logback.composite.loggingevent.SequenceJsonProvider;
import net.logstash.logback.composite.loggingevent.StackTraceJsonProvider;
import net.logstash.logback.composite.loggingevent.TagsJsonProvider;
import net.logstash.logback.composite.loggingevent.ThreadNameJsonProvider;
import net.logstash.logback.composite.loggingevent.UuidProvider;
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder;
import net.logstash.logback.marker.Markers;
import net.logstash.logback.stacktrace.ShortenedThrowableConverter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class ConfigurationTest {

    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(ConfigurationTest.class);

    private final ListAppender<ILoggingEvent> listAppender = (ListAppender<ILoggingEvent>) LOGGER.getAppender("listAppender");

    private final JsonFactory jsonFactory = new MappingJsonFactory();

    @BeforeEach
    public void setup() {
        listAppender.list.clear();
    }

    @Test
    public void testLogstashEncoderAppender() throws IOException {
        LoggingEventCompositeJsonEncoder encoder = getEncoder("logstashEncoderAppender");
        List<JsonProvider<ILoggingEvent>> providers = encoder.getProviders().getProviders();
        Assertions.assertEquals(19, providers.size());

        verifyCommonProviders(providers);

        verifyOutput(encoder);
    }

    @Test
    public void testLoggingEventCompositeJsonEncoderAppender() throws IOException {
        LoggingEventCompositeJsonEncoder encoder = getEncoder("loggingEventCompositeJsonEncoderAppender");
        List<JsonProvider<ILoggingEvent>> providers = encoder.getProviders().getProviders();
        Assertions.assertEquals(23, providers.size());

        verifyCommonProviders(providers);

        Assertions.assertNotNull(getInstance(providers, TestJsonProvider.class));

        verifyOutput(encoder);
    }

    @Test
    public void testAppenderHasListener() throws IOException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        LoggingEventAsyncDisruptorAppender appender = getAppender("asyncAppender");
        Field listenersField = AsyncDisruptorAppender.class.getDeclaredField("listeners");
        listenersField.setAccessible(true);
        List<AppenderListener<ILoggingEvent>> listeners = (List<AppenderListener<ILoggingEvent>>) listenersField.get(appender);
        Assertions.assertEquals(1, listeners.size());
    }


    private void verifyCommonProviders(List<JsonProvider<ILoggingEvent>> providers) {
        LoggingEventFormattedTimestampJsonProvider timestampJsonProvider = getInstance(providers, LoggingEventFormattedTimestampJsonProvider.class);
        Assertions.assertNotNull(timestampJsonProvider);
        Assertions.assertEquals("@timestamp", timestampJsonProvider.getFieldName());

        LogstashVersionJsonProvider<ILoggingEvent> versionJsonProvider = getInstance(providers, LogstashVersionJsonProvider.class);
        Assertions.assertNotNull(versionJsonProvider);
        Assertions.assertEquals("@version", versionJsonProvider.getFieldName());

        MessageJsonProvider messageJsonProvider = getInstance(providers, MessageJsonProvider.class);
        Assertions.assertNotNull(messageJsonProvider);
        Assertions.assertEquals("customMessage", messageJsonProvider.getFieldName());

        LoggerNameJsonProvider loggerNameJsonProvider = getInstance(providers, LoggerNameJsonProvider.class);
        Assertions.assertNotNull(loggerNameJsonProvider);
        Assertions.assertEquals("logger_name", loggerNameJsonProvider.getFieldName());

        ThreadNameJsonProvider threadNameJsonProvider = getInstance(providers, ThreadNameJsonProvider.class);
        Assertions.assertNotNull(threadNameJsonProvider);
        Assertions.assertEquals("thread_name", threadNameJsonProvider.getFieldName());

        LogLevelJsonProvider logLevelJsonProvider = getInstance(providers, LogLevelJsonProvider.class);
        Assertions.assertNotNull(logLevelJsonProvider);
        Assertions.assertEquals("level", logLevelJsonProvider.getFieldName());

        LogLevelValueJsonProvider levelValueJsonProvider = getInstance(providers, LogLevelValueJsonProvider.class);
        Assertions.assertNotNull(levelValueJsonProvider);
        Assertions.assertEquals("level_value", levelValueJsonProvider.getFieldName());

        CallerDataJsonProvider callerDataJsonProvider = getInstance(providers, CallerDataJsonProvider.class);
        Assertions.assertNotNull(callerDataJsonProvider);
        Assertions.assertEquals("caller", callerDataJsonProvider.getFieldName());
        Assertions.assertEquals("class", callerDataJsonProvider.getClassFieldName());
        Assertions.assertEquals("method", callerDataJsonProvider.getMethodFieldName());
        Assertions.assertEquals("file", callerDataJsonProvider.getFileFieldName());
        Assertions.assertEquals("line", callerDataJsonProvider.getLineFieldName());

        StackTraceJsonProvider stackTraceJsonProvider = getInstance(providers, StackTraceJsonProvider.class);
        Assertions.assertNotNull(stackTraceJsonProvider);
        ShortenedThrowableConverter throwableConverter = (ShortenedThrowableConverter) stackTraceJsonProvider.getThrowableConverter();
        Assertions.assertEquals(20, throwableConverter.getMaxDepthPerThrowable());
        Assertions.assertEquals(1000, throwableConverter.getMaxLength());
        Assertions.assertEquals(30, throwableConverter.getShortenedClassNameLength());
        Assertions.assertTrue(throwableConverter.isRootCauseFirst());
        Assertions.assertEquals(2, throwableConverter.getExcludes().size());
        Assertions.assertEquals("excluded1", throwableConverter.getExcludes().get(0));
        Assertions.assertEquals("excluded2", throwableConverter.getExcludes().get(1));
        Assertions.assertEquals(true, throwableConverter.isInlineHash());

        Assertions.assertNotNull(getInstance(providers, ContextJsonProvider.class));
        Assertions.assertNotNull(getInstance(providers, ContextNameJsonProvider.class));

        MdcJsonProvider mdcJsonProvider = getInstance(providers, MdcJsonProvider.class);
        Assertions.assertNotNull(mdcJsonProvider);
        Assertions.assertEquals("included", mdcJsonProvider.getIncludeMdcKeyNames().get(0));
        Assertions.assertEquals("renamedKey", mdcJsonProvider.getMdcKeyFieldNames().get("key"));

        GlobalCustomFieldsJsonProvider<ILoggingEvent> globalCustomFieldsJsonProvider = getInstance(providers, GlobalCustomFieldsJsonProvider.class);
        Assertions.assertNotNull(globalCustomFieldsJsonProvider);
        Assertions.assertEquals("{\"customName\":\"customValue\"}", globalCustomFieldsJsonProvider.getCustomFields());

        Assertions.assertNotNull(getInstance(providers, TagsJsonProvider.class));
        Assertions.assertNotNull(getInstance(providers, LogstashMarkersJsonProvider.class));

        LoggingEventPatternJsonProvider patternProvider = getInstance(providers, LoggingEventPatternJsonProvider.class);
        Assertions.assertEquals("{\"patternName\":\"patternValue\",\"relativeTime\":\"#asLong{%relative}\"}", patternProvider.getPattern());
        Assertions.assertNotNull(patternProvider);
        
        LoggingEventNestedJsonProvider nestedJsonProvider = getInstance(providers, LoggingEventNestedJsonProvider.class);
        Assertions.assertNotNull(nestedJsonProvider);
        Assertions.assertEquals("nested", nestedJsonProvider.getFieldName());
        
        RawMessageJsonProvider rawMessageJsonProvider = getInstance(nestedJsonProvider.getProviders().getProviders(), RawMessageJsonProvider.class);
        Assertions.assertNotNull(rawMessageJsonProvider);
        Assertions.assertEquals("customRawMessage", rawMessageJsonProvider.getFieldName());

        ArgumentsJsonProvider argumentsJsonProvider = getInstance(providers, ArgumentsJsonProvider.class);
        Assertions.assertNotNull(argumentsJsonProvider);

        UuidProvider uuidProvider = getInstance(nestedJsonProvider.getProviders().getProviders(), UuidProvider.class);
        Assertions.assertNotNull(uuidProvider);
        Assertions.assertEquals("id", uuidProvider.getFieldName());
        Assertions.assertEquals("00:C0:F0:3D:5B:7C", uuidProvider.getEthernet());
        Assertions.assertEquals(UuidProvider.STRATEGY_TIME, uuidProvider.getStrategy());

        SequenceJsonProvider sequenceJsonProvider = getInstance(providers, SequenceJsonProvider.class);
        Assertions.assertNotNull(sequenceJsonProvider);
        Assertions.assertEquals("sequenceNumberField", sequenceJsonProvider.getFieldName());
    }

    private <T extends JsonProvider<ILoggingEvent>> T getInstance(List<JsonProvider<ILoggingEvent>> providers, Class<T> clazz) {
        for (JsonProvider<ILoggingEvent> jsonProvider : providers) {
            if (clazz.isInstance(jsonProvider)) {
                return clazz.cast(jsonProvider);
            }
        }
        return null;
    }

    private void verifyOutput(LoggingEventCompositeJsonEncoder encoder) throws IOException {
        LOGGER.info(Markers.append("markerFieldName", "markerFieldValue"), "message {} {} {} {}",
                new Object[] {
                    "arg",
                    StructuredArguments.keyValue("k1", "v1"),
                    StructuredArguments.keyValue("k2", "v2", "{0}=[{1}]"),
                    StructuredArguments.value("k3", "v3"),
                    new Throwable()
                });

        byte[] encoded = encoder.encode(listAppender.list.get(0));

        Map<String, Object> output = parseJson(new String(encoded, StandardCharsets.UTF_8));
        Assertions.assertNotNull(output.get("@timestamp"));
        Assertions.assertEquals("1", output.get("@version"));
        Assertions.assertEquals("message arg k1=v1 k2=[v2] v3", output.get("customMessage"));
        Map<String, Object> nested = (Map<String, Object>) output.get("nested");
        Assertions.assertEquals("message {} {} {} {}", nested.get("customRawMessage"));
        Assertions.assertEquals("n.l.l.ConfigurationTest", output.get("logger_name"));
        Assertions.assertEquals("testContext", output.get("context"));
        Assertions.assertNotNull(output.get("thread_name"));
        Assertions.assertEquals("INFO", output.get("level"));
        Assertions.assertEquals(20000, output.get("level_value"));
        Assertions.assertNotNull(output.get("caller"));
        Assertions.assertTrue(((String) output.get("stack_trace")).contains("n.l.logback.ConfigurationTest.verifyOutput"));
        Assertions.assertEquals("customValue", output.get("customName"));
        Assertions.assertEquals("patternValue", output.get("patternName"));
        Assertions.assertEquals("markerFieldValue", output.get("markerFieldName"));
        Assertions.assertTrue(output.get("relativeTime") instanceof Number);
        Assertions.assertEquals("arg", output.get("prefix0"));
        Assertions.assertEquals("v1", output.get("k1"));
        Assertions.assertEquals("v2", output.get("k2"));
        Assertions.assertEquals("v3", output.get("k3"));

        Number sequence = (Number) output.get("sequenceNumberField");
        Assertions.assertNotNull(sequence);
        Assertions.assertNotEquals("", sequence);
        Assertions.assertTrue(0L < sequence.longValue());
    }

    @SuppressWarnings("unchecked")
    private <T extends Appender<ILoggingEvent>> T getAppender(String appenderName) {
        return (T) LOGGER.getAppender(appenderName);
    }

    @SuppressWarnings("unchecked")
    private <T extends Encoder<ILoggingEvent>> T getEncoder(String appenderName) {
        return (T) this.<OutputStreamAppender<ILoggingEvent>>getAppender(appenderName).getEncoder();
    }

    private Map<String, Object> parseJson(final String text) throws IOException {
        return jsonFactory.createParser(text).readValueAs(new TypeReference<Map<String, Object>>() {
        });
    }
}
