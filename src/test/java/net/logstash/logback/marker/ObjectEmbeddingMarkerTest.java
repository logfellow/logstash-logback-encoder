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
package net.logstash.logback.marker;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectEmbeddingMarkerTest {
    
    private static final JsonFactory FACTORY = new MappingJsonFactory().enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);
    private static final ObjectMapper MAPPER = new ObjectMapper(FACTORY);
    
    public static class MyClass {
        private String myField;
        
        public MyClass(String myField) {
            this.myField = myField;
        }

        public String getMyField() {
            return myField;
        }
        
        public void setMyField(String myField) {
            this.myField = myField;
        }
    }
    
    @Test
    public void testWriteTo() throws IOException {
        
        MyClass myObject = new MyClass("value");
        
        StringWriter writer = new StringWriter();
        JsonGenerator generator = FACTORY.createGenerator(writer);
        
        ObjectEmbeddingMarker marker = Markers.embed(myObject);
        generator.writeStartObject();
        marker.writeTo(generator, MAPPER);
        generator.writeEndObject();
        generator.flush();
        
        assertThat(writer.toString(), is("{\"myField\":\"value\"}"));
    }
    
    @Test
    public void testEquals() {
        MyClass myObject = new MyClass("value");
        
        assertThat(Markers.embed(myObject), is(Markers.embed(myObject)));
        
        assertThat(Markers.embed(myObject), not(is(Markers.embed(new MyClass("value1")))));
    }
    

    @Test
    public void testHashCode() {
        MyClass myObject = new MyClass("value");
        
        assertThat(Markers.embed(myObject).hashCode(), is(Markers.embed(myObject).hashCode()));
        
        assertThat(Markers.embed(myObject).hashCode(), not(is(Markers.embed(new MyClass("value1")).hashCode())));
    }
    

}
