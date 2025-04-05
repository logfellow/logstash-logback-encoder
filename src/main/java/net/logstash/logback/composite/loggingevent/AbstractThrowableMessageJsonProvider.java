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
package net.logstash.logback.composite.loggingevent;

import java.io.IOException;

import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.JsonWritingUtils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Logs an exception message for a given logging event. Which exception to be
 * logged depends on the subclass's implementation of
 * {@link #getThrowable(ILoggingEvent)}.
 */
public abstract class AbstractThrowableMessageJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> {

    protected AbstractThrowableMessageJsonProvider(String fieldName) {
        setFieldName(fieldName);
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        IThrowableProxy throwable = getThrowable(event);
        if (throwable != null) {
            String throwableMessage = throwable.getMessage();
            JsonWritingUtils.writeStringField(generator, getFieldName(), throwableMessage);
        }
    }

    /**
     * @param event the event being logged, never {@code null}
     * @return the throwable to use, or {@code null} if no appropriate throwable is
     *         available
     * @throws NullPointerException if {@code event} is {@code null}
     */
    protected abstract IThrowableProxy getThrowable(ILoggingEvent event);
}
