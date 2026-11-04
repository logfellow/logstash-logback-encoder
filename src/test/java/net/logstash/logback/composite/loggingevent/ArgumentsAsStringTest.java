/*
 * Copyright 2013-2025 the original author or authors.
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
package net.logstash.logback.composite.loggingevent;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import net.logstash.logback.LogstashFormatter;
import net.logstash.logback.argument.StructuredArguments;
import net.logstash.logback.encoder.LogstashEncoder;
import net.logstash.logback.layout.LogstashLayout;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.spi.ContextAwareBase;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class ArgumentsAsStringTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Test
    public void retainsDefaultObjectSerialization() {
        ArgumentsJsonProvider provider = new ArgumentsJsonProvider();
        provider.setIncludeNonStructuredArguments(true);
        assertThat(render(provider, new Argument(false), 7, true, null))
                .isEqualTo(MAPPER.readTree("{\"arg0\":{\"secret\":\"secret\"},\"arg1\":7,\"arg2\":true,\"arg3\":null}"));
    }

    @Test
    public void excludesNonStructuredArgumentsByDefault() {
        assertThat(render(new ArgumentsJsonProvider(), new Argument(false)))
                .isEqualTo(MAPPER.readTree("{}"));
    }

    @Test
    public void writesStringRepresentationsWithoutReadingBeanProperties() throws ReflectiveOperationException {
        ArgumentsJsonProvider provider = new ArgumentsJsonProvider();
        provider.setIncludeNonStructuredArguments(true);
        setStringMode(provider, true);
        assertThat(render(provider, new Argument(true), 7, true, List.of(1, 2), null))
                .isEqualTo(MAPPER.readTree("{\"arg0\":\"summary\",\"arg1\":\"7\",\"arg2\":\"true\","
                        + "\"arg3\":\"[1, 2]\",\"arg4\":null}"));
    }

    @Test
    public void preservesStructuredArgumentsAndArgumentIndexes() throws ReflectiveOperationException {
        ArgumentsJsonProvider provider = new ArgumentsJsonProvider();
        provider.setIncludeNonStructuredArguments(true);
        setStringMode(provider, true);
        assertThat(render(provider, StructuredArguments.keyValue("structured", Map.of("key", 7)), new Argument(true)))
                .isEqualTo(MAPPER.readTree("{\"structured\":{\"key\":7},\"arg1\":\"summary\"}"));
    }

    @Test
    public void skipsExcludedStructuredArgumentsInStringMode() throws ReflectiveOperationException {
        ArgumentsJsonProvider provider = new ArgumentsJsonProvider();
        provider.setIncludeStructuredArguments(false);
        provider.setIncludeNonStructuredArguments(true);
        setStringMode(provider, true);
        assertThat(render(provider, StructuredArguments.keyValue("excluded", 7), new Argument(true)))
                .isEqualTo(MAPPER.readTree("{\"arg1\":\"summary\"}"));
    }

    @Test
    public void escapesStringRepresentations() throws ReflectiveOperationException {
        ArgumentsJsonProvider provider = new ArgumentsJsonProvider();
        provider.setIncludeNonStructuredArguments(true);
        setStringMode(provider, true);
        assertThat(render(provider, "quote \" and newline\n"))
                .isEqualTo(MAPPER.readTree("{\"arg0\":\"quote \\\" and newline\\n\"}"));
    }

    @Test
    public void retainsWrappingAndCustomFieldPrefix() throws ReflectiveOperationException {
        ArgumentsJsonProvider provider = new ArgumentsJsonProvider();
        provider.setIncludeNonStructuredArguments(true);
        provider.setFieldName("args");
        provider.setNonStructuredArgumentsFieldPrefix("custom");
        setStringMode(provider, true);
        assertThat(render(provider, new Argument(true), null))
                .isEqualTo(MAPPER.readTree("{\"args\":{\"custom0\":\"summary\",\"custom1\":null}}"));
    }

    @Test
    public void doesNotStringifyExcludedArguments() throws ReflectiveOperationException {
        ArgumentsJsonProvider provider = new ArgumentsJsonProvider();
        provider.setFieldName("args");
        setStringMode(provider, true);
        Object argument = new Object() {
            @Override
            public String toString() {
                throw new AssertionError("Excluded arguments must not be converted");
            }
        };
        assertThat(render(provider, argument)).isEqualTo(MAPPER.readTree("{}"));
    }

    @Test
    public void canRestoreObjectSerialization() throws ReflectiveOperationException {
        ArgumentsJsonProvider provider = new ArgumentsJsonProvider();
        provider.setIncludeNonStructuredArguments(true);
        setStringMode(provider, true);
        setStringMode(provider, false);
        assertThat(render(provider, new Argument(false)))
                .isEqualTo(MAPPER.readTree("{\"arg0\":{\"secret\":\"secret\"}}"));
    }

    @Test
    public void exposesThePropertyOnAllStandardEntryPoints() throws ReflectiveOperationException {
        Object[] beans = {new ArgumentsJsonProvider(), new LogstashFormatter(new ContextAwareBase()),
                new LogstashEncoder(), new LogstashLayout()};
        for (Object bean : beans) {
            assertThat(isStringMode(bean)).isFalse();
            setStringMode(bean, true);
            assertThat(isStringMode(bean)).isTrue();
            setStringMode(bean, false);
            assertThat(isStringMode(bean)).isFalse();
        }
    }

    @Test
    public void writesStringArgumentsThroughTheStandardLayout() throws ReflectiveOperationException {
        LogstashLayout layout = new LogstashLayout();
        layout.setIncludeNonStructuredArguments(true);
        setStringMode(layout, true);
        layout.start();
        try {
            assertThat(MAPPER.readTree(layout.doLayout(event(new Argument(true)))).get("arg0"))
                    .isEqualTo(MAPPER.readTree("\"summary\""));
        } finally {
            layout.stop();
        }
    }

    @Test
    public void configuresStandardEncoderFromXml() throws Exception {
        assertThat(encodeFromXml("<encoder class=\"net.logstash.logback.encoder.LogstashEncoder\">"
                + "<includeNonStructuredArguments>true</includeNonStructuredArguments>"
                + "<writeNonStructuredArgumentsAsString>true</writeNonStructuredArgumentsAsString></encoder>")
                .get("arg0")).isEqualTo(MAPPER.readTree("\"summary\""));
    }

    @Test
    public void configuresCompositeEncoderFromXml() throws Exception {
        assertThat(encodeFromXml("<encoder class=\"net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder\">"
                + "<providers><arguments><includeNonStructuredArguments>true</includeNonStructuredArguments>"
                + "<writeNonStructuredArgumentsAsString>true</writeNonStructuredArgumentsAsString>"
                + "</arguments></providers></encoder>")).isEqualTo(MAPPER.readTree("{\"arg0\":\"summary\"}"));
    }

    private static JsonNode render(ArgumentsJsonProvider provider, Object... arguments) {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = MAPPER.createGenerator(writer)) {
            generator.writeStartObject();
            provider.writeTo(generator, event(arguments));
            generator.writeEndObject();
        }
        return MAPPER.readTree(writer.toString());
    }

    private static void setStringMode(Object bean, boolean enabled) throws ReflectiveOperationException {
        // Exercise the public JavaBean property while keeping these same tests compilable on the base version.
        bean.getClass().getMethod("setWriteNonStructuredArgumentsAsString", boolean.class).invoke(bean, enabled);
    }

    private static boolean isStringMode(Object bean) throws ReflectiveOperationException {
        return (boolean) bean.getClass().getMethod("isWriteNonStructuredArgumentsAsString").invoke(bean);
    }

    @SuppressWarnings("unchecked")
    private static JsonNode encodeFromXml(String encoderXml) throws Exception {
        LoggerContext context = new LoggerContext();
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            String xml = "<configuration><appender name=\"test\" class=\"ch.qos.logback.core.ConsoleAppender\">"
                    + encoderXml + "</appender><root level=\"INFO\"><appender-ref ref=\"test\"/></root></configuration>";
            configurator.doConfigure(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            OutputStreamAppender<ILoggingEvent> appender = (OutputStreamAppender<ILoggingEvent>) context
                    .getLogger(Logger.ROOT_LOGGER_NAME).getAppender("test");
            assertThat(appender.getEncoder().isStarted()).isTrue();
            return MAPPER.readTree(appender.getEncoder().encode(event(new Argument(true))));
        } finally {
            context.stop();
        }
    }

    private static LoggingEvent event(Object... arguments) {
        LoggingEvent event = new LoggingEvent();
        event.setLoggerName("test");
        event.setThreadName("test");
        event.setLevel(Level.INFO);
        event.setMessage("message");
        event.setInstant(Instant.EPOCH);
        event.setMDCPropertyMap(Map.of());
        event.setArgumentArray(arguments);
        return event;
    }

    private static class Argument {
        private final boolean failOnPropertyAccess;

        Argument(boolean failOnPropertyAccess) {
            this.failOnPropertyAccess = failOnPropertyAccess;
        }

        public String getSecret() {
            if (failOnPropertyAccess) {
                throw new AssertionError("String mode must not read bean properties");
            }
            return "secret";
        }

        @Override
        public String toString() {
            return "summary";
        }
    }
}
