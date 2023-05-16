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
package net.logstash.logback.composite.loggingevent;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.fieldnames.LogstashFieldNames;
import org.slf4j.event.KeyValuePair;
import net.logstash.logback.argument.StructuredArguments;

import java.io.IOException;
import java.util.*;

/**
 * Includes {@link KeyValuePair} properties in the JSON output according to
 * {@link #includeKvpKeyNames} and {@link #excludeKvpKeyNames}.
 *
 * <p>There are three valid combinations of {@link #includeKvpKeyNames}
 * and {@link #excludeKvpKeyNames}:</p>
 *
 * <ol>
 * <li>When {@link #includeKvpKeyNames} and {@link #excludeKvpKeyNames}
 *     are both empty, then all entries will be included.</li>
 * <li>When {@link #includeKvpKeyNames} is not empty and
 *     {@link #excludeKvpKeyNames} is empty, then only those entries
 *     with key names in {@link #includeKvpKeyNames} will be included.</li>
 * <li>When {@link #includeKvpKeyNames} is empty and
 *     {@link #excludeKvpKeyNames} is not empty, then all entries except those
 *     with key names in {@link #excludeKvpKeyNames} will be included.</li>
 * </ol>
 *
 * <p>It is a configuration error for both {@link #includeKvpKeyNames}
 * and {@link #excludeKvpKeyNames} to be not empty.</p>
 *
 * <p>By default, for each entry in the KeyValuePair, the KeyValuePair key is output as the field name.
 * This can be changed by specifying an explicit field name to use for an KeyValuePair key
 * via {@link #addKvpKeyFieldName(String)}</p>
 *
 * <p>If the fieldName is set, then the properties will be written
 * to that field as a subobject.
 * Otherwise, the properties are written inline.</p>
 */
public class KeyValuePairJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> implements FieldNamesAware<LogstashFieldNames> {

    /**
     * See {@link KeyValuePairJsonProvider}.
     */
    private List<String> includeKvpKeyNames = new ArrayList<>();

    /**
     * See {@link KeyValuePairJsonProvider}.
     */
    private List<String> excludeKvpKeyNames = new ArrayList<>();

    private final Map<String, String> kvpKeyFieldNames = new HashMap<>();

    @Override
    public void start() {
        if (!this.includeKvpKeyNames.isEmpty() && !this.excludeKvpKeyNames.isEmpty()) {
            addError("Both includeKvpKeyNames and excludeKvpKeyNames are not empty.  Only one is allowed to be not empty.");
        }
        super.start();
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        List<KeyValuePair> kvp = event.getKeyValuePairs();
        if (kvp == null || kvp.isEmpty())
            return;

        boolean hasWrittenStart = false;
        for (KeyValuePair kv : kvp) {
            if (kv.key != null && kv.value != null
                    && (includeKvpKeyNames.isEmpty() || includeKvpKeyNames.contains(kv.key))
                    && (excludeKvpKeyNames.isEmpty() || !excludeKvpKeyNames.contains(kv.key))) {

                String fieldName = kvpKeyFieldNames.get(kv.key);
                if (fieldName == null) {
                    fieldName = kv.key;
                }
                if (!hasWrittenStart && getFieldName() != null) {
                    generator.writeObjectFieldStart(getFieldName());
                    hasWrittenStart = true;
                }
                StructuredArguments.keyValue(fieldName, kv.value).writeTo(generator);
            }
        }
        if (hasWrittenStart) {
            generator.writeEndObject();
        }
    }

    @Override
    public void setFieldNames(LogstashFieldNames fieldNames) {
        setFieldName(fieldNames.getKeyValuePair());
    }

    public List<String> getIncludeKvpKeyNames() {
        return Collections.unmodifiableList(includeKvpKeyNames);
    }
    public void addIncludeKvpKeyName(String includedKvpKeyName) {
        this.includeKvpKeyNames.add(includedKvpKeyName);
    }
    public void setIncludeKvpKeyNames(List<String> includeKvpKeyNames) {
        this.includeKvpKeyNames = new ArrayList<String>(includeKvpKeyNames);
    }

    public List<String> getExcludeKvpKeyNames() {
        return Collections.unmodifiableList(excludeKvpKeyNames);
    }
    public void addExcludeKvpKeyName(String excludedKvpKeyName) {
        this.excludeKvpKeyNames.add(excludedKvpKeyName);
    }
    public void setExcludeKvpKeyNames(List<String> excludeKvpKeyNames) {
        this.excludeKvpKeyNames = new ArrayList<>(excludeKvpKeyNames);
    }

    public Map<String, String> getKvpKeyFieldNames() {
        return kvpKeyFieldNames;
    }

    /**
     * Adds the given kvpKeyFieldName entry in the form kvpKeyName=fieldName
     * to use an alternative field name for an KeyValuePair key.
     *
     * @param kvpKeyFieldName a string in the form kvpKeyName=fieldName that identifies what field name to use for a specific KeyValuePair key.
     */
    public void addKvpKeyFieldName(String kvpKeyFieldName) {
        String[] split = kvpKeyFieldName.split("=");
        if (split.length != 2) {
            throw new IllegalArgumentException("kvpKeyFieldName (" + kvpKeyFieldName + ") must be in the form kvpKeyName=fieldName");
        }
        kvpKeyFieldNames.put(split[0], split[1]);
    }

}
