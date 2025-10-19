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
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import net.logstash.logback.argument.StructuredArguments;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;


@ExtendWith(MockitoExtension.class)
public class ArgumentsJsonProviderTest {

    private final ArgumentsJsonProvider provider = new ArgumentsJsonProvider();

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private final StringWriter writer = new StringWriter();

    private JsonGenerator generator;

    @Mock
    private ILoggingEvent event;

    private static class Foo {
        private final String k6 = "v6";
        
        @SuppressWarnings("unused")
        public String getK6() {
            return k6;
        }
    }

    @BeforeEach
    public void setup() {
        
        generator = objectMapper.createGenerator(writer);
        
        Map<String, String> map = new LinkedHashMap<>();
        map.put("k4", "v4");
        map.put("k5", "v5");
        Object[] arguments = new Object[]{
                StructuredArguments.keyValue("k0", "v0"),
                StructuredArguments.keyValue("k1", "v1", "{0}=[{1}]"),
                StructuredArguments.value("k2", "v2"),
                StructuredArguments.array("k3", "v3a", "v3b"),
                StructuredArguments.entries(map),
                StructuredArguments.fields(new Foo()),
                StructuredArguments.raw("k7", "\"v7\""),
                "v8",
        };
        when(event.getArgumentArray()).thenReturn(arguments);
    }

    @Test
    public void testUnwrapped() {
        generator.writeStartObject();
        provider.writeTo(generator, event);
        generator.writeEndObject();
        
        generator.flush();
        
        assertThat(writer).hasToString(
                "{"
                        + "\"k0\":\"v0\","
                        + "\"k1\":\"v1\","
                        + "\"k2\":\"v2\","
                        + "\"k3\":[\"v3a\",\"v3b\"],"
                        + "\"k4\":\"v4\","
                        + "\"k5\":\"v5\","
                        + "\"k6\":\"v6\","
                        + "\"k7\":\"v7\""
                + "}");
    }

    @Test
    public void testWrapped() {
        provider.setFieldName("args");

        generator.writeStartObject();
        provider.writeTo(generator, event);
        generator.writeEndObject();
        
        generator.flush();
        
        assertThat(writer).hasToString(
                "{"
                    + "\"args\":{"
                        + "\"k0\":\"v0\","
                        + "\"k1\":\"v1\","
                        + "\"k2\":\"v2\","
                        + "\"k3\":[\"v3a\",\"v3b\"],"
                        + "\"k4\":\"v4\","
                        + "\"k5\":\"v5\","
                        + "\"k6\":\"v6\","
                        + "\"k7\":\"v7\""
                    + "}"
                + "}");
    }


    @Test
    public void testIncludeNonStructuredArguments() {
        provider.setIncludeNonStructuredArguments(true);

        generator.writeStartObject();
        provider.writeTo(generator, event);
        generator.writeEndObject();
        
        generator.flush();
        
        assertThat(writer).hasToString(
                "{"
                        + "\"k0\":\"v0\","
                        + "\"k1\":\"v1\","
                        + "\"k2\":\"v2\","
                        + "\"k3\":[\"v3a\",\"v3b\"],"
                        + "\"k4\":\"v4\","
                        + "\"k5\":\"v5\","
                        + "\"k6\":\"v6\","
                        + "\"k7\":\"v7\","
                        + "\"arg7\":\"v8\""
                + "}");
    }

    @Test
    public void testIncludeNonStructuredArgumentsAndCustomPrefix() {
        provider.setIncludeNonStructuredArguments(true);
        provider.setNonStructuredArgumentsFieldPrefix("prefix");

        generator.writeStartObject();
        provider.writeTo(generator, event);
        generator.writeEndObject();
        
        generator.flush();
        
        assertThat(writer).hasToString(
                "{"
                        + "\"k0\":\"v0\","
                        + "\"k1\":\"v1\","
                        + "\"k2\":\"v2\","
                        + "\"k3\":[\"v3a\",\"v3b\"],"
                        + "\"k4\":\"v4\","
                        + "\"k5\":\"v5\","
                        + "\"k6\":\"v6\","
                        + "\"k7\":\"v7\","
                        + "\"prefix7\":\"v8\""
                + "}");
    }

}
