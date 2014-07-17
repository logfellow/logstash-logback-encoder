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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * A marker that appends a single field into the logstash json event.
 */
@SuppressWarnings("serial")
public abstract class SingleFieldAppendingMarker extends LogstashMarker {
    
    public static final String MARKER_NAME_PREFIX = LogstashMarker.MARKER_NAME_PREFIX + "APPEND_";
    
    /**
     * Name of the field to append.
     * 
     * Note that the value of the field is provided by subclasses via {@link #writeFieldValue(JsonGenerator, ObjectMapper)}.
     */
    private final String fieldName;

    public SingleFieldAppendingMarker(String markerName, String fieldName) {
        super(markerName);
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName must not be null");
        }
        this.fieldName = fieldName;
    }
    
    public String getFieldName() {
        return fieldName;
    }

    public void writeTo(JsonGenerator generator, ObjectMapper mapper) throws IOException {
        writeFieldName(generator);
        writeFieldValue(generator, mapper);
    }

    /**
     * Writes the field name to the generator.
     */
    protected void writeFieldName(JsonGenerator generator) throws IOException {
        generator.writeFieldName(getFieldName());
    }

    /**
     * Writes the field value to the generator.
     */
    protected abstract void writeFieldValue(JsonGenerator generator, ObjectMapper mapper) throws IOException;
    
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof SingleFieldAppendingMarker)) {
            return false;
        }

        SingleFieldAppendingMarker other = (SingleFieldAppendingMarker) obj;
        return this.fieldName.equals(other.fieldName);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + super.hashCode();
        result = prime * result + this.fieldName.hashCode();
        return result;
    }
}
