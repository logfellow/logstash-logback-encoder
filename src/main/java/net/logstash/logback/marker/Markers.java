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

import java.util.Map;

import org.slf4j.Marker;

/**
 * Convenience class for constructing various {@link LogstashMarker}s used to add
 * fields into the logstash event.
 * <p>
 * This creates a somewhat fluent interface that can be used to create markers.
 * <p>
 * For example:
 * 
 * <pre>
 * {@code
 * import static net.logstash.logback.marker.Markers.*
 *     
 * logger.info(append("name1", "value1"), "log message");
 * logger.info(append("name1", "value1").with(append("name2", "value2")), "log message");
 * logger.info(appendEntries(myMap), "log message");
 * }
 * </pre>
 */
public class Markers {
    
    private Markers() {
    }
    
    /**
     * @see MapEntriesAppendingMarker
     */
    public static LogstashMarker appendEntries(Map<?, ?> map) {
        return new MapEntriesAppendingMarker(map);
    }
    
    /**
     * @see ObjectFieldsAppendingMarker
     */
    public static LogstashMarker appendFields(Object object) {
        return new ObjectFieldsAppendingMarker(object);
    }
    
    /**
     * @see ObjectAppendingMarker
     */
    public static LogstashMarker append(String fieldName, Object object) {
        return new ObjectAppendingMarker(fieldName, object);
    }
    
    /**
     * @see ObjectAppendingMarker
     */
    public static LogstashMarker appendArray(String fieldName, Object... objects) {
        return new ObjectAppendingMarker(fieldName, objects);
    }
    
    /**
     * @see RawJsonAppendingMarker
     */
    public static LogstashMarker appendRaw(String fieldName, String rawJsonValue) {
        return new RawJsonAppendingMarker(fieldName, rawJsonValue);
    }
    
}
