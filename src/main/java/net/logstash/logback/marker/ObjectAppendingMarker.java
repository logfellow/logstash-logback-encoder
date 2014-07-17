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

import org.apache.commons.lang.ObjectUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A marker that converts an Object into an appropriate JSON type (number, string, object, array)
 * and writes it to the logstash json event under a given fieldName.
 * <p>
 * 
 * For example, to append a string field, use a String object as the object. To append a numeric field, use a Number object as the object. To append an array field, use an array as the object. To
 * append an object field, use some other Object as the object.
 * <p>
 * 
 * An {@link ObjectMapper} is used to convert/write the value, so as long as the {@link ObjectMapper} can convert the object, you're good.
 * 
 */
@SuppressWarnings("serial")
public class ObjectAppendingMarker extends SingleFieldAppendingMarker {
    
    public static final String MARKER_NAME = SingleFieldAppendingMarker.MARKER_NAME_PREFIX + "OBJECT";
    
    /**
     * The object to write as the field's value.
     * Can be a {@link String}, {@link Number}, array, or some other object that can be processed by an {@link ObjectMapper}
     */
    private final Object object;
    
    public ObjectAppendingMarker(String fieldName, Object object) {
        super(MARKER_NAME, fieldName);
        this.object = object;
    }
    
    @Override
    protected void writeFieldValue(JsonGenerator generator, ObjectMapper mapper) throws IOException {
        mapper.writeValue(generator, object);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof ObjectAppendingMarker)) {
            return false;
        }
        
        ObjectAppendingMarker other = (ObjectAppendingMarker) obj;
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
