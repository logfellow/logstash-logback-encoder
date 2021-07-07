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

import java.io.IOException;

import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.fieldnames.LogstashFieldNames;

import ch.qos.logback.classic.pattern.ExtendedThrowableProxyConverter;
import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import com.fasterxml.jackson.core.JsonGenerator;

public class StackTraceJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> implements FieldNamesAware<LogstashFieldNames> {

    public static final String FIELD_STACK_TRACE = "stack_trace";

    /**
     * Used to format throwables as Strings.
     *
     * Uses an {@link ExtendedThrowableProxyConverter} from logstash by default.
     *
     * Consider using a
     * {@link net.logstash.logback.stacktrace.ShortenedThrowableConverter ShortenedThrowableConverter}
     * for more customization options.
     */
    private ThrowableHandlingConverter throwableConverter = new ExtendedThrowableProxyConverter();

    public StackTraceJsonProvider() {
        setFieldName(FIELD_STACK_TRACE);
    }

    @Override
    public void start() {
        this.throwableConverter.start();
        super.start();
    }

    @Override
    public void stop() {
        this.throwableConverter.stop();
        super.stop();
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            JsonWritingUtils.writeStringField(generator, getFieldName(), throwableConverter.convert(event));
        }
    }

    @Override
    public void setFieldNames(LogstashFieldNames fieldNames) {
        setFieldName(fieldNames.getStackTrace());
    }

    public ThrowableHandlingConverter getThrowableConverter() {
        return throwableConverter;
    }
    public void setThrowableConverter(ThrowableHandlingConverter throwableConverter) {
        this.throwableConverter = throwableConverter;
    }
}
