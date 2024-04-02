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

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.DeferredProcessingAware;


/**
 * Base {@link JsonProvider} that provides default implementations for standard methods.
 *
 * @param <Event> type of event ({@link ILoggingEvent} or {@link IAccessEvent}).
 */
public abstract class AbstractJsonProvider<Event extends DeferredProcessingAware> extends ContextAwareBase implements JsonProvider<Event> {

    private volatile boolean started;

    @Override
    public void start() {
        started = true;
    }
    
    @Override
    public void stop() {
        started = false;
    }
    
    @Override
    public boolean isStarted() {
        return started;
    }
    
    @Override
    public void prepareForDeferredProcessing(Event event) {
    }

    
    /**
     * Assert the component is started and throw {@link IllegalStateException} if not
     * 
     * @throws IllegalStateException if component is not started
     */
    protected void assertIsStarted() {
        if (!isStarted()) {
            throw new IllegalStateException(this.getClass().getSimpleName() + " is not started");
        }
    }
}
