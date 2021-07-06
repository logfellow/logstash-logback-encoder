/*
 * Copyright 2013-2021 the original author or authors.
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
package net.logstash.logback.argument;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;

import ch.qos.logback.classic.spi.LoggingEvent;
import net.logstash.logback.marker.EmptyLogstashMarker;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * A {@link StructuredArgument} that defers the creation of another {@link StructuredArgument} until
 * the first time its value is needed.
 *
 * <p>The value is needed in the following conditions:</p>
 * <ul>
 *     <li>When {@link LoggingEvent#getFormattedMessage()} is called</li>
 *     <li>When {@link LoggingEvent#prepareForDeferredProcessing()} ()} is called
 *         (async appenders call this prior to dispatching the event to another thread)</li>
 *     <li>When the {@link LoggingEvent} is encoded</li>
 * </ul>
 *
 * <p>The deferred value will always be created in the thread calling the {@link Logger}
 * (even if an async appender is used) since the values of structured arguments are needed
 * when {@link LoggingEvent#prepareForDeferredProcessing()} is called
 * (by async appenders prior to dispatching the event to another thread).
 *
 * <p>The deferred value will only be calculated once.
 * The single value supplied by the supplier will be reused every time the structured argument is written.
 * For example, if multiple appenders use a logstash encoder,
 * the supplier will be invoked when the first appender encodes the marker.
 * That same supplied value will be used when the next appender encodes the marker.</p>
 */
public class DeferredStructuredArgument implements StructuredArgument {

    private final Supplier<? extends StructuredArgument> structureArgumentSupplier;

    private volatile StructuredArgument suppliedValue;

    public DeferredStructuredArgument(Supplier<? extends StructuredArgument> structureArgumentSupplier) {
        this.structureArgumentSupplier = Objects.requireNonNull(structureArgumentSupplier, "structureArgumentSupplier must not be null");
    }

    @Override
    public void writeTo(JsonGenerator generator) throws IOException {
        getSuppliedValue().writeTo(generator);
    }

    private StructuredArgument getSuppliedValue() {
        if (suppliedValue == null) {
            synchronized (this) {
                if (suppliedValue == null) {
                    StructuredArgument structuredArgument = structureArgumentSupplier.get();
                    if (structuredArgument == null) {
                        structuredArgument = new EmptyLogstashMarker();
                    }
                    suppliedValue = structuredArgument;
                }
            }
        }
        return suppliedValue;
    }

}
