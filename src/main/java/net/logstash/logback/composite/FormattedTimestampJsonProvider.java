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
package net.logstash.logback.composite;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import net.logstash.logback.fieldnames.LogstashCommonFieldNames;
import net.logstash.logback.util.TimeZoneUtils;

import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Writes the timestamp field as either:
 * <ul>
 *      <li>A string value formatted by a {@link DateTimeFormatter} pattern</li>
 *      <li>A string value representing the number of milliseconds since unix epoch (designated by specifying the pattern value as {@value #UNIX_TIMESTAMP_AS_STRING})</li>
 *      <li>A number value of the milliseconds since unix epoch (designated by specifying the pattern value as {@value #UNIX_TIMESTAMP_AS_NUMBER})</li>
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

    private static final String DEFAULT_PATTERN = "[ISO_OFFSET_DATE_TIME]";

    /**
     * The pattern in which to format the timestamp.
     *
     * <p>Possible values:</p>
     *
     * <ul>
     *     <li>{@value #UNIX_TIMESTAMP_AS_NUMBER} - timestamp written as a JSON number value of the milliseconds since unix epoch</li>
     *     <li>{@value #UNIX_TIMESTAMP_AS_STRING} - timestamp written as a JSON string value of the milliseconds since unix epoch</li>
     *     <li><code>[<em>constant</em>]</code> - timestamp written using the {@link DateTimeFormatter} constant specified by <code><em>constant</em></code> (e.g. {@code [ISO_OFFSET_DATE_TIME]})</li>
     *     <li>any other value - timestamp written by a {@link DateTimeFormatter} created from the pattern string specified
     * </ul>
     */
    private String pattern = DEFAULT_PATTERN;

    /**
     * The timezone for which to write the timestamp.
     * Only applicable if the pattern is not {@value #UNIX_TIMESTAMP_AS_NUMBER} or {@value #UNIX_TIMESTAMP_AS_STRING}
     */
    private TimeZone timeZone = TimeZone.getDefault();

    /**
     * Writes the timestamp to the JsonGenerator.
     */
    private TimestampWriter timestampWriter;

    /**
     * Writes the timestamp to the JsonGenerator
     */
    private interface TimestampWriter {
        void writeTo(JsonGenerator generator, String fieldName, long timestampInMillis) throws IOException;

        String getTimestampAsString(long timestampInMillis);
    }

    /**
     * Writes the timestamp to the JsonGenerator as a string formatted by the pattern.
     */
    private static class PatternTimestampWriter implements TimestampWriter {

        private final DateTimeFormatter formatter;

        PatternTimestampWriter(DateTimeFormatter formatter) {
            this.formatter = formatter;
        }


        @Override
        public void writeTo(JsonGenerator generator, String fieldName, long timestampInMillis) throws IOException {
            JsonWritingUtils.writeStringField(generator, fieldName, getTimestampAsString(timestampInMillis));
        }

        @Override
        public String getTimestampAsString(long timestampInMillis) {
            return formatter.format(Instant.ofEpochMilli(timestampInMillis));
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
        updateTimestampWriter();
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
        } else if (pattern.startsWith("[") && pattern.endsWith("]")) {
            String constant = pattern.substring("[".length(), pattern.length() - "]".length());
            try {
                Field field = DateTimeFormatter.class.getField(constant);
                if (Modifier.isStatic(field.getModifiers())
                        && Modifier.isFinal(field.getModifiers())
                        && field.getType().equals(DateTimeFormatter.class)) {
                    try {
                        DateTimeFormatter formatter = (DateTimeFormatter) field.get(null);
                        timestampWriter = new PatternTimestampWriter(formatter.withZone(timeZone.toZoneId()));
                    } catch (IllegalAccessException e) {
                        throw new IllegalArgumentException(String.format("Unable to get value of constant named %s in %s", constant, DateTimeFormatter.class), e);
                    }
                } else {
                    throw new IllegalArgumentException(String.format("Field named %s in %s is not a constant %s", constant, DateTimeFormatter.class, DateTimeFormatter.class));
                }
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException(String.format("No constant named %s found in %s", constant, DateTimeFormatter.class), e);
            }
        } else {
            timestampWriter = new PatternTimestampWriter(DateTimeFormatter.ofPattern(pattern).withZone(timeZone.toZoneId()));
        }
    }

    public String getPattern() {
        return pattern;
    }
    public void setPattern(String pattern) {
        this.pattern = pattern;
        updateTimestampWriter();
    }
    
    /**
     * Get the time zone used to write the timestamp.
     * 
     * @return the time zone used to write the timestamp
     */
    public String getTimeZone() {
        return timeZone.getID();
    }
    
    
    /**
     * Set the timezone for which to write the timestamp.
     * Only applicable if the pattern is not {@value #UNIX_TIMESTAMP_AS_NUMBER} or {@value #UNIX_TIMESTAMP_AS_STRING}.
     * 
     * <p>The TimeZone can be expressed into any format supported by {@link TimeZone#getTimeZone(String)}.
     * It can be a valid time zone ID. For instance, the time zone ID for the
     * U.S. Pacific Time zone is "America/Los_Angeles".
     * 
     * <p>If the time zone you want is not represented by one of the
     * supported IDs, then a custom time zone ID can be specified to
     * produce a TimeZone. The syntax of a custom time zone ID is:
     *
     * <blockquote><pre>
     * <i>CustomID:</i>
     *         <code>GMT</code> <i>Sign</i> <i>Hours</i> <code>:</code> <i>Minutes</i>
     *         <code>GMT</code> <i>Sign</i> <i>Hours</i> <i>Minutes</i>
     *         <code>GMT</code> <i>Sign</i> <i>Hours</i>
     * <i>Sign:</i> one of
     *         <code>+ -</code>
     * <i>Hours:</i>
     *         <i>Digit</i>
     *         <i>Digit</i> <i>Digit</i>
     * <i>Minutes:</i>
     *         <i>Digit</i> <i>Digit</i>
     * <i>Digit:</i> one of
     *         <code>0 1 2 3 4 5 6 7 8 9</code>
     * </pre></blockquote>
     *
     * <i>Hours</i> must be between 0 to 23 and <i>Minutes</i> must be
     * between 00 to 59.  For example, "GMT+10" and "GMT+0010" mean ten
     * hours and ten minutes ahead of GMT, respectively.
     * 
     * <p>Use a blank string or {@code null} to use the default TimeZone of the system.
     * 
     * @param timeZoneId the textual representation of the desired time zone
     * @throws IllegalArgumentException if the input string is not a valid TimeZone representation
     */
    public void setTimeZone(String timeZoneId) {
        if (timeZoneId == null || timeZoneId.trim().isEmpty()) {
            this.timeZone = TimeZone.getDefault();
        } else {
            this.timeZone = TimeZoneUtils.parse(timeZoneId);
        }
        updateTimestampWriter();
    }
}
