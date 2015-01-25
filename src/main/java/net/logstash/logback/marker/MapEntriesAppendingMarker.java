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

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A marker that appends the entries from a map into the logstash json event.
 * The keys are converted to a {@link String} via {@link Object#toString()}, and used as the field names.
 * The values are converted using an {@link ObjectMapper}.
 * <p>
 * For example, if the map contains is
 * 
 * <pre>
 * {@code
 * name1= a String "value1",
 * name2= an Integer 5,
 * name3= an array containing [1, 2, 3],
 * name4= a map containing  name5=6 
 * }
 * </pre>
 * <p>
 * Then the name1, name2, name3, name4 fields will be added to the json for the logstash event.
 * <p>
 * For example:
 * 
 * <pre>
 * {
 *     name1 : "value1",
 *     name2 : 5,
 *     name3 : [1, 2, 3],
 *     name4 : {
 *         name5 : 6
 *     }
 * }
 * <p>
 * 
 * </pre>
 */
@SuppressWarnings("serial")
public class MapEntriesAppendingMarker extends LogstashMarker {
    
    public static final String MARKER_NAME = LogstashMarker.MARKER_NAME_PREFIX + "MAP_FIELDS";
    
    /**
     * The map from which entries will be appended to the logstash json event.
     */
    private final Map<?, ?> map;
    
    public MapEntriesAppendingMarker(Map<?, ?> map) {
        super(MARKER_NAME);
        this.map = map;
    }
    
    @Override
    public void writeTo(JsonGenerator generator) throws IOException {
        if (map != null) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                generator.writeFieldName(entry.getKey().toString());
                generator.writeObject(entry.getValue());
            }
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof MapEntriesAppendingMarker)) {
            return false;
        }
        
        MapEntriesAppendingMarker other = (MapEntriesAppendingMarker) obj;
        return ObjectUtils.equals(this.map, other.map);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + super.hashCode();
        result = prime * result + (this.map == null ? 0 : this.map.hashCode());
        return result;
    }
}
