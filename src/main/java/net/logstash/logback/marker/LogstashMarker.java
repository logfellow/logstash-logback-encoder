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

import org.slf4j.Marker;
import org.slf4j.helpers.LogstashBasicMarker;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A {@link Marker} that is known and understood by the logstash logback encoder.
 * <p>
 * In particular these markers are used to write data into the logstash json event via {@link #writeTo(JsonGenerator, ObjectMapper)}.
 */
@SuppressWarnings("serial")
public abstract class LogstashMarker extends LogstashBasicMarker {
    
    public static final String MARKER_NAME_PREFIX = "LS_";
    
    public LogstashMarker(String name) {
        super(name);
    }
    
    /**
     * Adds the given marker as a reference, and returns this marker.
     * <p>
     * This can be used to chain markers together fluently on a log line. For example:
     * 
     * <pre>
     * {@code
     * {
     *     import static net.logstash.logback.marker.Markers.*
     *     
     *     logger.info(append("name1", "value1).with(append("name2", "value2")), "log message");
     * }
     * </pre>
     */
    @SuppressWarnings("unchecked")
    public <T extends LogstashMarker> T with(Marker reference) {
        add(reference);
        return (T) this;
    }
    
    /**
     * Writes the data associated with this marker to the given {@link JsonGenerator}.
     * <p>
     * The {@link ObjectMapper} can be used to write objects if necessary. In particular {@link ObjectMapper#writeValue(JsonGenerator, Object)},
     * {@link ObjectMapper#writeTree(JsonGenerator, com.fasterxml.jackson.databind.JsonNode)}, and {@link ObjectMapper#writeTree(JsonGenerator, com.fasterxml.jackson.core.TreeNode)} can be useful.
     * 
     */
    public abstract void writeTo(JsonGenerator generator, ObjectMapper mapper) throws IOException;
    
}
