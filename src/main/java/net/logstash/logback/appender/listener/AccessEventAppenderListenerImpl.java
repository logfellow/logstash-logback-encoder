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
package net.logstash.logback.appender.listener;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.Appender;

/**
 * Implementation of {@link AppenderListener} for {@link IAccessEvent}s that does nothing.
 * 
 * @deprecated Replaced by default methods in interface.
 */
@Deprecated
public class AccessEventAppenderListenerImpl implements AppenderListener<IAccessEvent> {

    @Override
    public void appenderStarted(Appender<IAccessEvent> appender) {
    }

    @Override
    public void appenderStopped(Appender<IAccessEvent> appender) {
    }

    @Override
    public void eventAppended(Appender<IAccessEvent> appender, IAccessEvent event, long durationInNanos) {
    }

    @Override
    public void eventAppendFailed(Appender<IAccessEvent> appender, IAccessEvent event, Throwable reason) {
    }
}
