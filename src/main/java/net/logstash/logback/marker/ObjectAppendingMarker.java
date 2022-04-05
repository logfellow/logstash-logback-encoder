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
package net.logstash.logback.marker;

import java.io.IOException;
import java.util.Objects;

import net.logstash.logback.argument.StructuredArgument;
import net.logstash.logback.argument.StructuredArguments;
import net.logstash.logback.composite.loggingevent.ArgumentsJsonProvider;
import net.logstash.logback.composite.loggingevent.LogstashMarkersJsonProvider;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Marker;

/**
 * A {@link Marker} OR {@link StructuredArgument} that
 * writes an object under a given fieldName in the log event output.
 * <p>
 *
 * When writing to the JSON data (via {@link ArgumentsJsonProvider} or {@link LogstashMarkersJsonProvider}),
 * the object will be converted into an appropriate JSON type
 * (number, string, object, array) and written to the JSON event under a given fieldName.
 * <p>
 *
 * When writing to a String (when used as a {@link StructuredArgument} to the event's formatted message),
 * as specified in {@link SingleFieldAppendingMarker}, the {@link SingleFieldAppendingMarker#messageFormatPattern}
 * is used to construct the string to include in the event's formatted message.
 * {@link StructuredArguments#toString(Object)} will be used to convert the value to a string,
 * prior to being substituted into the messageFormatPattern.
 * <p>
 *
 * An {@link ObjectMapper} is used to convert/write the value when writing to JSON,
 * so as long as the {@link ObjectMapper} can convert the object, you're good.
 * For example, to append a string field, use a String object as the object.
 * To append a numeric field, use a Number object as the object.
 * To append an array field, use an array as the object.
 * To append an object field, use some other Object as the object.
 * <p>
 *
 * Example:
 *
 * <pre>
 * logger.info("My Message {}", StructuredArguments.keyValue("key", "value"));
 * </pre>
 *
 * results in the following log event output:
 *
 * <pre>
 * {
 *     "message" : "My Message key=value",
 *     "key"     : "value"
 * }
 * </pre>
 */
@SuppressWarnings("serial")
public class ObjectAppendingMarker extends SingleFieldAppendingMarker {

    public static final String MARKER_NAME = SingleFieldAppendingMarker.MARKER_NAME_PREFIX + "OBJECT";

    /**
     * The object to write as the field's value.
     * Can be a {@link String}, {@link Number}, array, or some other object that can be processed by an {@link ObjectMapper}
     */
    private final Object fieldValue;

    public ObjectAppendingMarker(String fieldName, Object fieldValue) {
        super(MARKER_NAME, fieldName);
        this.fieldValue = fieldValue;
    }

    public ObjectAppendingMarker(String fieldName, Object fieldValue, String messageFormatPattern) {
        super(MARKER_NAME, fieldName, messageFormatPattern);
        this.fieldValue = fieldValue;
    }

    @Override
    protected void writeFieldValue(JsonGenerator generator) throws IOException {
        generator.writeObject(fieldValue);
    }

    @Override
    protected Object getFieldValue() {
        return fieldValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof ObjectAppendingMarker)) {
            return false;
        }

        ObjectAppendingMarker other = (ObjectAppendingMarker) obj;
        return Objects.equals(this.fieldValue, other.fieldValue);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + super.hashCode();
        result = prime * result + (this.fieldValue == null ? 0 : this.fieldValue.hashCode());
        return result;
    }
}
