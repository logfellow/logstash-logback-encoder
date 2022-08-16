/*
 * Copyright 2013-2022 the original author or authors.
 *
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
import java.util.Iterator;
import java.util.List;

import net.logstash.logback.composite.AbstractJsonProvider;
import net.logstash.logback.composite.JsonProvider;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import net.logstash.logback.util.LogbackUtils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import org.slf4j.Marker;

/**
 * A {@link JsonProvider} that processes {@link LogstashMarker}s
 * (generally created via {@link Markers}).
 */
public class LogstashMarkersJsonProvider extends AbstractJsonProvider<ILoggingEvent> {

    @SuppressWarnings("deprecation")
    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        if (LogbackUtils.isVersion13()) {
            writeLogstashMarkerIfNecessary(generator, event.getMarkerList());
        }
        else {
            writeLogstashMarkerIfNecessary(generator, event.getMarker());
        }
    }
    
    private void writeLogstashMarkerIfNecessary(JsonGenerator generator, List<Marker> markers) throws IOException {
        if (markers != null) {
            for (Marker marker: markers) {
                writeLogstashMarkerIfNecessary(generator, marker);
            }
        }
    }
    
    private void writeLogstashMarkerIfNecessary(JsonGenerator generator, Marker marker) throws IOException {
        if (marker != null) {
            if (isLogstashMarker(marker)) {
                ((LogstashMarker) marker).writeTo(generator);
            }
            
            if (marker.hasReferences()) {
                for (Iterator<?> i = marker.iterator(); i.hasNext();) {
                    Marker next = (Marker) i.next();
                    writeLogstashMarkerIfNecessary(generator, next);
                }
            }
        }
    }

    public static boolean isLogstashMarker(Marker marker) {
        return marker instanceof LogstashMarker;
    }
    
}
