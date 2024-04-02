/*
 * Copyright 2013-2023 the original author or authors.
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
package net.logstash.logback.composite;

import java.io.IOException;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Contributes to the JSON output being written for the given Event.
 *
 * @param <Event> type of event ({@link ILoggingEvent} or {@link IAccessEvent}).
 */
public interface JsonProvider<Event extends DeferredProcessingAware> extends ContextAware {

    /**
     * Writes information about the event, to the given generator.
     *
     * <p>When called, the generator is assumed to be within a JSON object context
     * (i.e. this provider should write fields and their values to the generator).
     * Upon return, the generator should be within the same JSON object context.
     * 
     * @param generator the {@link JsonGenerator} to produce JSON content
     * @param event the event to convert into JSON
     * @throws IOException if an I/O error occurs
     */
    void writeTo(JsonGenerator generator, Event event) throws IOException;

    /**
     * Gives the provider a chance to perform more deferred processing
     * (in addition to what is already provided by
     * {@link DeferredProcessingAware#prepareForDeferredProcessing()}).
     * 
     * @param event the event to prepare for deferred processing
     */
    void prepareForDeferredProcessing(Event event);

    /**
     * Start the provider after all configuration properties are set.
     */
    void start();

    /**
     * Stop the provider
     */
    void stop();

    /**
     * Report whether the provider is started or not.
     * @return {@code true} if the provider is started, {@code false} otherwise.
     */
    boolean isStarted();
}
