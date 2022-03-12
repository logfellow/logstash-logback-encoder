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
package net.logstash.logback.encoder;

import net.logstash.logback.composite.AbstractCompositeJsonFormatter;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.composite.loggingevent.LoggingEventCompositeJsonFormatter;
import net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.joran.spi.DefaultClass;

public class LoggingEventCompositeJsonEncoder extends CompositeJsonEncoder<ILoggingEvent> {
    
    @Override
    protected AbstractCompositeJsonFormatter<ILoggingEvent> createFormatter() {
        return new LoggingEventCompositeJsonFormatter(this);
    }
    
    @Override
    @DefaultClass(LoggingEventJsonProviders.class)
    public void setProviders(JsonProviders<ILoggingEvent> jsonProviders) {
        super.setProviders(jsonProviders);
    }
    
}
