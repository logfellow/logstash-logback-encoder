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

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Outputs an incrementing sequence number.
 * Useful for determining if log events get lost along the transport chain.
 * 
 * @deprecated use {@link net.logstash.logback.composite.SequenceJsonProvider} instead
 */
@Deprecated
public class SequenceJsonProvider extends net.logstash.logback.composite.SequenceJsonProvider<ILoggingEvent> {

    public static final String FIELD_SEQUENCE = net.logstash.logback.composite.SequenceJsonProvider.FIELD_SEQUENCE;

    @Override
    public void start() {
        addWarn(this.getClass().getName() + " is deprecated, use " + net.logstash.logback.composite.SequenceJsonProvider.class.getName() + " instead.");
        super.start();
    }
}
