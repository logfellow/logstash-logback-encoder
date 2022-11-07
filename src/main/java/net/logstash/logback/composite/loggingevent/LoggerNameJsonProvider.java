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
package net.logstash.logback.composite.loggingevent;

import java.io.IOException;
import java.util.Objects;

import net.logstash.logback.abbreviator.DefaultTargetLengthAbbreviator;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.fieldnames.LogstashFieldNames;
import net.logstash.logback.util.LogbackUtils;

import ch.qos.logback.classic.pattern.Abbreviator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.joran.spi.DefaultClass;
import com.fasterxml.jackson.core.JsonGenerator;

public class LoggerNameJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> implements FieldNamesAware<LogstashFieldNames> {

    public static final String FIELD_LOGGER_NAME = "logger_name";

    /**
     * Abbreviator that will shorten the logger classname
     */
    private Abbreviator abbreviator = new DefaultTargetLengthAbbreviator();
    
    
    public LoggerNameJsonProvider() {
        setFieldName(FIELD_LOGGER_NAME);
    }
    
    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        if (!isStarted()) {
            throw new IllegalStateException("Generator is not started");
        }
        JsonWritingUtils.writeStringField(generator, getFieldName(), abbreviator.abbreviate(event.getLoggerName()));
    }
    
    @Override
    public void setFieldNames(LogstashFieldNames fieldNames) {
        setFieldName(fieldNames.getLogger());
    }

    public int getShortenedLoggerNameLength() {
        if (this.abbreviator instanceof DefaultTargetLengthAbbreviator) {
            return ((DefaultTargetLengthAbbreviator) this.abbreviator).getTargetLength();
        }
        else {
            throw new IllegalStateException("Cannot invoke getShortenedLoggerNameLength on non default abbreviator");
        }
    }

    public void setShortenedLoggerNameLength(int length) {
        if (this.abbreviator instanceof DefaultTargetLengthAbbreviator) {
            ((DefaultTargetLengthAbbreviator) this.abbreviator).setTargetLength(length);
        }
        else {
            throw new IllegalStateException("Cannot set shortenedLoggerNameLength on non default Abbreviator");
        }
    }
    
    @Override
    public void start() {
        LogbackUtils.start(getContext(), this.abbreviator);
        super.start();
    }
    
    @Override
    public void stop() {
        super.stop();
        LogbackUtils.stop(this.abbreviator);
    }
    
    
    @DefaultClass(DefaultTargetLengthAbbreviator.class)
    public void setAbbreviator(Abbreviator abbreviator) {
        this.abbreviator = Objects.requireNonNull(abbreviator);
    }
    
    public Abbreviator getAbbreviator() {
        return abbreviator;
    }
}
