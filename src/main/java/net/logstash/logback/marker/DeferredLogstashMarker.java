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
import java.util.Objects;
import java.util.function.Supplier;

import ch.qos.logback.classic.AsyncAppender;
import net.logstash.logback.appender.AsyncDisruptorAppender;
import org.slf4j.Marker;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * A {@link LogstashMarker} that defers the creation of another {@link LogstashMarker} until encoding time.
 *
 * <p>Encoding (and therefore the deferred marker creation) can potentially take place
 * on another thread if an async appender (e.g. {@link AsyncAppender} or {@link AsyncDisruptorAppender}) is used.</p>
 */
public class DeferredLogstashMarker extends LogstashMarker {

    public static final String DEFERRED_MARKER_NAME = "DEFERRED";

    private final Supplier<? extends LogstashMarker>  logstashMarkerSupplier;

    public DeferredLogstashMarker(Supplier<? extends LogstashMarker> logstashMarkerSupplier) {
        super(DEFERRED_MARKER_NAME);
        this.logstashMarkerSupplier = Objects.requireNonNull(logstashMarkerSupplier, "logstashMarkerSupplier must not be null");
    }

    @Override
    public void writeTo(JsonGenerator generator) throws IOException {
        writeMarker(generator, logstashMarkerSupplier.get());
    }

    private void writeMarker(JsonGenerator generator, Marker marker) throws IOException {
        if (marker == null) {
            return;
        }
        if (marker instanceof LogstashMarker) {
            ((LogstashMarker) marker).writeTo(generator);
        }

        if (marker.hasReferences()) {
            for (Iterator<Marker> i = marker.iterator(); i.hasNext();) {
                Marker next = i.next();
                writeMarker(generator, next);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
