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
import java.util.TimeZone;

import net.logstash.logback.fieldnames.LogstashCommonFieldNames;

import org.apache.commons.lang3.time.FastDateFormat;

import ch.qos.logback.core.spi.DeferredProcessingAware;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Writes the timestamp field as either:
 * <ul>
 *      <li>A string value formatted by a {@link FastDateFormat} pattern</li>
 *      <li>A string value representing the number of milliseconds since unix epoch  (designated by specifying the pattern value as {@value #UNIX_TIMESTAMP_AS_STRING})</li>
 *      <li>A number value of the milliseconds since unix epoch  (designated by specifying the pattern value as {@value #UNIX_TIMESTAMP_AS_NUMBER})</li>
 * </ul>
 */
public abstract class FormattedTimestampJsonProvider<Event extends DeferredProcessingAware, FieldNames extends LogstashCommonFieldNames> extends AbstractFieldJsonProvider<Event> implements FieldNamesAware<FieldNames> {
    
    public static final String FIELD_TIMESTAMP = "@timestamp";
    
    /**
     * Setting the {@link #pattern} as this value will make it so that the timestamp
     * is written as a number value of the milliseconds since unix epoch.
     */
    public static final String UNIX_TIMESTAMP_AS_NUMBER = "[UNIX_TIMESTAMP_AS_NUMBER]";
    
    /**
     * Setting the {@link #pattern} as this value will make it so that the timestamp
     * is written as a string value representing the number of milliseconds since unix epoch
     */
    public static final String UNIX_TIMESTAMP_AS_STRING = "[UNIX_TIMESTAMP_AS_STRING]";
    
    private static final String DEFAULT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";
    private static final TimeZone DEFAULT_TIMEZONE = null;
    
    /**
     * The pattern in which to format the timestamp.
     * Setting this value to {@value #UNIX_TIMESTAMP_AS_NUMBER} will cause the timestamp to be written as a number value of the milliseconds since unix epoch. 
     * Setting this value to {@value #UNIX_TIMESTAMP_AS_STRING} will cause the timestamp to be written as a string value representing the number value of the milliseconds since unix epoch.
     * Any other value will be used as a pattern for formatting the timestamp by a {@link FastDateFormat} 
     */
    private String pattern = DEFAULT_PATTERN;
    
    /**
     * The timezone for which to write the timestamp.
     * Only applicable if the pattern is not {@value #UNIX_TIMESTAMP_AS_NUMBER} or {@value #UNIX_TIMESTAMP_AS_STRING} 
     */
    private TimeZone timeZone = DEFAULT_TIMEZONE;
    
    /**
     * Writes the timestamp to the JsonGenerator.
     */
    private TimestampWriter timestampWriter = new PatternTimestampWriter(FastDateFormat.getInstance(pattern, timeZone));
    
    /**
     * Writes the timestamp to the JsonGenerator
     */
    private static interface TimestampWriter {
        void writeTo(JsonGenerator generator, String fieldName, long timestampInMillis) throws IOException;
        
        String getTimestampAsString(long timestampInMillis);
    }
    
    /**
     * Writes the timestamp to the JsonGenerator as a string formatted by the pattern.
     */
    private static class PatternTimestampWriter implements TimestampWriter {

        private final FastDateFormat formatter;
        
        public PatternTimestampWriter(FastDateFormat formatter) {
            this.formatter = formatter;
        }

        
        @Override
        public void writeTo(JsonGenerator generator, String fieldName, long timestampInMillis) throws IOException {
            JsonWritingUtils.writeStringField(generator, fieldName, getTimestampAsString(timestampInMillis));
        }

        @Override
        public String getTimestampAsString(long timestampInMillis) {
            return formatter.format(timestampInMillis);
        }
    }
    
    /**
     * Writes the timestamp to the JsonGenerator as a number of milliseconds since unix epoch.
     */
    private static class NumberTimestampWriter implements TimestampWriter {

        @Override
        public void writeTo(JsonGenerator generator, String fieldName, long timestampInMillis) throws IOException {
            JsonWritingUtils.writeNumberField(generator, fieldName, timestampInMillis);
        }
        
        @Override
        public String getTimestampAsString(long timestampInMillis) {
            return Long.toString(timestampInMillis);
        }
    }
    
    /**
     * Writes the timestamp to the JsonGenerator as a string representation of the of milliseconds since unix epoch.
     */
    private static class StringTimestampWriter implements TimestampWriter {

        @Override
        public void writeTo(JsonGenerator generator, String fieldName, long timestampInMillis) throws IOException {
            JsonWritingUtils.writeStringField(generator, fieldName, getTimestampAsString(timestampInMillis));
        }
        
        @Override
        public String getTimestampAsString(long timestampInMillis) {
            return Long.toString(timestampInMillis);
        }
        
    }
    
    public FormattedTimestampJsonProvider() {
        setFieldName(FIELD_TIMESTAMP);
    }
    
    @Override
    public void setFieldNames(FieldNames fieldNames) {
        setFieldName(fieldNames.getTimestamp());
    }
    
    @Override
    public void writeTo(JsonGenerator generator, Event event) throws IOException {
        timestampWriter.writeTo(generator, getFieldName(), getTimestampAsMillis(event));
    }

    protected String getFormattedTimestamp(Event event) {
        return timestampWriter.getTimestampAsString(getTimestampAsMillis(event));
    }

    protected abstract long getTimestampAsMillis(Event event);
    
    /**
     * Updates the {@link #timestampWriter} value based on the current pattern and timeZone.
     */
    private void updateTimestampWriter() {
        if (UNIX_TIMESTAMP_AS_NUMBER.equals(pattern)) {
            timestampWriter = new NumberTimestampWriter();
        } else if (UNIX_TIMESTAMP_AS_STRING.equals(pattern)) {
            timestampWriter = new StringTimestampWriter();
        } else {
            timestampWriter = new PatternTimestampWriter(FastDateFormat.getInstance(pattern, timeZone));
        }
    }
    
    public String getPattern() {
        return pattern;
    }
    public void setPattern(String pattern) {
        this.pattern = pattern;
        updateTimestampWriter();
    }
    public String getTimeZone() {
        return timeZone.getID();
    }
    public void setTimeZone(String timeZoneId) {
        this.timeZone = TimeZone.getTimeZone(timeZoneId);
        updateTimestampWriter();
    }
}
