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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public class CharacterEscapesJsonFactoryDecoratorTest {

    @Test
    public void basicEscape() throws Exception {
        CharacterEscapesJsonFactoryDecorator decorator = new CharacterEscapesJsonFactoryDecorator();
        decorator.addEscape(new CharacterEscapesJsonFactoryDecorator.Escape("\n", "_"));
        decorator.addEscape(new CharacterEscapesJsonFactoryDecorator.Escape(" ", "==="));
        decorator.addEscape(new CharacterEscapesJsonFactoryDecorator.Escape("y", "!"));
        decorator.addEscape(new CharacterEscapesJsonFactoryDecorator.Escape("ё", "?"));
        
        StringWriter writer = new StringWriter();
        JsonFactory factory = decorator.decorate(new MappingJsonFactory());
        JsonGenerator generator = factory.createGenerator(writer);
        
        generator.writeStartObject();
        generator.writeStringField("message", "My message\nМоё сообщение");
        generator.writeEndObject();
        generator.flush();
        
        assertThat(writer.toString()).isEqualTo("{\"message\":\"M!===message_Мо?===сообщение\"}");
    }

    @Test
    public void noEscapeSequence() throws Exception {
        CharacterEscapesJsonFactoryDecorator.Escape noEscapeSequence = new CharacterEscapesJsonFactoryDecorator.Escape();
        noEscapeSequence.setTarget("z");

        CharacterEscapesJsonFactoryDecorator decorator = new CharacterEscapesJsonFactoryDecorator();
        decorator.addEscape(noEscapeSequence);
        decorator.addEscape(new CharacterEscapesJsonFactoryDecorator.Escape(10, "==="));

        StringWriter writer = new StringWriter();
        JsonFactory factory = decorator.decorate(new MappingJsonFactory());
        JsonGenerator generator = factory.createGenerator(writer);
        
        generator.writeStartObject();
        generator.writeStringField("message", ".z.\n.y.");
        generator.writeEndObject();
        generator.flush();

        assertThat(writer.toString()).isEqualTo("{\"message\":\"..===.y.\"}");
    }

    @Test
    public void noStandard() throws Exception {
        CharacterEscapesJsonFactoryDecorator decorator = new CharacterEscapesJsonFactoryDecorator();
        decorator.setIncludeStandardAsciiEscapesForJSON(false);
        
        StringWriter writer = new StringWriter();
        JsonFactory factory = decorator.decorate(new MappingJsonFactory());
        JsonGenerator generator = factory.createGenerator(writer);
        
        generator.writeStartObject();
        generator.writeStringField("message", "foo\nbar");
        generator.writeEndObject();
        generator.flush();
        
        assertThat(writer.toString()).isEqualTo("{\"message\":\"foo\nbar\"}");
    }

}
