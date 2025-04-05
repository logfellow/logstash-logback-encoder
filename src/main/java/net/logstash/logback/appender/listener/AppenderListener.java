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
package net.logstash.logback.appender.listener;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.DeferredProcessingAware;

/**
 * Listens to an appender.
 *
 * For example, a listener implementation could be created for metrics or dynamic error handling.
 */
public interface AppenderListener<Event extends DeferredProcessingAware> {

    /**
     * Called when the given appender is started.
     *
     * @param appender the appender that was started
     */
    default void appenderStarted(Appender<Event> appender) {
    }

    /**
     * Called when the given appender is stopped.
     *
     * @param appender the appender that was stopped
     */
    default void appenderStopped(Appender<Event> appender) {
    }

    /**
     * Called when the given event was successfully appended by the given appender.
     *
     * Note that for Asynchronous appenders, this generally means that the event was
     * accepted for processing, but hasn't finished processing yet.
     *
     * @param appender the appender when successfully appended the event
     * @param event the event that was appended
     * @param durationInNanos the time (in nanoseconds) it took to append the event
     */
    default void eventAppended(Appender<Event> appender, Event event, long durationInNanos) {
    }

    /**
     * Called when the given event was failed to be appended by the given appender.
     *
     * @param appender the appender when successfully appended the event
     * @param event the event that was appended
     * @param reason what caused the failure
     */
    default void eventAppendFailed(Appender<Event> appender, Event event, Throwable reason) {
    }

}
