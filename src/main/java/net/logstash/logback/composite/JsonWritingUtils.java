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
import java.util.Map;

import net.logstash.logback.fieldnames.LogstashCommonFieldNames;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;

public class JsonWritingUtils {

    /**
     * Writes entries of the map as fields.
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
     * Writes a map as String fields to the generator if and only if the fieldName and values are not null.
     */
    public static void writeMapStringFields(JsonGenerator generator, String fieldName, Map<String, String> map) throws IOException, JsonMappingException {
        writeMapStringFields(generator, fieldName, map, false);
    }
    
    /**
     * Writes a map as String fields to the generator if and only if the fieldName and values are not null.
     * @param lowerCaseKeys when true, the map keys will be written in lowercase.
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
     * Writes the field to the generator if and only if the fieldName and fieldValue are not null.
     */
    public static void writeStringField(JsonGenerator generator, String fieldName, String fieldValue) throws IOException {
        if (shouldWriteField(fieldName) && fieldValue != null) {
            generator.writeStringField(fieldName, fieldValue);
        }
    }

    /**
     * Writes the field to the generator if and only if the fieldName is not null.
     */
    public static void writeNumberField(JsonGenerator generator, String fieldName, int fieldValue) throws IOException {
        if (shouldWriteField(fieldName)) {
            generator.writeNumberField(fieldName, fieldValue);
        }
    }

    /**
     * Writes the field to the generator if and only if the fieldName is not null.
     */
    public static void writeNumberField(JsonGenerator generator, String fieldName, long fieldValue) throws IOException {
        if (shouldWriteField(fieldName)) {
            generator.writeNumberField(fieldName, fieldValue);
        }
    }

    public static boolean shouldWriteField(String fieldName) {
        return fieldName != null && !fieldName.equals(LogstashCommonFieldNames.IGNORE_FIELD_INDICATOR);
    }

}
