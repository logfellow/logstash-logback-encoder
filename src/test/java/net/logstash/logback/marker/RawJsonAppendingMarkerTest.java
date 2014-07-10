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

public class RawJsonAppendingMarkerTest {
    
    private static final JsonFactory FACTORY = new MappingJsonFactory().enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);
    private static final ObjectMapper MAPPER = new ObjectMapper(FACTORY);
    
    @Test
    public void testWriteTo() throws IOException {
        
        String rawJson = "{\"myField\":\"value\"}";
        
        StringWriter writer = new StringWriter();
        JsonGenerator generator = FACTORY.createGenerator(writer);
        
        RawJsonAppendingMarker marker = Markers.appendRaw("rawJson", rawJson);
        generator.writeStartObject();
        marker.writeTo(generator, MAPPER);
        generator.writeEndObject();
        generator.flush();
        
        assertThat(writer.toString(), is("{\"rawJson\":{\"myField\":\"value\"}}"));
    }
    
    @Test
    public void testEquals() {
        String rawJson = "{\"myField\":\"value\"}";

        assertThat(Markers.appendRaw("rawJson", rawJson), is(Markers.appendRaw("rawJson", rawJson)));
        
        assertThat(Markers.appendRaw("rawJson", rawJson), not(is(Markers.appendRaw("rawJson", ""))));
        
        assertThat(Markers.appendRaw("rawJson", rawJson), not(is(Markers.appendRaw("myDifferentObject", rawJson))));
    }
    

    @Test
    public void testHashCode() {
        String rawJson = "{\"myField\":\"value\"}";
        
        assertThat(Markers.appendRaw("rawJson", rawJson).hashCode(), is(Markers.appendRaw("rawJson", rawJson).hashCode()));
        
        assertThat(Markers.appendRaw("rawJson", rawJson).hashCode(), not(is(Markers.appendRaw("rawJson", "").hashCode())));
        
        assertThat(Markers.appendRaw("rawJson", rawJson).hashCode(), not(is(Markers.appendRaw("myDifferentObject", rawJson)).hashCode()));
    }
    

}
