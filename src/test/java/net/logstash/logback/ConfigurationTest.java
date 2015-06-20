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
package net.logstash.logback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import net.logstash.logback.composite.ContextJsonProvider;
import net.logstash.logback.composite.JsonProvider;
import net.logstash.logback.composite.LogstashVersionJsonProvider;
import net.logstash.logback.composite.loggingevent.CallerDataJsonProvider;
import net.logstash.logback.composite.loggingevent.ContextMapJsonProvider;
import net.logstash.logback.composite.loggingevent.GlobalCustomFieldsJsonProvider;
import net.logstash.logback.composite.loggingevent.JsonMessageJsonProvider;
import net.logstash.logback.composite.loggingevent.LogLevelJsonProvider;
import net.logstash.logback.composite.loggingevent.LogLevelValueJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggerNameJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggingEventFormattedTimestampJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggingEventPatternJsonProvider;
import net.logstash.logback.composite.loggingevent.LogstashMarkersJsonProvider;
import net.logstash.logback.composite.loggingevent.MdcJsonProvider;
import net.logstash.logback.composite.loggingevent.MessageJsonProvider;
import net.logstash.logback.composite.loggingevent.RawMessageJsonProvider;
import net.logstash.logback.composite.loggingevent.StackTraceJsonProvider;
import net.logstash.logback.composite.loggingevent.TagsJsonProvider;
import net.logstash.logback.composite.loggingevent.ThreadNameJsonProvider;
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder;
import net.logstash.logback.marker.Markers;
import net.logstash.logback.stacktrace.ShortenedThrowableConverter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.read.ListAppender;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public class ConfigurationTest {

    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(ConfigurationTest.class);

    private final ListAppender<ILoggingEvent> listAppender = (ListAppender<ILoggingEvent>) LOGGER.getAppender("listAppender");

    private final JsonFactory jsonFactory = new MappingJsonFactory();

    @Before
    public void setup() {
        listAppender.list.clear();
    }

    @Test
    public void testLogstashEncoderAppender() throws IOException {
        LoggingEventCompositeJsonEncoder encoder = getEncoder("logstashEncoderAppender");
        List<JsonProvider<ILoggingEvent>> providers = encoder.getProviders().getProviders();
        Assert.assertEquals(18, providers.size());

        verifyCommonProviders(providers);

        verifyOutput(encoder);
    }

    @Test
    public void testLoggingEventCompositeJsonEncoderAppender() throws UnsupportedEncodingException, IOException {
        LoggingEventCompositeJsonEncoder encoder = getEncoder("loggingEventCompositeJsonEncoderAppender");
        List<JsonProvider<ILoggingEvent>> providers = encoder.getProviders().getProviders();
        Assert.assertEquals(19, providers.size());

        verifyCommonProviders(providers);

        Assert.assertNotNull(getInstance(providers, TestJsonProvider.class));

        verifyOutput(encoder);
    }


    private void verifyCommonProviders(List<JsonProvider<ILoggingEvent>> providers) {
        LoggingEventFormattedTimestampJsonProvider timestampJsonProvider = getInstance(providers, LoggingEventFormattedTimestampJsonProvider.class);
        Assert.assertNotNull(timestampJsonProvider);
        Assert.assertEquals("@timestamp", timestampJsonProvider.getFieldName());

        LogstashVersionJsonProvider<ILoggingEvent> versionJsonProvider = getInstance(providers, LogstashVersionJsonProvider.class);
        Assert.assertNotNull(versionJsonProvider);
        Assert.assertEquals("@version", versionJsonProvider.getFieldName());

        MessageJsonProvider messageJsonProvider = getInstance(providers, MessageJsonProvider.class);
        Assert.assertNotNull(messageJsonProvider);
        Assert.assertEquals("customMessage", messageJsonProvider.getFieldName());

        LoggerNameJsonProvider loggerNameJsonProvider = getInstance(providers, LoggerNameJsonProvider.class);
        Assert.assertNotNull(loggerNameJsonProvider);
        Assert.assertEquals("logger_name", loggerNameJsonProvider.getFieldName());

        ThreadNameJsonProvider threadNameJsonProvider = getInstance(providers, ThreadNameJsonProvider.class);
        Assert.assertNotNull(threadNameJsonProvider);
        Assert.assertEquals("thread_name", threadNameJsonProvider.getFieldName());

        LogLevelJsonProvider logLevelJsonProvider = getInstance(providers, LogLevelJsonProvider.class);
        Assert.assertNotNull(logLevelJsonProvider);
        Assert.assertEquals("level", logLevelJsonProvider.getFieldName());

        LogLevelValueJsonProvider levelValueJsonProvider = getInstance(providers, LogLevelValueJsonProvider.class);
        Assert.assertNotNull(levelValueJsonProvider);
        Assert.assertEquals("level_value", levelValueJsonProvider.getFieldName());

        CallerDataJsonProvider callerDataJsonProvider = getInstance(providers, CallerDataJsonProvider.class);
        Assert.assertNotNull(callerDataJsonProvider);
        Assert.assertEquals("caller", callerDataJsonProvider.getFieldName());
        Assert.assertEquals("class", callerDataJsonProvider.getClassFieldName());
        Assert.assertEquals("method", callerDataJsonProvider.getMethodFieldName());
        Assert.assertEquals("file", callerDataJsonProvider.getFileFieldName());
        Assert.assertEquals("line", callerDataJsonProvider.getLineFieldName());

        StackTraceJsonProvider stackTraceJsonProvider = getInstance(providers, StackTraceJsonProvider.class);
        Assert.assertNotNull(stackTraceJsonProvider);
        ShortenedThrowableConverter throwableConverter = (ShortenedThrowableConverter) stackTraceJsonProvider.getThrowableConverter();
        Assert.assertEquals(20, throwableConverter.getMaxDepthPerThrowable());
        Assert.assertEquals(1000, throwableConverter.getMaxLength());
        Assert.assertEquals(30, throwableConverter.getShortenedClassNameLength());
        Assert.assertTrue(throwableConverter.isRootCauseFirst());
        Assert.assertEquals("excluded", throwableConverter.getExcludes().get(0));

        Assert.assertNotNull(getInstance(providers, ContextJsonProvider.class));
        Assert.assertNotNull(getInstance(providers, JsonMessageJsonProvider.class));

        MdcJsonProvider mdcJsonProvider = getInstance(providers, MdcJsonProvider.class);
        Assert.assertNotNull(mdcJsonProvider);
        Assert.assertEquals("included", mdcJsonProvider.getIncludeMdcKeyNames().get(0));

        Assert.assertNotNull(getInstance(providers, ContextMapJsonProvider.class));

        GlobalCustomFieldsJsonProvider<ILoggingEvent> globalCustomFieldsJsonProvider = getInstance(providers, GlobalCustomFieldsJsonProvider.class);
        Assert.assertNotNull(globalCustomFieldsJsonProvider);
        Assert.assertEquals("{\"customName\":\"customValue\"}", globalCustomFieldsJsonProvider.getCustomFields());

        Assert.assertNotNull(getInstance(providers, TagsJsonProvider.class));
        Assert.assertNotNull(getInstance(providers, LogstashMarkersJsonProvider.class));

        LoggingEventPatternJsonProvider patternProvider = getInstance(providers, LoggingEventPatternJsonProvider.class);
        Assert.assertEquals("{\"patternName\":\"patternValue\",\"relativeTime\":\"#asLong{%relative}\"}", patternProvider.getPattern());
        Assert.assertNotNull(patternProvider);
        
        RawMessageJsonProvider rawMessageJsonProvider = getInstance(providers, RawMessageJsonProvider.class);
        Assert.assertNotNull(rawMessageJsonProvider);
        Assert.assertEquals("customRawMessage", rawMessageJsonProvider.getFieldName());
    }

    private <T extends JsonProvider<ILoggingEvent>> T getInstance(List<JsonProvider<ILoggingEvent>> providers, Class<T> clazz) {
        for (JsonProvider<ILoggingEvent> jsonProvider : providers) {
            if (clazz.isInstance(jsonProvider)) {
                return clazz.cast(jsonProvider);
            }
        }
        return null;
    }

    private void verifyOutput(LoggingEventCompositeJsonEncoder encoder) throws IOException, UnsupportedEncodingException {
        LOGGER.info(Markers.append("markerFieldName", "markerFieldValue"), "message {}", "arg", new Throwable());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        encoder.init(outputStream);
        encoder.doEncode(listAppender.list.get(0));

        Map<String, Object> output = parseJson(outputStream.toString("UTF-8"));
        Assert.assertNotNull(output.get("@timestamp"));
        Assert.assertEquals(1, output.get("@version"));
        Assert.assertEquals("message arg", output.get("customMessage"));
        Assert.assertEquals("message {}", output.get("customRawMessage"));
        Assert.assertEquals("n.l.l.ConfigurationTest", output.get("logger_name"));
        Assert.assertNotNull(output.get("thread_name"));
        Assert.assertEquals("INFO", output.get("level"));
        Assert.assertEquals(20000, output.get("level_value"));
        Assert.assertNotNull(output.get("caller"));
        Assert.assertTrue(((String) output.get("stack_trace")).contains("n.l.logback.ConfigurationTest.verifyOutput"));
        Assert.assertEquals("customValue", output.get("customName"));
        Assert.assertEquals("patternValue", output.get("patternName"));
        Assert.assertEquals("markerFieldValue", output.get("markerFieldName"));
        Assert.assertTrue(output.get("relativeTime") instanceof Number);
    }


    @SuppressWarnings("unchecked")
    private <T extends Encoder<ILoggingEvent>> T getEncoder(String appenderName) {
        OutputStreamAppender<ILoggingEvent> appender = (OutputStreamAppender<ILoggingEvent>) LOGGER.getAppender(appenderName);
        return (T) appender.getEncoder();
    }

    private Map<String, Object> parseJson(final String text) throws IOException {
        return jsonFactory.createParser(text).readValueAs(new TypeReference<Map<String, Object>>() {
        });
    }
}
