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
package net.logstash.logback;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

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
import net.logstash.logback.composite.UuidJsonProvider;
import net.logstash.logback.composite.loggingevent.ArgumentsJsonProvider;
import net.logstash.logback.composite.loggingevent.CallerDataJsonProvider;
import net.logstash.logback.composite.loggingevent.ContextNameJsonProvider;
import net.logstash.logback.composite.loggingevent.KeyValuePairsJsonProvider;
import net.logstash.logback.composite.loggingevent.LogLevelJsonProvider;
import net.logstash.logback.composite.loggingevent.LogLevelValueJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggerNameJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggingEventFormattedTimestampJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggingEventNestedJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggingEventPatternJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggingEventThreadNameJsonProvider;
import net.logstash.logback.composite.loggingevent.LogstashMarkersJsonProvider;
import net.logstash.logback.composite.loggingevent.MdcJsonProvider;
import net.logstash.logback.composite.loggingevent.MessageJsonProvider;
import net.logstash.logback.composite.loggingevent.RawMessageJsonProvider;
import net.logstash.logback.composite.loggingevent.SequenceJsonProvider;
import net.logstash.logback.composite.loggingevent.StackTraceJsonProvider;
import net.logstash.logback.composite.loggingevent.TagsJsonProvider;
import net.logstash.logback.composite.loggingevent.mdc.LongMdcEntryWriter;
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
import org.assertj.core.api.InstanceOfAssertFactories;
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

        assertThat(providers).hasSize(20);

        verifyCommonProviders(providers);

        verifyOutput(encoder);
    }

    @Test
    public void testLoggingEventCompositeJsonEncoderAppender() throws IOException {
        LoggingEventCompositeJsonEncoder encoder = getEncoder("loggingEventCompositeJsonEncoderAppender");
        List<JsonProvider<ILoggingEvent>> providers = encoder.getProviders().getProviders();

        assertThat(providers).hasSize(24);

        verifyCommonProviders(providers);

        assertThat(getInstance(providers, TestJsonProvider.class)).isNotNull();

        verifyOutput(encoder);
    }

    @Test
    public void testAppenderHasListener() throws IOException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        LoggingEventAsyncDisruptorAppender appender = getAppender("asyncAppender");
        Field listenersField = AsyncDisruptorAppender.class.getDeclaredField("listeners");
        listenersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<AppenderListener<ILoggingEvent>> listeners = (List<AppenderListener<ILoggingEvent>>) listenersField.get(appender);
        assertThat(listeners).hasSize(1);
    }


    @SuppressWarnings("unchecked")
    private void verifyCommonProviders(List<JsonProvider<ILoggingEvent>> providers) {
        LoggingEventFormattedTimestampJsonProvider timestampJsonProvider = getInstance(providers, LoggingEventFormattedTimestampJsonProvider.class);
        assertThat(timestampJsonProvider).isNotNull();
        assertThat(timestampJsonProvider.getFieldName()).isEqualTo("@timestamp");

        LogstashVersionJsonProvider<ILoggingEvent> versionJsonProvider = getInstance(providers, LogstashVersionJsonProvider.class);
        assertThat(versionJsonProvider).isNotNull();
        assertThat(versionJsonProvider.getFieldName()).isEqualTo("@version");

        MessageJsonProvider messageJsonProvider = getInstance(providers, MessageJsonProvider.class);
        assertThat(messageJsonProvider).isNotNull();
        assertThat(messageJsonProvider.getFieldName()).isEqualTo("customMessage");

        LoggerNameJsonProvider loggerNameJsonProvider = getInstance(providers, LoggerNameJsonProvider.class);
        assertThat(loggerNameJsonProvider).isNotNull();
        assertThat(loggerNameJsonProvider.getFieldName()).isEqualTo("logger_name");

        LoggingEventThreadNameJsonProvider threadNameJsonProvider = getInstance(providers, LoggingEventThreadNameJsonProvider.class);
        assertThat(threadNameJsonProvider).isNotNull();
        assertThat(threadNameJsonProvider.getFieldName()).isEqualTo("thread_name");

        LogLevelJsonProvider logLevelJsonProvider = getInstance(providers, LogLevelJsonProvider.class);
        assertThat(logLevelJsonProvider).isNotNull();
        assertThat(logLevelJsonProvider.getFieldName()).isEqualTo("level");

        LogLevelValueJsonProvider levelValueJsonProvider = getInstance(providers, LogLevelValueJsonProvider.class);
        assertThat(levelValueJsonProvider).isNotNull();
        assertThat(levelValueJsonProvider.getFieldName()).isEqualTo("level_value");

        CallerDataJsonProvider callerDataJsonProvider = getInstance(providers, CallerDataJsonProvider.class);
        assertThat(callerDataJsonProvider).isNotNull();
        assertThat(callerDataJsonProvider.getClassFieldName()).isEqualTo("class");
        assertThat(callerDataJsonProvider.getMethodFieldName()).isEqualTo("method");
        assertThat(callerDataJsonProvider.getFileFieldName()).isEqualTo("file");
        assertThat(callerDataJsonProvider.getLineFieldName()).isEqualTo("line");

        StackTraceJsonProvider stackTraceJsonProvider = getInstance(providers, StackTraceJsonProvider.class);
        assertThat(stackTraceJsonProvider).isNotNull();
        
        ShortenedThrowableConverter throwableConverter = (ShortenedThrowableConverter) stackTraceJsonProvider.getThrowableConverter();
        assertThat(throwableConverter).isNotNull();
        assertThat(throwableConverter.getMaxDepthPerThrowable()).isEqualTo(20);
        assertThat(throwableConverter.getMaxLength()).isEqualByComparingTo(1000);
        assertThat(throwableConverter.getShortenedClassNameLength()).isEqualByComparingTo(30);
        assertThat(throwableConverter.isRootCauseFirst()).isTrue();
        assertThat(throwableConverter.getExcludes()).hasSize(2);
        assertThat(throwableConverter.getExcludes()).element(0).isEqualTo("excluded1");
        assertThat(throwableConverter.getExcludes()).element(1).isEqualTo("excluded2");
        
        assertThat(throwableConverter.isInlineHash()).isTrue();

        assertThat(getInstance(providers, ContextJsonProvider.class)).isNotNull();
        assertThat(getInstance(providers, ContextNameJsonProvider.class)).isNotNull();

        MdcJsonProvider mdcJsonProvider = getInstance(providers, MdcJsonProvider.class);
        assertThat(mdcJsonProvider).isNotNull();
        assertThat(mdcJsonProvider.getIncludeMdcKeyNames()).containsExactly("included");
        assertThat(mdcJsonProvider.getMdcKeyFieldNames()).containsOnly(entry("key", "renamedKey"));
        assertThat(mdcJsonProvider.getMdcEntryWriters()).hasSize(1);
        assertThat(mdcJsonProvider.getMdcEntryWriters()).element(0).isExactlyInstanceOf(LongMdcEntryWriter.class);

        KeyValuePairsJsonProvider keyValuePairsJsonProvider = getInstance(providers, KeyValuePairsJsonProvider.class);
        assertThat(keyValuePairsJsonProvider).isNotNull();
        assertThat(keyValuePairsJsonProvider.getIncludeKeyNames()).containsExactly("included");
        assertThat(keyValuePairsJsonProvider.getKeyFieldNames()).containsOnly(entry("key", "renamedKey"));

        GlobalCustomFieldsJsonProvider<ILoggingEvent> globalCustomFieldsJsonProvider = getInstance(providers, GlobalCustomFieldsJsonProvider.class);
        assertThat(globalCustomFieldsJsonProvider).isNotNull();
        assertThat(globalCustomFieldsJsonProvider.getCustomFields()).isEqualTo("{\"customName\":\"customValue\"}");

        assertThat(getInstance(providers, TagsJsonProvider.class)).isNotNull();
        assertThat(getInstance(providers, LogstashMarkersJsonProvider.class)).isNotNull();

        LoggingEventPatternJsonProvider patternProvider = getInstance(providers, LoggingEventPatternJsonProvider.class);
        assertThat(patternProvider).isNotNull();
        assertThat(patternProvider.getPattern()).isEqualTo("{\"patternName\":\"patternValue\",\"relativeTime\":\"#asLong{%relative}\"}");
        
        LoggingEventNestedJsonProvider nestedJsonProvider = getInstance(providers, LoggingEventNestedJsonProvider.class);
        assertThat(nestedJsonProvider).isNotNull();
        assertThat(nestedJsonProvider.getFieldName()).isEqualTo("nested");
        
        RawMessageJsonProvider rawMessageJsonProvider = getInstance(nestedJsonProvider.getProviders().getProviders(), RawMessageJsonProvider.class);
        assertThat(rawMessageJsonProvider).isNotNull();
        assertThat(rawMessageJsonProvider.getFieldName()).isEqualTo("customRawMessage");

        ArgumentsJsonProvider argumentsJsonProvider = getInstance(providers, ArgumentsJsonProvider.class);
        assertThat(argumentsJsonProvider).isNotNull();

        UuidJsonProvider<ILoggingEvent> uuidProvider = getInstance(nestedJsonProvider.getProviders().getProviders(), UuidJsonProvider.class);
        assertThat(uuidProvider).isNotNull();
        assertThat(uuidProvider.getFieldName()).isEqualTo("id");
        assertThat(uuidProvider.getEthernet()).isEqualTo("00:C0:F0:3D:5B:7C");
        assertThat(uuidProvider.getStrategy()).isEqualTo(UuidJsonProvider.STRATEGY_TIME);

        SequenceJsonProvider sequenceJsonProvider = getInstance(providers, SequenceJsonProvider.class);
        assertThat(sequenceJsonProvider).isNotNull();
        assertThat(sequenceJsonProvider.getFieldName()).isEqualTo("sequenceNumberField");
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
        
        assertThat(output)
            .containsEntry("@version", "1")
            .containsEntry("customMessage", "message arg k1=v1 k2=[v2] v3")
            .containsEntry("logger_name", "n.l.l.ConfigurationTest")
            .containsEntry("context", "testContext")
            .containsKey("thread_name")
            .containsEntry("level", "INFO")
            .containsEntry("level_value", 20000)
            .containsKey("caller")
            .containsEntry("customName", "customValue")
            .containsEntry("patternName", "patternValue")
            .containsEntry("markerFieldName", "markerFieldValue")
            .containsEntry("prefix0", "arg")
            .containsEntry("k1", "v1")
            .containsEntry("k2", "v2")
            .containsEntry("k3", "v3");

        assertThat(output).extracting("relativeTime").isNotNull().isInstanceOf(Number.class);
        assertThat(output).extracting("@timestamp").isNotNull().isInstanceOf(String.class);
        assertThat(output).extracting("nested", as(InstanceOfAssertFactories.MAP))
            .containsEntry("customRawMessage", "message {} {} {} {}");
        
        assertThat(output).extracting("stack_trace", as(InstanceOfAssertFactories.STRING))
            .contains("n.l.logback.ConfigurationTest.verifyOutput");
        

        Number sequence = (Number) output.get("sequenceNumberField");
        assertThat(sequence).isNotNull();
        assertThat(sequence.longValue()).isPositive();
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
