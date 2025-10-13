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
package net.logstash.logback.composite;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.TimeZone;
import java.util.function.Function;

import net.logstash.logback.fieldnames.LogstashCommonFieldNames;
import net.logstash.logback.util.TimeZoneUtils;

import ch.qos.logback.core.spi.DeferredProcessingAware;
import tools.jackson.core.JsonGenerator;

/**
 * Writes the timestamp field as either:
 * <ul>
 *      <li>A string value formatted by a {@link DateTimeFormatter} pattern</li>
 *      <li>A string value representing the number of milliseconds since unix epoch (designated by specifying the pattern value as {@value #UNIX_TIMESTAMP_AS_STRING})</li>
 *      <li>A number value of the milliseconds since unix epoch (designated by specifying the pattern value as {@value #UNIX_TIMESTAMP_AS_NUMBER})</li>
 * </ul>
 */
public abstract class AbstractFormattedTimestampJsonProvider<Event extends DeferredProcessingAware, FieldNames extends LogstashCommonFieldNames> extends AbstractFieldJsonProvider<Event> implements FieldNamesAware<FieldNames> {

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

    /**
     * The default {@link #pattern} value.
     */
    private static final String DEFAULT_PATTERN = "[ISO_OFFSET_DATE_TIME]";

    /**
     * Keyword used by {@link #setTimeZone(String)} to denote the system default time zone.
     */
    public static final String DEFAULT_TIMEZONE_KEYWORD = "[DEFAULT]";
    
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
    protected interface TimestampWriter {
        void writeTo(JsonGenerator generator, String fieldName, Instant timestamp);

        String getTimestampAsString(Instant timestamp);
    }


    /**
     * Writes the timestamp to the JsonGenerator as a number of milliseconds since unix epoch.
     */
    protected static class NumberTimestampWriter implements TimestampWriter {

        @Override
        public void writeTo(JsonGenerator generator, String fieldName, Instant timestamp) {
            JsonWritingUtils.writeNumberField(generator, fieldName, timestamp.toEpochMilli());
        }

        @Override
        public String getTimestampAsString(Instant timestamp) {
            return Long.toString(timestamp.toEpochMilli());
        }
    }

    /**
     * Writes the timestamp to the JsonGenerator as a string, converting the timestamp millis into a
     * String using the supplied Function.
     */
    protected static class StringFormatterWriter implements TimestampWriter {
        private final Function<Instant, String> provider;
        
        StringFormatterWriter(Function<Instant, String> provider) {
            this.provider = Objects.requireNonNull(provider);
        }
        
        @Override
        public void writeTo(JsonGenerator generator, String fieldName, Instant timestamp) {
            JsonWritingUtils.writeStringField(generator, fieldName, getTimestampAsString(timestamp));
        }

        @Override
        public String getTimestampAsString(Instant timestamp) {
            return provider.apply(timestamp);
        }
        
        static StringFormatterWriter with(DateTimeFormatter formatter) {
            return new StringFormatterWriter(formatter::format);
        }
        static StringFormatterWriter with(FastISOTimestampFormatter formatter) {
            return new StringFormatterWriter(formatter::format);
        }
        static StringFormatterWriter with(Function<Instant, String> formatter) {
            return new StringFormatterWriter(formatter);
        }
    }
    
    
    public AbstractFormattedTimestampJsonProvider() {
        setFieldName(FIELD_TIMESTAMP);
        updateTimestampWriter();
    }

    @Override
    public void setFieldNames(FieldNames fieldNames) {
        setFieldName(fieldNames.getTimestamp());
    }

    @Override
    public void writeTo(JsonGenerator generator, Event event) {
        timestampWriter.writeTo(generator, getFieldName(), getTimestampAsInstant(event));
    }

    protected String getFormattedTimestamp(Event event) {
        return timestampWriter.getTimestampAsString(getTimestampAsInstant(event));
    }

    protected abstract Instant getTimestampAsInstant(Event event);
    
    /**
     * Updates the {@link #timestampWriter} value based on the current pattern and timeZone.
     */
    private void updateTimestampWriter() {
        timestampWriter = createTimestampWriter();
    }
    
    private TimestampWriter createTimestampWriter() {
        if (UNIX_TIMESTAMP_AS_NUMBER.equals(pattern)) {
            return new NumberTimestampWriter();
        }
        
        if (UNIX_TIMESTAMP_AS_STRING.equals(pattern)) {
            return StringFormatterWriter.with(tstamp -> Long.toString(tstamp.toEpochMilli()));
        }
        
        if (pattern.startsWith("[") && pattern.endsWith("]")) {
            // Get the standard formatter by name...
            //
            String constant = pattern.substring(1, pattern.length() - 1);

            // Use our fast FastISOTimestampFormatter if suitable...
            //
            ZoneId zone = timeZone.toZoneId();
            switch (constant) {
                case "ISO_OFFSET_DATE_TIME" -> {
                    return StringFormatterWriter.with(FastISOTimestampFormatter.isoOffsetDateTime(zone));
                }
                case "ISO_ZONED_DATE_TIME" -> {
                    return StringFormatterWriter.with(FastISOTimestampFormatter.isoZonedDateTime(zone));
                }
                case "ISO_LOCAL_DATE_TIME" -> {
                    return StringFormatterWriter.with(FastISOTimestampFormatter.isoLocalDateTime(zone));
                }
                case "ISO_DATE_TIME" -> {
                    return StringFormatterWriter.with(FastISOTimestampFormatter.isoDateTime(zone));
                }
                case "ISO_INSTANT" -> {
                    return StringFormatterWriter.with(FastISOTimestampFormatter.isoInstant(zone));
                }
                default -> {
                    DateTimeFormatter formatter = getStandardDateTimeFormatter(constant).withZone(zone);
                    return StringFormatterWriter.with(formatter);
                }
            }
        }
        
        
        // Construct using a pattern
        //
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern).withZone(timeZone.toZoneId());
        return StringFormatterWriter.with(formatter);
    }
    
    
    private DateTimeFormatter getStandardDateTimeFormatter(String name) {
        try {
            Field field = DateTimeFormatter.class.getField(name);
            if (Modifier.isStatic(field.getModifiers())
                    && Modifier.isFinal(field.getModifiers())
                    && field.getType().equals(DateTimeFormatter.class)) {
                    return (DateTimeFormatter) field.get(null);
            }
            else {
                throw new IllegalArgumentException(String.format("Field named %s in %s is not a constant %s", name, DateTimeFormatter.class, DateTimeFormatter.class));
            }
        }
        catch (IllegalAccessException e) {
            throw new IllegalArgumentException(String.format("Unable to get value of constant named %s in %s", name, DateTimeFormatter.class), e);
        }
        catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(String.format("No constant named %s found in %s", name, DateTimeFormatter.class), e);
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
     * <p>The value of the {@code timeZone} can be any string accepted by java's {@link TimeZone#getTimeZone(String)} method.
     * For example "America/Los_Angeles" or "GMT+10".
     * 
     * <p>Use a blank string, {@code null} or the value {@value #DEFAULT_TIMEZONE_KEYWORD} to use the default TimeZone of the system.
     * 
     * @param timeZone the textual representation of the desired time zone
     * @throws IllegalArgumentException if the input string is not a valid TimeZone representation
     */
    public void setTimeZone(String timeZone) {
        if (timeZone == null || timeZone.trim().isEmpty() || DEFAULT_TIMEZONE_KEYWORD.equalsIgnoreCase(timeZone)) {
            this.timeZone = TimeZone.getDefault();
        } else {
            this.timeZone = TimeZoneUtils.parseTimeZone(timeZone);
        }
        updateTimestampWriter();
    }
}
