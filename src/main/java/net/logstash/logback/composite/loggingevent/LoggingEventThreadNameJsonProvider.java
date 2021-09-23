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
package net.logstash.logback.composite.loggingevent;

import net.logstash.logback.composite.AbstractThreadNameJsonProvider;
import net.logstash.logback.composite.JsonProvider;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * {@link JsonProvider} producing a single JSON field with the {@link ILoggingEvent#getThreadName()}.
 * 
 * @author brenuart
 */
public class LoggingEventThreadNameJsonProvider extends AbstractThreadNameJsonProvider<ILoggingEvent> {

    @Override
    protected String getThreadName(ILoggingEvent event) {
        return event.getThreadName();
    }
}
