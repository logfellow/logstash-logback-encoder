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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class MapEntriesAppendingMarkerTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();
    
    @Test
    public void testWriteTo() throws IOException {
        
        Map<String, String> map = new HashMap<String, String>();
        map.put("myField", "value");
        
        StringWriter writer = new StringWriter();
        JsonGenerator generator = MAPPER.createGenerator(writer);
        
        LogstashMarker marker = Markers.appendEntries(map);
        generator.writeStartObject();
        marker.writeTo(generator);
        generator.writeEndObject();
        generator.flush();
        
        assertThat(writer.toString()).isEqualTo("{\"myField\":\"value\"}");
    }
    
    @Test
    public void testEquals() {
        Map<String, String> map = new HashMap<String, String>();
        
        assertThat(Markers.appendEntries(map)).isEqualTo(Markers.appendEntries(map));
        
        Map<String, String> map2 = new HashMap<String, String>();
        map2.put("foo", "bar");
        assertThat(Markers.appendEntries(map)).isNotEqualTo(Markers.appendEntries(map2));
    }
    
    @Test
    public void testHashCode() {
        Map<String, String> map = new HashMap<String, String>();
        
        assertThat(Markers.appendEntries(map).hashCode()).isEqualTo(Markers.appendEntries(map).hashCode());
        
        Map<String, String> map2 = new HashMap<String, String>();
        map2.put("foo", "bar");
        assertThat(Markers.appendEntries(map).hashCode()).isNotEqualTo(Markers.appendEntries(map2).hashCode());
    }
    
}
