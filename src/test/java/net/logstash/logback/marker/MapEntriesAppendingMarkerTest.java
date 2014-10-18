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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public class MapEntriesAppendingMarkerTest {
    
    private static final JsonFactory FACTORY = new MappingJsonFactory().enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);
    
    @Test
    public void testWriteTo() throws IOException {
        
        Map<String, String> map = new HashMap<String, String>();
        map.put("myField", "value");
        
        StringWriter writer = new StringWriter();
        JsonGenerator generator = FACTORY.createGenerator(writer);
        
        MapEntriesAppendingMarker marker = Markers.appendEntries(map);
        generator.writeStartObject();
        marker.writeTo(generator);
        generator.writeEndObject();
        generator.flush();
        
        assertThat(writer.toString(), is("{\"myField\":\"value\"}"));
    }
    
    @Test
    public void testEquals() {
        Map<String, String> map = new HashMap<String, String>();
        
        assertThat(Markers.appendEntries(map), is(Markers.appendEntries(map)));
        
        Map<String, String> map2 = new HashMap<String, String>();
        map2.put("foo", "bar");
        assertThat(Markers.appendEntries(map), not(is(Markers.appendEntries(map2))));
    }
    
    @Test
    public void testHashCode() {
        Map<String, String> map = new HashMap<String, String>();
        
        assertThat(Markers.appendEntries(map).hashCode(), is(Markers.appendEntries(map).hashCode()));
        
        Map<String, String> map2 = new HashMap<String, String>();
        map2.put("foo", "bar");
        assertThat(Markers.appendEntries(map).hashCode(), not(is(Markers.appendEntries(map2).hashCode())));
    }
    
}
