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
import java.util.List;
import java.util.Map;

import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.composite.loggingevent.mdc.MdcEntryWriter;
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
 * <p>The output of the MDC entry values can be manipulated by the provided
 * {@link #mdcEntryWriters}. By default, all MDC entry values are written as texts.
 */
public class MdcJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> implements FieldNamesAware<LogstashFieldNames> {

    /**
     * See {@link MdcJsonProvider}.
     */
    protected List<String> includeMdcKeyNames = new ArrayList<>();

    /**
     * See {@link MdcJsonProvider}.
     */
    protected List<String> excludeMdcKeyNames = new ArrayList<>();

    protected final Map<String, String> mdcKeyFieldNames = new HashMap<>();

    /**
     * See {@link MdcJsonProvider}.
     */
    protected final List<MdcEntryWriter> mdcEntryWriters = new ArrayList<>();

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
                    writeMdcEntry(generator, fieldName, entry.getKey(), entry.getValue());
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
        this.includeMdcKeyNames = new ArrayList<>(includeMdcKeyNames);
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

    public List<MdcEntryWriter> getMdcEntryWriters() {
        return Collections.unmodifiableList(mdcEntryWriters);
    }
    public void addMdcEntryWriter(MdcEntryWriter mdcEntryWriter) {
        this.mdcEntryWriters.add(mdcEntryWriter);
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

    /**
     * Writes the MDC entry with the given generator by iterating over the chain of {@link #mdcEntryWriters}
     * in the given order till the first {@link MdcEntryWriter} returns true.
     * <p>
     * If none of the {@link #mdcEntryWriters} returned true, the MDC field is written as String value by default.
     *
     * @param generator the generator to write the entry to.
     * @param fieldName the field name to use when writing the entry.
     * @param mdcKey    the key of the MDC map entry.
     * @param mdcValue  the value of the MDC map entry.
     */
    private void writeMdcEntry(JsonGenerator generator, String fieldName, String mdcKey, String mdcValue) throws IOException {
        for (MdcEntryWriter mdcEntryWriter : this.mdcEntryWriters) {
            if (mdcEntryWriter.writeMdcEntry(generator, fieldName, mdcKey, mdcValue)) {
                return;
            }
        }

        generator.writeFieldName(fieldName);
        generator.writeObject(mdcValue);
    }

}
