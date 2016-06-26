/**
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

import com.fasterxml.jackson.core.JsonGenerator;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.DeferredProcessingAware;

/**
 * Writes a version field as a numeric value (by default) or a string value (if {@link #isWriteAsString()}).
 * This is intended to be the logstash JSON format version.
 * 
 * By default, the version is {@value #DEFAULT_VERSION}.
 *
 * @param <Event> type of event ({@link ILoggingEvent} or {@link IAccessEvent}).
 */
public class LogstashVersionJsonProvider<Event extends DeferredProcessingAware> extends AbstractFieldJsonProvider<Event> implements FieldNamesAware<LogstashCommonFieldNames> {
    
    public static final String FIELD_VERSION = "@version";
    
    public static final int DEFAULT_VERSION = 1;
    
    private int version;
    private String versionAsString;
    
    /**
     * When true, the version will be written as a string value instead of a numeric value. 
     */
    private boolean writeAsString;
    
    public LogstashVersionJsonProvider() {
        setFieldName(FIELD_VERSION);
        setVersion(DEFAULT_VERSION);
    }

    @Override
    public void writeTo(JsonGenerator generator, Event event) throws IOException {
        if (writeAsString) {
            JsonWritingUtils.writeStringField(generator, getFieldName(), versionAsString);
        } else {
            JsonWritingUtils.writeNumberField(generator, getFieldName(), version);
        }
    }
    
    @Override
    public void setFieldNames(LogstashCommonFieldNames fieldNames) {
        setFieldName(fieldNames.getVersion());
    }
    
    public int getVersion() {
        return version;
    }
    
    public void setVersion(int version) {
        this.version = version;
        this.versionAsString = Integer.toString(version);
    }
    
    public boolean isWriteAsString() {
        return writeAsString;
    }
    public void setWriteAsString(boolean writeAsString) {
        this.writeAsString = writeAsString;
    }

}
