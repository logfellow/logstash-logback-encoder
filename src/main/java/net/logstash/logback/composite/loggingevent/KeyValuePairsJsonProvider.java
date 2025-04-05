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
package net.logstash.logback.composite.loggingevent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.fieldnames.LogstashFieldNames;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import org.slf4j.event.KeyValuePair;

/**
 * Includes key value pairs added from slf4j's fluent api in the output according to
 * {@link #includeKeyNames} and {@link #excludeKeyNames}.
 *
 * <p>There are three valid combinations of {@link #includeKeyNames}
 * and {@link #excludeKeyNames}:</p>
 *
 * <ol>
 * <li>When {@link #includeKeyNames} and {@link #excludeKeyNames}
 *     are both empty, then all entries will be included.</li>
 * <li>When {@link #includeKeyNames} is not empty and
 *     {@link #excludeKeyNames} is empty, then only those entries
 *     with key names in {@link #includeKeyNames} will be included.</li>
 * <li>When {@link #includeKeyNames} is empty and
 *     {@link #excludeKeyNames} is not empty, then all entries except those
 *     with key names in {@link #excludeKeyNames} will be included.</li>
 * </ol>
 *
 * <p>It is a configuration error for both {@link #includeKeyNames}
 * and {@link #excludeKeyNames} to be not empty.</p>
 *
 * <p>By default, for each key value pair, the key is output as the field name.
 * This can be changed by specifying an explicit field name to use for a ke
 * via {@link #addKeyFieldName(String)}</p>
 *
 * <p>If the fieldName is set, then the pairs will be written
 * to that field as a subobject.
 * Otherwise, the pairs are written inline.</p>
 */
public class KeyValuePairsJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> implements FieldNamesAware<LogstashFieldNames> {

    /**
     * See {@link KeyValuePairsJsonProvider}.
     */
    private List<String> includeKeyNames = new ArrayList<>();

    /**
     * See {@link KeyValuePairsJsonProvider}.
     */
    private List<String> excludeKeyNames = new ArrayList<>();

    private final Map<String, String> keyFieldNames = new HashMap<>();

    @Override
    public void start() {
        if (!this.includeKeyNames.isEmpty() && !this.excludeKeyNames.isEmpty()) {
            addError("Both includeKeyNames and excludeKeyNames are not empty.  Only one is allowed to be not empty.");
        }
        super.start();
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        List<KeyValuePair> keyValuePairs = event.getKeyValuePairs();
        if (keyValuePairs == null || keyValuePairs.isEmpty()) {
            return;
        }

        String fieldName = getFieldName();
        if (fieldName != null) {
            generator.writeObjectFieldStart(getFieldName());
        }

        for (KeyValuePair keyValuePair : keyValuePairs) {
            if (keyValuePair.key != null && keyValuePair.value != null
                    && (includeKeyNames.isEmpty() || includeKeyNames.contains(keyValuePair.key))
                    && (excludeKeyNames.isEmpty() || !excludeKeyNames.contains(keyValuePair.key))) {

                String key = keyFieldNames.get(keyValuePair.key);
                if (key == null) {
                    key = keyValuePair.key;
                }
                generator.writeFieldName(key);
                generator.writeObject(keyValuePair.value);
            }
        }

        if (fieldName != null) {
            generator.writeEndObject();
        }
    }

    @Override
    public void setFieldNames(LogstashFieldNames fieldNames) {
        setFieldName(fieldNames.getKeyValuePair());
    }

    public List<String> getIncludeKeyNames() {
        return Collections.unmodifiableList(includeKeyNames);
    }

    public void addIncludeKeyName(String includedKeyName) {
        this.includeKeyNames.add(includedKeyName);
    }

    public void setIncludeKeyNames(List<String> includeKeyNames) {
        this.includeKeyNames = new ArrayList<>(includeKeyNames);
    }

    public List<String> getExcludeKeyNames() {
        return Collections.unmodifiableList(excludeKeyNames);
    }

    public void addExcludeKeyName(String excludedKeyName) {
        this.excludeKeyNames.add(excludedKeyName);
    }

    public void setExcludeKeyNames(List<String> excludeKeyNames) {
        this.excludeKeyNames = new ArrayList<>(excludeKeyNames);
    }

    public Map<String, String> getKeyFieldNames() {
        return keyFieldNames;
    }

    /**
     * Adds the given keyFieldName entry in the form keyName=fieldName
     * to use an alternative field name for an KeyValuePair key.
     *
     * @param keyFieldName a string in the form kvpKeyName=fieldName that identifies what field name to use for a specific KeyValuePair key.
     */
    public void addKeyFieldName(String keyFieldName) {
        String[] split = keyFieldName.split("=");
        if (split.length != 2) {
            throw new IllegalArgumentException("keyFieldName (" + keyFieldName + ") must be in the form keyName=fieldName");
        }
        keyFieldNames.put(split[0], split[1]);
    }

}
