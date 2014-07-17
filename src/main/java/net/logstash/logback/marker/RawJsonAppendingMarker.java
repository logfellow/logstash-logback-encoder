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
 * A marker that writes a raw json value to the logstash json event
 * under a given field name.
 * <p>
 * The raw string is written verbatim without any modifications, but assuming it must constitute a single legal JSON value (number, string, boolean, null, Array or List)
 */
@SuppressWarnings("serial")
public class RawJsonAppendingMarker extends SingleFieldAppendingMarker {
    
    public static final String MARKER_NAME = SingleFieldAppendingMarker.MARKER_NAME_PREFIX + "RAW";
    
    /**
     * The raw json value to write as the field value.
     */
    private final String rawJson;
    
    public RawJsonAppendingMarker(String fieldName, String rawJson) {
        super(MARKER_NAME, fieldName);
        if (rawJson == null) {
            throw new IllegalArgumentException("rawJson must not be null");
        }
        this.rawJson = rawJson;
    }
    
    @Override
    protected void writeFieldValue(JsonGenerator generator, ObjectMapper mapper) throws IOException {
        generator.writeRawValue(rawJson);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof RawJsonAppendingMarker)) {
            return false;
        }
        
        RawJsonAppendingMarker other = (RawJsonAppendingMarker) obj;
        return ObjectUtils.equals(this.rawJson, other.rawJson);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + super.hashCode();
        result = prime * result + this.rawJson.hashCode();
        return result;
    }
}
