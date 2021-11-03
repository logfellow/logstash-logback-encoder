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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import net.logstash.logback.fieldnames.LogstashCommonFieldNames;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

/**
 * Utilities for writing JSON
 *
 * <p>Note: This class is for internal use only and subject to backward incompatible change
 * at any time.
 */
public class JsonWritingUtils {

    /**
     * Writes entries of the map as fields.
     * 
     * @param generator the {@link JsonGenerator} to produce JSON content
     * @param map map whose entries are written as JSON field/values
     * 
     * @throws IOException if an I/O error occurs
     * @throws JsonMappingException when problem to convert map values of type Object into JSON
     */
    public static void writeMapEntries(JsonGenerator generator, Map<?, ?> map) throws IOException, JsonMappingException {
        if (map != null) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    generator.writeFieldName(entry.getKey().toString());
                    generator.writeObject(entry.getValue());
                }
            }
        }
    }

    /**
     * Writes a map as String fields to the generator if and only if the {@code fieldName}
     * and values are not {@code null}.
     * 
     * @param generator the {@link JsonGenerator} to produce JSON content
     * @param fieldName name of the JSON property to write the map content under
     * @param map map whose entries are written as JSON field/values
     * 
     * @throws IOException if an I/O error occurs
     * @throws JsonMappingException when problem to convert map values of type Object into JSON

     */
    public static void writeMapStringFields(JsonGenerator generator, String fieldName, Map<String, String> map) throws IOException, JsonMappingException {
        writeMapStringFields(generator, fieldName, map, false);
    }

    /**
     * Writes a map as String fields to the generator if and only if the {@code fieldName}
     * and values are not {@code null}.
     * 
     * @param generator the {@link JsonGenerator} to produce JSON content
     * @param fieldName name of the JSON property to write the map content under
     * @param map map whose entries are written as JSON field/values
     * @param lowerCaseKeys when true, the map keys will be written in lower case.
     * 
     * @throws IOException if an I/O error occurs
     * @throws JsonMappingException when problem to convert map values of type Object into JSON
     */
    public static void writeMapStringFields(JsonGenerator generator, String fieldName, Map<String, String> map, boolean lowerCaseKeys) throws IOException, JsonMappingException {
        if (shouldWriteField(fieldName) && map != null && !map.isEmpty()) {
            generator.writeObjectFieldStart(fieldName);
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey() != null && lowerCaseKeys
                        ? entry.getKey().toLowerCase()
                        : entry.getKey();
                writeStringField(generator, key, entry.getValue());
            }
            generator.writeEndObject();
        }
    }

    /**
     * Writes an array of strings to the generator if and only if the {@code fieldName}
     * and values are not {@code null}.
     * 
     * @param generator the {@link JsonGenerator} to produce JSON content
     * @param fieldName the field name
     * @param fieldValues the field values
     * 
     * @throws IOException if an I/O error occurs
     */
    public static void writeStringArrayField(JsonGenerator generator, String fieldName, String[] fieldValues) throws IOException {
        if (shouldWriteField(fieldName) && fieldValues != null && fieldValues.length > 0) {
            generator.writeArrayFieldStart(fieldName);
            for (String fieldValue : fieldValues) {
                generator.writeString(fieldValue);
            }
            generator.writeEndArray();
        }
    }

    /**
     * Writes the field to the generator if and only if the {@code fieldName} and
     * {@code fieldValue} are not {@code null}.
     * 
     * @param generator the {@link JsonGenerator} to produce JSON content
     * @param fieldName the field name
     * @param fieldValue the field value
     * 
     * @throws IOException if an I/O error occurs
     */
    public static void writeStringField(JsonGenerator generator, String fieldName, String fieldValue) throws IOException {
        if (shouldWriteField(fieldName) && fieldValue != null) {
            generator.writeStringField(fieldName, fieldValue);
        }
    }

    /**
     * Writes the field to the generator if and only if the {@code fieldName} is not {@code null}.
     * 
     * @param generator the {@link JsonGenerator} to produce JSON content
     * @param fieldName the field name
     * @param fieldValue the field value
     * 
     * @throws IOException if an I/O error occurs
     */
    public static void writeNumberField(JsonGenerator generator, String fieldName, int fieldValue) throws IOException {
        if (shouldWriteField(fieldName)) {
            generator.writeNumberField(fieldName, fieldValue);
        }
    }

    /**
     * Writes the field to the generator if and only if the {@code fieldName} is not {@code null}.
     * 
     * @param generator the {@link JsonGenerator} to produce JSON content
     * @param fieldName the field name
     * @param fieldValue the field value
     * 
     * @throws IOException if an I/O error occurs
     */
    public static void writeNumberField(JsonGenerator generator, String fieldName, long fieldValue) throws IOException {
        if (shouldWriteField(fieldName)) {
            generator.writeNumberField(fieldName, fieldValue);
        }
    }

    /**
     * Indicates whether the given field name must be written or not.
     * A field should be written if its name is not null and not ignored.
     * 
     * @param fieldName the field name
     * @return {@code true} if the field should be written, {@code false} otherwise
     * 
     * @see LogstashCommonFieldNames#IGNORE_FIELD_INDICATOR
     */
    public static boolean shouldWriteField(String fieldName) {
        return fieldName != null && !fieldName.equals(LogstashCommonFieldNames.IGNORE_FIELD_INDICATOR);
    }

    /**
     * Helper method to try to call appropriate write method for given
     * untyped Object. At this point, no structural conversions should be done,
     * only simple basic types are to be coerced as necessary.
     *
     * @param generator the {@link JsonGenerator} to produce JSON content
     * @param value Value to write
     *
     * @throws IOException if there is either an underlying I/O problem or encoding
     *    issue at format layer
     */
    public static void writeSimpleObject(JsonGenerator generator, Object value) throws IOException {
        if (value == null) {
            generator.writeNull();
            return;
        }
        if (value instanceof String) {
            generator.writeString((String) value);
            return;
        }
        if (value instanceof Number) {
            Number n = (Number) value;
            if (n instanceof Integer) {
                generator.writeNumber(n.intValue());
                return;
            }
            if (n instanceof Long) {
                generator.writeNumber(n.longValue());
                return;
            }
            if (n instanceof Double) {
                generator.writeNumber(n.doubleValue());
                return;
            }
            if (n instanceof Float) {
                generator.writeNumber(n.floatValue());
                return;
            }
            if (n instanceof Short) {
                generator.writeNumber(n.shortValue());
                return;
            }
            if (n instanceof Byte) {
                generator.writeNumber(n.byteValue());
                return;
            }
            if (n instanceof BigInteger) {
                generator.writeNumber((BigInteger) n);
                return;
            }
            if (n instanceof BigDecimal) {
                generator.writeNumber((BigDecimal) n);
                return;
            }
            if (n instanceof AtomicInteger) {
                generator.writeNumber(((AtomicInteger) n).get());
                return;
            }
            if (n instanceof AtomicLong) {
                generator.writeNumber(((AtomicLong) n).get());
                return;
            }
        }
        if (value instanceof byte[]) {
            generator.writeBinary((byte[]) value);
            return;
        }
        if (value instanceof Boolean) {
            generator.writeBoolean((Boolean) value);
            return;
        }
        if (value instanceof AtomicBoolean) {
            generator.writeBoolean(((AtomicBoolean) value).get());
            return;
        }
        if (value instanceof JsonNode) {
            JsonNode node = (JsonNode) value;
            
            if (node instanceof NullNode) {
                generator.writeNull();
            } else {
                /*
                 * Ask the JsonNode to serialize itself using the supplied JsonGenerator
                 * but without SerializerProvider (last parameter set to 'null').
                 * 
                 * This works except for:
                 * - NullNode: the case is already handled above
                 * - POJONode: this type of node is definitely not a "simple type" and requires
                 *             the help of an ObjectCodec to be serialized. Attempts to serialize
                 *             a POJONode with this method will throw a NullPointerException.
                 * 
                 */
                node.serialize(generator, null);
            }
            return;
        }
        throw new IllegalArgumentException("Can only serialize simple wrapper types (type passed " + value.getClass().getName() + ")");
    }
}
