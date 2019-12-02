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
package net.logstash.logback.decorate;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class CharacterEscapesJsonFactoryBuilderDecoratorTest {

    @Test
    public void basicEscape() throws Exception {
        CharacterEscapesJsonFactoryBuilderDecorator decorator = new CharacterEscapesJsonFactoryBuilderDecorator();
        decorator.addEscape(new CharacterEscapesJsonFactoryBuilderDecorator.Escape("\n", "_"));
        decorator.addEscape(new CharacterEscapesJsonFactoryBuilderDecorator.Escape(" ", "==="));
        decorator.addEscape(new CharacterEscapesJsonFactoryBuilderDecorator.Escape("y", "!"));
        decorator.addEscape(new CharacterEscapesJsonFactoryBuilderDecorator.Escape("ё", "?"));
        
        StringWriter writer = new StringWriter();
        ObjectMapper objectMapper = JsonMapper.builder(decorator.decorate(JsonFactory.builder()).build()).build();
        JsonGenerator generator = objectMapper.createGenerator(writer);
        
        generator.writeStartObject();
        generator.writeStringField("message", "My message\nМоё сообщение");
        generator.writeEndObject();
        generator.flush();
        
        assertThat(writer.toString()).isEqualTo("{\"message\":\"M!===message_Мо?===сообщение\"}");
    }

    @Test
    public void noEscapeSequence() throws Exception {
        CharacterEscapesJsonFactoryBuilderDecorator.Escape noEscapeSequence = new CharacterEscapesJsonFactoryBuilderDecorator.Escape();
        noEscapeSequence.setTarget("z");

        CharacterEscapesJsonFactoryBuilderDecorator decorator = new CharacterEscapesJsonFactoryBuilderDecorator();
        decorator.addEscape(noEscapeSequence);
        decorator.addEscape(new CharacterEscapesJsonFactoryBuilderDecorator.Escape(10, "==="));

        StringWriter writer = new StringWriter();
        ObjectMapper objectMapper = JsonMapper.builder(decorator.decorate(JsonFactory.builder()).build()).build();
        JsonGenerator generator = objectMapper.createGenerator(writer);
        
        generator.writeStartObject();
        generator.writeStringField("message", ".z.\n.y.");
        generator.writeEndObject();
        generator.flush();

        assertThat(writer.toString()).isEqualTo("{\"message\":\"..===.y.\"}");
    }

    @Test
    public void noStandard() throws Exception {
        CharacterEscapesJsonFactoryBuilderDecorator decorator = new CharacterEscapesJsonFactoryBuilderDecorator();
        decorator.setIncludeStandardAsciiEscapesForJSON(false);
        
        StringWriter writer = new StringWriter();
        ObjectMapper objectMapper = JsonMapper.builder(decorator.decorate(JsonFactory.builder()).build()).build();
        JsonGenerator generator = objectMapper.createGenerator(writer);
        
        generator.writeStartObject();
        generator.writeStringField("message", "foo\nbar");
        generator.writeEndObject();
        generator.flush();
        
        assertThat(writer.toString()).isEqualTo("{\"message\":\"foo\nbar\"}");
    }

}
