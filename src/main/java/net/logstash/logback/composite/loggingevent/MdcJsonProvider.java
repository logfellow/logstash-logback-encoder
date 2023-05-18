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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.fieldnames.LogstashFieldNames;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import org.slf4j.MDC;

/**
 * Includes {@link MDC} properties in the JSON output according to
 * {@link #includeMdcKeyNames} and {@link #excludeMdcKeyNames}.
 *
 * <p>There are three valid combinations of {@link #includeMdcKeyNames}
 * and {@link #excludeMdcKeyNames}:</p>
 *
 * <ol>
 * <li>When {@link #includeMdcKeyNames} and {@link #excludeMdcKeyNames}
 *     are both empty, then all entries will be included.</li>
 * <li>When {@link #includeMdcKeyNames} is not empty and
 *     {@link #excludeMdcKeyNames} is empty, then only those entries
 *     with key names in {@link #includeMdcKeyNames} will be included.</li>
 * <li>When {@link #includeMdcKeyNames} is empty and
 *     {@link #excludeMdcKeyNames} is not empty, then all entries except those
 *     with key names in {@link #excludeMdcKeyNames} will be included.</li>
 * </ol>
 *
 * <p>It is a configuration error for both {@link #includeMdcKeyNames}
 * and {@link #excludeMdcKeyNames} to be not empty.</p>
 *
 * <p>By default, for each entry in the MDC, the MDC key is output as the field name.
 * This can be changed by specifying an explicit field name to use for an MDC key
 * via {@link #addMdcKeyFieldName(String)}</p>
 *
 * <p>If the fieldName is set, then the properties will be written
 * to that field as a subobject.
 * Otherwise, the properties are written inline.</p>
 *
 * <p>{@link #mdcConvertValueTypes} (empty per default) can be set to convert a MDC field value
 * into a number in the JSON output if the value matches the format of a Double or Long value:</p>
 *
 * <ul>
 * <li>Add {@link #MDC_CONVERT_VALUE_TYPE_DOUBLE} to attempt a conversion of Double values.</li>
 * <li>Add {@link #MDC_CONVERT_VALUE_TYPE_LONG} to attempt a conversion of Long values.</li>
 * </ul>
 */
public class MdcJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> implements FieldNamesAware<LogstashFieldNames> {

    protected static final String MDC_CONVERT_VALUE_TYPE_LONG = "LONG";
    protected static final String MDC_CONVERT_VALUE_TYPE_DOUBLE = "DOUBLE";

    /**
     * See {@link MdcJsonProvider}.
     */
    protected List<String> includeMdcKeyNames = new ArrayList<>();

    /**
     * See {@link MdcJsonProvider}.
     */
    protected List<String> excludeMdcKeyNames = new ArrayList<>();

    protected final Map<String, String> mdcKeyFieldNames = new HashMap<>();

    protected final Set<String> mdcConvertValueTypes = new HashSet<>();

    @Override
    public void start() {
        if (!this.includeMdcKeyNames.isEmpty() && !this.excludeMdcKeyNames.isEmpty()) {
            addError("Both includeMdcKeyNames and excludeMdcKeyNames are not empty.  Only one is allowed to be not empty.");
        }
        super.start();
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        Map<String, String> mdcProperties = event.getMDCPropertyMap();
        if (mdcProperties != null && !mdcProperties.isEmpty()) {

            boolean hasWrittenStart = false;

            for (Map.Entry<String, String> entry : mdcProperties.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null
                        && (includeMdcKeyNames.isEmpty() || includeMdcKeyNames.contains(entry.getKey()))
                        && (excludeMdcKeyNames.isEmpty() || !excludeMdcKeyNames.contains(entry.getKey()))) {

                    String fieldName = mdcKeyFieldNames.get(entry.getKey());
                    if (fieldName == null) {
                        fieldName = entry.getKey();
                    }
                    if (!hasWrittenStart && getFieldName() != null) {
                        generator.writeObjectFieldStart(getFieldName());
                        hasWrittenStart = true;
                    }
                    generator.writeFieldName(fieldName);
                    this.writeFieldValue(generator, fieldName, entry.getValue());
                }
            }
            if (hasWrittenStart) {
                generator.writeEndObject();
            }
        }
    }

    @Override
    public void setFieldNames(LogstashFieldNames fieldNames) {
        setFieldName(fieldNames.getMdc());
    }

    public List<String> getIncludeMdcKeyNames() {
        return Collections.unmodifiableList(includeMdcKeyNames);
    }
    public void addIncludeMdcKeyName(String includedMdcKeyName) {
        this.includeMdcKeyNames.add(includedMdcKeyName);
    }
    public void setIncludeMdcKeyNames(List<String> includeMdcKeyNames) {
        this.includeMdcKeyNames = new ArrayList<String>(includeMdcKeyNames);
    }

    public List<String> getExcludeMdcKeyNames() {
        return Collections.unmodifiableList(excludeMdcKeyNames);
    }
    public void addExcludeMdcKeyName(String excludedMdcKeyName) {
        this.excludeMdcKeyNames.add(excludedMdcKeyName);
    }
    public void setExcludeMdcKeyNames(List<String> excludeMdcKeyNames) {
        this.excludeMdcKeyNames = new ArrayList<>(excludeMdcKeyNames);
    }

    public Map<String, String> getMdcKeyFieldNames() {
        return mdcKeyFieldNames;
    }

    public Set<String> getMdcConvertValueTypes() {
        return mdcConvertValueTypes;
    }
    public void addMdcConvertValueType(String mdcConvertValueType) {
        this.mdcConvertValueTypes.add(mdcConvertValueType.toUpperCase());
    }

    /**
     * Adds the given mdcKeyFieldName entry in the form mdcKeyName=fieldName
     * to use an alternative field name for an MDC key.
     *
     * @param mdcKeyFieldName a string in the form mdcKeyName=fieldName that identifies what field name to use for a specific MDC key.
     */
    public void addMdcKeyFieldName(String mdcKeyFieldName) {
        String[] split = mdcKeyFieldName.split("=");
        if (split.length != 2) {
            throw new IllegalArgumentException("mdcKeyFieldName (" + mdcKeyFieldName + ") must be in the form mdcKeyName=fieldName");
        }
        mdcKeyFieldNames.put(split[0], split[1]);
    }

    protected void writeFieldValue(JsonGenerator generator, String fieldName, String value) throws IOException {
        if (this.mdcConvertValueTypes.isEmpty() || value == null || value.isEmpty()) {
            generator.writeObject(value);
        } else {
            if (!writeConvertedFieldValue(generator, fieldName, value)) {
                generator.writeObject(value);
            }
        }
    }

    /**
     * Writes a converted field value using the provided generator.
     *
     * @param fieldName MDC field name (which may be altered by {@link #addMdcKeyFieldName(String)}
     * @param value MDC field value
     * @return true if the field has been converted and written with the generator;
     *      false otherwise, in which case the String value is written as-is.
     */
    protected boolean writeConvertedFieldValue(JsonGenerator generator, String fieldName, String value) throws IOException {
        if (this.mdcConvertValueTypes.contains(MDC_CONVERT_VALUE_TYPE_LONG)) {
            try {
                generator.writeNumber(Long.parseLong(value));
                return true;
            } catch (NumberFormatException ignore) {
            }
        }

        if (this.mdcConvertValueTypes.contains(MDC_CONVERT_VALUE_TYPE_DOUBLE)) {
            try {
                generator.writeNumber(Double.parseDouble(value));
                return true;
            } catch (NumberFormatException ignore) {
            }
        }

        return false;
    }

}
