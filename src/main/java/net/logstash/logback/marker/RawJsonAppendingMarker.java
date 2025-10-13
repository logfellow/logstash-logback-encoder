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
package net.logstash.logback.marker;

import java.util.Objects;

import net.logstash.logback.argument.StructuredArgument;
import net.logstash.logback.composite.loggingevent.ArgumentsJsonProvider;
import net.logstash.logback.composite.loggingevent.LogstashMarkersJsonProvider;

import org.slf4j.Marker;
import tools.jackson.core.JsonGenerator;

/**
 * A {@link Marker} OR {@link StructuredArgument} that
 * writes a raw json value to the logstash json event
 * under a given field name.
 * <p>
 *
 * When writing to the JSON data (via {@link ArgumentsJsonProvider} or {@link LogstashMarkersJsonProvider}),
 * the raw string is written verbatim without any modifications,
 * but assuming it must constitute a single legal JSON value (number, string, boolean, null, Array or List)
 * <p>
 *
 * When writing to a String (when used as a {@link StructuredArgument} to the event's formatted message),
 * the raw string is written as the field value.
 * Note that using {@link RawJsonAppendingMarker} as a {@link StructuredArgument} is not very common.
 */
public class RawJsonAppendingMarker extends SingleFieldAppendingMarker {

    public static final String MARKER_NAME = SingleFieldAppendingMarker.MARKER_NAME_PREFIX + "RAW";

    /**
     * The raw json value to write as the field value.
     */
    private final String rawJson;

    public RawJsonAppendingMarker(String fieldName, String rawJson) {
        super(MARKER_NAME, fieldName);
        this.rawJson = Objects.requireNonNull(rawJson, "rawJson must not be null");
    }

    public RawJsonAppendingMarker(String fieldName, String rawJson, String messageFormatPattern) {
        super(MARKER_NAME, fieldName, messageFormatPattern);
        this.rawJson = Objects.requireNonNull(rawJson, "rawJson must not be null");
    }

    @Override
    protected void writeFieldValue(JsonGenerator generator) {
        generator.writeRawValue(rawJson);
    }

    @Override
    protected Object getFieldValue() {
        return rawJson;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof RawJsonAppendingMarker other)) {
            return false;
        }

        return Objects.equals(this.rawJson, other.rawJson);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + super.hashCode();
        result = prime * result + this.rawJson.hashCode();
        return result;
    }
}
