/*
 * Copyright 2013-2025 the original author or authors.
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
package net.logstash.logback.marker;

import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Supplier;

import net.logstash.logback.appender.AsyncDisruptorAppender;

import ch.qos.logback.classic.AsyncAppender;
import com.fasterxml.jackson.core.JsonGenerator;
import org.slf4j.Marker;

/**
 * A {@link LogstashMarker} that defers the creation of another {@link LogstashMarker} until
 * the first time it is encoded.
 *
 * <p>Encoding (and therefore the deferred marker creation) can potentially take place
 * on another thread if an async appender (e.g. {@link AsyncAppender} or {@link AsyncDisruptorAppender}) is used.</p>
 *
 * <p>The deferred value will only be calculated once.
 * The single value supplied by the supplier will be reused every time the marker is written.
 * For example, if multiple appenders use a logstash encoder,
 * the supplier will be invoked when the first appender encodes the marker.
 * That same supplied value will be used when the next appender encodes the marker.</p>
 */
@SuppressWarnings("serial")
public class DeferredLogstashMarker extends LogstashMarker {

    public static final String DEFERRED_MARKER_NAME = "DEFERRED";

    /**
     * Supplier for the deferred marker
     */
    private final Supplier<? extends LogstashMarker> logstashMarkerSupplier;

    /**
     * Cached value of the marker returned by {@link #logstashMarkerSupplier}.
     * {@code null} until {@link #getSuppliedValue()} is first called.
     */
    private volatile LogstashMarker suppliedValue;

    public DeferredLogstashMarker(Supplier<? extends LogstashMarker> logstashMarkerSupplier) {
        super(DEFERRED_MARKER_NAME);
        this.logstashMarkerSupplier = Objects.requireNonNull(logstashMarkerSupplier, "logstashMarkerSupplier must not be null");
    }

    @Override
    public void writeTo(JsonGenerator generator) throws IOException {
        writeMarker(generator, getSuppliedValue());
    }

    /**
     * Get the deferred marker from the supplier or return it from the cache
     * if already initialized.
     * 
     * @return the deferred marker
     */
    private LogstashMarker getSuppliedValue() {
        if (suppliedValue == null) {
            synchronized (this) {
                if (suppliedValue == null) {
                    LogstashMarker logstashMarker = logstashMarkerSupplier.get();
                    if (logstashMarker == null) {
                        logstashMarker = Markers.empty();
                    }
                    suppliedValue = logstashMarker;
                }
            }
        }
        return suppliedValue;
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
                writeMarker(generator, i.next());
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
