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
package net.logstash.logback.composite.loggingevent;

import java.io.IOException;

import org.slf4j.Marker;

import com.fasterxml.jackson.core.JsonGenerator;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.marker.Markers;

/**
 * Provides logic for deprecated functionality.
 *
 * @deprecated Use the {@link LogstashMarkersJsonProvider}, and log events with {@link Markers} instead.
 */
@Deprecated
public class JsonMessageJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> {

    /**
     * Name of the {@link Marker} that indicates that the log event arguments should be appended to the
     * logstash json as an array with field value "json_message".
     *
     * @deprecated When logging, prefer using a {@link Markers#appendArray(String, Object...)} marker
     *             with fieldName = "json_message" and objects = an array of arguments instead.
     */
    @Deprecated
    public static final String JSON_MARKER_NAME = "JSON";

    public JsonMessageJsonProvider() {
        super();
        setFieldName("json_message");
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        final Marker marker = event.getMarker();
        if (marker != null && marker.contains(JSON_MARKER_NAME)) {
            generator.writeFieldName(getFieldName());
            generator.writeObject(event.getArgumentArray());
        }
    }

}
