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
package net.logstash.logback.decorate.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class CharacterEscapesDecoratorTest {

    @Test
    public void basicEscape() {
        CharacterEscapesDecorator decorator = new CharacterEscapesDecorator();
        decorator.addEscape(new CharacterEscapesDecorator.Escape("\n", "_"));
        decorator.addEscape(new CharacterEscapesDecorator.Escape(" ", "==="));
        decorator.addEscape(new CharacterEscapesDecorator.Escape("y", "!"));
        decorator.addEscape(new CharacterEscapesDecorator.Escape("ё", "?"));
        
        StringWriter writer = new StringWriter();
        ObjectMapper objectMapper = JsonMapper.builder(decorator.decorate(JsonFactory.builder()).build()).build();
        JsonGenerator generator = objectMapper.createGenerator(writer);
        
        generator.writeStartObject();
        generator.writeStringProperty("message", "My message\nМоё сообщение");
        generator.writeEndObject();
        generator.flush();
        
        assertThat(writer.toString()).isEqualTo("{\"message\":\"M!===message_Мо?===сообщение\"}");
    }

    @Test
    public void noEscapeSequence() {
        CharacterEscapesDecorator.Escape noEscapeSequence = new CharacterEscapesDecorator.Escape();
        noEscapeSequence.setTarget("z");

        CharacterEscapesDecorator decorator = new CharacterEscapesDecorator();
        decorator.addEscape(noEscapeSequence);
        decorator.addEscape(new CharacterEscapesDecorator.Escape(10, "==="));

        StringWriter writer = new StringWriter();
        ObjectMapper objectMapper = JsonMapper.builder(decorator.decorate(JsonFactory.builder()).build()).build();
        JsonGenerator generator = objectMapper.createGenerator(writer);
        
        generator.writeStartObject();
        generator.writeStringProperty("message", ".z.\n.y.");
        generator.writeEndObject();
        generator.flush();

        assertThat(writer.toString()).isEqualTo("{\"message\":\"..===.y.\"}");
    }

    @Test
    public void noStandard() {
        CharacterEscapesDecorator decorator = new CharacterEscapesDecorator();
        decorator.setIncludeStandardAsciiEscapesForJSON(false);
        
        StringWriter writer = new StringWriter();
        ObjectMapper objectMapper = JsonMapper.builder(decorator.decorate(JsonFactory.builder()).build()).build();
        JsonGenerator generator = objectMapper.createGenerator(writer);
        
        generator.writeStartObject();
        generator.writeStringProperty("message", "foo\nbar");
        generator.writeEndObject();
        generator.flush();
        
        assertThat(writer.toString()).isEqualTo("{\"message\":\"foo\nbar\"}");
    }

}
