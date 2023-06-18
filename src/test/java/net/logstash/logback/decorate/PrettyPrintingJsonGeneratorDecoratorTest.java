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
package net.logstash.logback.decorate;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;


class PrettyPrintingJsonGeneratorDecoratorTest {

    @Test
    void defaultOptions() throws IOException {
        PrettyPrintingJsonGeneratorDecorator decorator = new PrettyPrintingJsonGeneratorDecorator();

        StringWriter writer = new StringWriter();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonGenerator generator = decorator.decorate(objectMapper.createGenerator(writer));

        generator.writeObject(Collections.singletonMap("key1", "value1"));
        generator.writeObject(Collections.singletonMap("key2", "value2"));

        generator.flush();
        writer.flush();
        assertThat(writer.toString()).isEqualTo("{\n  \"key1\" : \"value1\"\n}{\n  \"key2\" : \"value2\"\n}");
    }

    @Test
    void customRootSeparator() throws IOException {
        PrettyPrintingJsonGeneratorDecorator decorator = new PrettyPrintingJsonGeneratorDecorator();
        decorator.setRootSeparator(" ");

        StringWriter writer = new StringWriter();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonGenerator generator = decorator.decorate(objectMapper.createGenerator(writer));

        generator.writeObject(Collections.singletonMap("key1", "value1"));
        generator.writeObject(Collections.singletonMap("key2", "value2"));

        generator.flush();
        writer.flush();
        assertThat(writer.toString()).isEqualTo("{\n  \"key1\" : \"value1\"\n} {\n  \"key2\" : \"value2\"\n}");
    }

    @Test
    void customRootSeparatorWithSpace() throws IOException {
        PrettyPrintingJsonGeneratorDecorator decorator = new PrettyPrintingJsonGeneratorDecorator();
        decorator.setRootSeparator("[SPACE]");

        StringWriter writer = new StringWriter();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonGenerator generator = decorator.decorate(objectMapper.createGenerator(writer));

        generator.writeObject(Collections.singletonMap("key1", "value1"));
        generator.writeObject(Collections.singletonMap("key2", "value2"));

        generator.flush();
        writer.flush();
        assertThat(writer.toString()).isEqualTo("{\n  \"key1\" : \"value1\"\n} {\n  \"key2\" : \"value2\"\n}");
    }

    @Test
    void noSpacesInObjectEntries() throws IOException {
        PrettyPrintingJsonGeneratorDecorator decorator = new PrettyPrintingJsonGeneratorDecorator();
        decorator.setSpacesInObjectEntries(false);

        StringWriter writer = new StringWriter();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonGenerator generator = decorator.decorate(objectMapper.createGenerator(writer));

        generator.writeObject(Collections.singletonMap("key1", "value1"));
        generator.writeObject(Collections.singletonMap("key2", "value2"));

        generator.flush();
        writer.flush();
        assertThat(writer.toString()).isEqualTo("{\n  \"key1\":\"value1\"\n}{\n  \"key2\":\"value2\"\n}");
    }
}
