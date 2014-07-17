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
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.commons.lang.ObjectUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A marker that first converts an object to a {@link JsonNode}, and then
 * appends the fields of the {@link JsonNode} into the logstash event.
 * <p>
 * Unless the object is already a {@link JsonNode), this may not be very efficient, so prefer using one of the other {@link LogstashMarker}s. It is included here for convenience where performance is
 * not a concern.
 * <p>
 * The object will be converted to a {@link JsonNode} via {@link ObjectMapper#convertValue(Object, Class)};
 * <p>
 * For example, if the converted JsonNode is
 * 
 * <pre>
 * {@code
 * {
 *     name1 : "value1",
 *     name2 : 5,
 *     name3 : [1, 2, 3],
 *     name4 : {
 *         name5 : 6
 *     }
 * }
 * </pre>
 * <p>
 * Then the name1, name2, name3, name4 fields will be added to the json for the logstash event.
 * <p>
 * For example:
 * 
 * <pre>
 * {@code
 * {
 *     @timestamp : "2014-07-09T19:05:29.629-07:00",
 *     @version : 1,
 *     name1 : "value1",
 *     name2 : 5,
 *     name3 : [1, 2, 3],
 *     name4 : {
 *         name5 : 6
 *     }
 * }
 * </pre>
 */
@SuppressWarnings("serial")
public class ObjectFieldsAppendingMarker extends LogstashMarker {
    
    public static final String MARKER_NAME = LogstashMarker.MARKER_NAME_PREFIX + "OBJECT_FIELDS";
    
    private final Object object;
    
    public ObjectFieldsAppendingMarker(Object object) {
        super(MARKER_NAME);
        this.object = object;
    }
    
    @Override
    public void writeTo(JsonGenerator generator, ObjectMapper mapper) throws IOException {
        if (object != null) {
            JsonNode jsonNode = mapper.convertValue(object, JsonNode.class);
            for (Iterator<Entry<String, JsonNode>> fields = jsonNode.fields(); fields.hasNext();) {
                Entry<String, JsonNode> field = fields.next();
                generator.writeFieldName(field.getKey());
                generator.writeTree(field.getValue());
            }
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof ObjectFieldsAppendingMarker)) {
            return false;
        }
        
        ObjectFieldsAppendingMarker other = (ObjectFieldsAppendingMarker) obj;
        return ObjectUtils.equals(this.object, other.object);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + super.hashCode();
        result = prime * result + (this.object == null ? 0 : this.object.hashCode());
        return result;
    }
}
