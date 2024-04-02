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

import java.io.IOException;

import net.logstash.logback.fieldnames.LogstashCommonFieldNames;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Writes a version field as a string value (by default) or a numeric value (if {@link #isWriteAsInteger()} is true).
 * This is intended to be the logstash JSON format version.
 * 
 * By default, the version is {@value #DEFAULT_VERSION}.
 *
 * @param <Event> type of event ({@link ILoggingEvent} or {@link IAccessEvent}).
 */
public class LogstashVersionJsonProvider<Event extends DeferredProcessingAware> extends AbstractFieldJsonProvider<Event> implements FieldNamesAware<LogstashCommonFieldNames> {
    
    public static final String FIELD_VERSION = "@version";
    
    public static final String DEFAULT_VERSION = "1";
    
    private String version;
    private long versionAsInteger;
    
    /**
     * When false (the default), the version will be written as a string value.
     * When true, the version will be written as a numeric integer value.
     */
    private boolean writeAsInteger;
    
    public LogstashVersionJsonProvider() {
        setFieldName(FIELD_VERSION);
        setVersion(DEFAULT_VERSION);
    }

    @Override
    public void writeTo(JsonGenerator generator, Event event) throws IOException {
        if (writeAsInteger) {
            JsonWritingUtils.writeNumberField(generator, getFieldName(), versionAsInteger);
        } else {
            JsonWritingUtils.writeStringField(generator, getFieldName(), version);
        }
    }
    
    @Override
    public void setFieldNames(LogstashCommonFieldNames fieldNames) {
        setFieldName(fieldNames.getVersion());
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
        if (writeAsInteger) {
            this.versionAsInteger = Integer.parseInt(version);
        }
    }
    
    public boolean isWriteAsInteger() {
        return writeAsInteger;
    }
    
    public void setWriteAsInteger(boolean writeAsInteger) {
        this.writeAsInteger = writeAsInteger;
        if (writeAsInteger) {
            this.versionAsInteger = Integer.parseInt(version);
        }
    }
    
}
