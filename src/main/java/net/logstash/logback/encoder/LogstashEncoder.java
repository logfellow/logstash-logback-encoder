/*
 * Copyright 2013-2023 the original author or authors.
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
package net.logstash.logback.encoder;

import java.util.List;

import net.logstash.logback.LogstashFormatter;
import net.logstash.logback.composite.AbstractCompositeJsonFormatter;
import net.logstash.logback.composite.JsonProvider;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.composite.loggingevent.mdc.MdcEntryWriter;
import net.logstash.logback.fieldnames.LogstashFieldNames;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class LogstashEncoder extends LoggingEventCompositeJsonEncoder {
    
    @Override
    protected AbstractCompositeJsonFormatter<ILoggingEvent> createFormatter() {
        return new LogstashFormatter(this);
    }
    
    @Override
    protected LogstashFormatter getFormatter() {
        return (LogstashFormatter) super.getFormatter();
    }
    
    public void addProvider(JsonProvider<ILoggingEvent> provider) {
        getFormatter().addProvider(provider);
    }
    
    public boolean isIncludeCallerData() {
        return getFormatter().isIncludeCallerData();
    }
    
    public void setIncludeCallerData(boolean includeCallerData) {
        getFormatter().setIncludeCallerData(includeCallerData);
    }
    
    public void setCustomFields(String customFields) {
        getFormatter().setCustomFieldsFromString(customFields);
    }
    
    public String getCustomFields() {
        return getFormatter().getCustomFieldsAsString();
    }
    
    public LogstashFieldNames getFieldNames() {
        return getFormatter().getFieldNames();
    }
    
    public void setFieldNames(LogstashFieldNames fieldNames) {
        getFormatter().setFieldNames(fieldNames);
    }

    public int getShortenedLoggerNameLength() {
        return getFormatter().getShortenedLoggerNameLength();
    }
    
    public void setShortenedLoggerNameLength(int length) {
        getFormatter().setShortenedLoggerNameLength(length);
    }
    
    public boolean isIncludeMdc() {
        return getFormatter().isIncludeMdc();
    }
    
    public void setIncludeMdc(boolean includeMdc) {
        getFormatter().setIncludeMdc(includeMdc);
    }
    
    public List<String> getIncludeMdcKeyNames() {
        return getFormatter().getIncludeMdcKeyNames();
    }

    public void addIncludeMdcKeyName(String includedMdcKeyName) {
        getFormatter().addIncludeMdcKeyName(includedMdcKeyName);
    }

    public void setIncludeMdcKeyNames(List<String> includeMdcKeyNames) {
        getFormatter().setIncludeMdcKeyNames(includeMdcKeyNames);
    }

    public List<String> getExcludeMdcKeyNames() {
        return getFormatter().getExcludeMdcKeyNames();
    }

    public void addExcludeMdcKeyName(String excludedMdcKeyName) {
        getFormatter().addExcludeMdcKeyName(excludedMdcKeyName);
    }

    public void setExcludeMdcKeyNames(List<String> excludeMdcKeyNames) {
        getFormatter().setExcludeMdcKeyNames(excludeMdcKeyNames);
    }

    public void addMdcKeyFieldName(String mdcKeyFieldName) {
        getFormatter().addMdcKeyFieldName(mdcKeyFieldName);
    }

    public List<MdcEntryWriter> getMdcEntryWriters() {
        return getFormatter().getMdcEntryWriters();
    }

    public void addMdcEntryWriter(MdcEntryWriter mdcEntryWriter) {
        getFormatter().addMdcEntryWriter(mdcEntryWriter);
    }

    public boolean isIncludeKeyValuePairs() {
        return getFormatter().isIncludeKeyValuePairs();
    }

    public void setIncludeKeyValuePairs(boolean includeKeyValuePairs) {
        getFormatter().setIncludeKeyValuePairs(includeKeyValuePairs);
    }

    public void addIncludeKeyValueKeyName(String includedKeyValueKeyName) {
        getFormatter().addIncludeKeyValueKeyName(includedKeyValueKeyName);
    }

    public List<String> getIncludeKeyValueKeyNames() {
        return getFormatter().getIncludeKeyValueKeyNames();
    }

    public void setIncludeKeyValueKeyNames(List<String> includeKeyValueKeyNames) {
        getFormatter().setIncludeKeyValueKeyNames(includeKeyValueKeyNames);
    }

    public void addExcludeKeyValueKeyName(String excludedKeyValueKeyName) {
        getFormatter().addExcludeKeyValueKeyName(excludedKeyValueKeyName);
    }

    public List<String> getExcludeKeyValueKeyNames() {
        return getFormatter().getExcludeKeyValueKeyNames();
    }

    public void setExcludeKeyValueKeyNames(List<String> excludedKeyValueKeyNames) {
        getFormatter().setExcludeKeyValueKeyNames(excludedKeyValueKeyNames);
    }

    public void addKeyValueKeyFieldName(String keyValueKeyFieldName) {
        getFormatter().addKeyValueKeyFieldName(keyValueKeyFieldName);
    }

    public boolean isIncludeTags() {
        return getFormatter().isIncludeTags();
    }

    public void setIncludeTags(boolean includeTags) {
        getFormatter().setIncludeTags(includeTags);
    }

    public boolean isIncludeContext() {
        return getFormatter().isIncludeContext();
    }
    
    public void setIncludeContext(boolean includeContext) {
        getFormatter().setIncludeContext(includeContext);
    }
    
    public boolean isIncludeStructuredArguments() {
        return getFormatter().isIncludeStructuredArguments();
    }

    public void setIncludeStructuredArguments(boolean includeStructuredArguments) {
        getFormatter().setIncludeStructuredArguments(includeStructuredArguments);
    }
    
    public boolean isIncludeNonStructuredArguments() {
        return getFormatter().isIncludeNonStructuredArguments();
    }

    public void setIncludeNonStructuredArguments(boolean includeNonStructuredArguments) {
        getFormatter().setIncludeNonStructuredArguments(includeNonStructuredArguments);
    }
    
    public String getNonStructuredArgumentsFieldPrefix() {
        return getFormatter().getNonStructuredArgumentsFieldPrefix();
    }

    public void setNonStructuredArgumentsFieldPrefix(String nonStructuredArgumentsFieldPrefix) {
        getFormatter().setNonStructuredArgumentsFieldPrefix(nonStructuredArgumentsFieldPrefix);
    }
    
    public ThrowableHandlingConverter getThrowableConverter() {
        return getFormatter().getThrowableConverter();
    }

    public void setThrowableConverter(ThrowableHandlingConverter throwableConverter) {
        getFormatter().setThrowableConverter(throwableConverter);
    }

    public String getTimeZone() {
        return getFormatter().getTimeZone();
    }

    public void setTimeZone(String timeZoneId) {
        getFormatter().setTimeZone(timeZoneId);
    }
    
    public String getTimestampPattern() {
        return getFormatter().getTimestampPattern();
    }
    public void setTimestampPattern(String pattern) {
        getFormatter().setTimestampPattern(pattern);
    }

    public String getVersion() {
        return getFormatter().getVersion();
    }
    public void setVersion(String version) {
        getFormatter().setVersion(version);
    }

    public boolean isWriteVersionAsInteger() {
        return getFormatter().isWriteVersionAsInteger();
    }
    public void setWriteVersionAsInteger(boolean writeVersionAsInteger) {
        getFormatter().setWriteVersionAsInteger(writeVersionAsInteger);
    }
    
    /**
     * Write the message as a JSON array by splitting the message text using the specified regex.
     *
     * @return The regex used to split the message text
     */
    public String getMessageSplitRegex() {
        return getFormatter().getMessageSplitRegex();
    }

    /**
     * Write the message as a JSON array by splitting the message text using the specified regex.
     *
     * <p>The allowed values are:
     * <ul>
     *     <li>Null/Empty : Disable message splitting. This is also the default behavior.</li>
     *     <li>Any valid regex : Use the specified regex.</li>
     *     <li>{@code SYSTEM} : Use the system-default line separator.</li>
     *     <li>{@code UNIX} : Use {@code \n}.</li>
     *     <li>{@code WINDOWS} : Use {@code \r\n}.</li>
     * </ul>
     * 
     * <p>For example, if this parameter is set to the regex {@code #+}, then the logging statement:
     * <pre>
     * log.info("First line##Second line###Third line")
     * </pre>
     * will produce:
     * <pre>
     * {
     *     ...
     *     "message": [
     *         "First line",
     *         "Second line",
     *         "Third line"
     *     ],
     *     ...
     * }
     * </pre>
     *
     * @param messageSplitRegex The regex used to split the message text
     */
    public void setMessageSplitRegex(String messageSplitRegex) {
        getFormatter().setMessageSplitRegex(messageSplitRegex);
    }
    
    
    @Override
    public void setProviders(JsonProviders<ILoggingEvent> jsonProviders) {
        throw new IllegalArgumentException("Using the <providers> configuration property is not allowed. Use <provider> instead to register additional " + JsonProvider.class.getSimpleName() + ".");
    }
}
