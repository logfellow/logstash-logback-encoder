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
package net.logstash.logback.decorate;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.json.JsonMapper;


class PrettyPrintingDecoratorTest {

    @Test
    void defaultOptions() {
        PrettyPrintingDecorator<JsonMapper, JsonMapper.Builder> decorator = new PrettyPrintingDecorator<>();

        StringWriter writer = new StringWriter();
        JsonGenerator generator = decorator.decorate(JsonMapper.builder()).build().createGenerator(writer);

        generator.writePOJO(Collections.singletonMap("key1", "value1"));
        generator.writePOJO(Collections.singletonMap("key2", "value2"));

        generator.flush();
        writer.flush();
        assertThat(writer.toString()).isEqualTo("{\n  \"key1\" : \"value1\"\n}{\n  \"key2\" : \"value2\"\n}");
    }

    @Test
    void customRootSeparator() {
        PrettyPrintingDecorator<JsonMapper, JsonMapper.Builder> decorator = new PrettyPrintingDecorator<>();
        decorator.setRootSeparator(" ");

        StringWriter writer = new StringWriter();
        JsonGenerator generator = decorator.decorate(JsonMapper.builder()).build().createGenerator(writer);

        generator.writePOJO(Collections.singletonMap("key1", "value1"));
        generator.writePOJO(Collections.singletonMap("key2", "value2"));

        generator.flush();
        writer.flush();
        assertThat(writer.toString()).isEqualTo("{\n  \"key1\" : \"value1\"\n} {\n  \"key2\" : \"value2\"\n}");
    }

    @Test
    void customRootSeparatorWithSpace() {
        PrettyPrintingDecorator<JsonMapper, JsonMapper.Builder> decorator = new PrettyPrintingDecorator<>();
        decorator.setRootSeparator("[SPACE]");

        StringWriter writer = new StringWriter();
        JsonGenerator generator = decorator.decorate(JsonMapper.builder()).build().createGenerator(writer);

        generator.writePOJO(Collections.singletonMap("key1", "value1"));
        generator.writePOJO(Collections.singletonMap("key2", "value2"));

        generator.flush();
        writer.flush();
        assertThat(writer.toString()).isEqualTo("{\n  \"key1\" : \"value1\"\n} {\n  \"key2\" : \"value2\"\n}");
    }

    @Test
    void noSpacesInObjectEntries() {
        PrettyPrintingDecorator<JsonMapper, JsonMapper.Builder> decorator = new PrettyPrintingDecorator<>();
        decorator.setSpacesInObjectEntries(false);

        StringWriter writer = new StringWriter();
        JsonGenerator generator = decorator.decorate(JsonMapper.builder()).build().createGenerator(writer);

        generator.writePOJO(Collections.singletonMap("key1", "value1"));
        generator.writePOJO(Collections.singletonMap("key2", "value2"));

        generator.flush();
        writer.flush();
        assertThat(writer.toString()).isEqualTo("{\n  \"key1\":\"value1\"\n}{\n  \"key2\":\"value2\"\n}");
    }
}
