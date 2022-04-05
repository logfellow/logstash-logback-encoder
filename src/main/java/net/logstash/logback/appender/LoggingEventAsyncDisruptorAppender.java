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
package net.logstash.logback.appender;

import net.logstash.logback.appender.listener.AppenderListener;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.lmax.disruptor.RingBuffer;

/**
 * A {@link DelegatingAsyncDisruptorAppender} for {@link ILoggingEvent}s.
 */
public class LoggingEventAsyncDisruptorAppender extends DelegatingAsyncDisruptorAppender<ILoggingEvent, AppenderListener<ILoggingEvent>> {

    /**
     * Set to true if the caller data should be captured before publishing the event
     * to the {@link RingBuffer}
     */
    private boolean includeCallerData;

    protected void prepareForDeferredProcessing(ILoggingEvent event) {
        super.prepareForDeferredProcessing(event);
        if (includeCallerData) {
            event.getCallerData();
        }
    }

    public boolean isIncludeCallerData() {
        return includeCallerData;
    }

    public void setIncludeCallerData(boolean includeCallerData) {
        this.includeCallerData = includeCallerData;
    }

}
