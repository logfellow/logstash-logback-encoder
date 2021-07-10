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
package net.logstash.logback.layout;

import java.util.List;

import net.logstash.logback.LogstashFormatter;
import net.logstash.logback.composite.CompositeJsonFormatter;
import net.logstash.logback.composite.JsonProvider;
import net.logstash.logback.fieldnames.LogstashFieldNames;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class LogstashLayout extends LoggingEventCompositeJsonLayout {
    
    @Override
    protected CompositeJsonFormatter<ILoggingEvent> createFormatter() {
        return new LogstashFormatter(this);
    }
    
    @Override
    protected LogstashFormatter getFormatter() {
        return (LogstashFormatter) super.getFormatter();
    }
    
    public void addProvider(JsonProvider<ILoggingEvent> provider) {
        getFormatter().addProvider(provider);
    }
    
    public void setCustomFields(String customFields) {
        getFormatter().setCustomFieldsFromString(customFields);
    }
    
    public String getCustomFields() {
        return getFormatter().getCustomFieldsAsString();
    }
    
    public boolean isIncludeCallerData() {
        return getFormatter().isIncludeCallerData();
    }
    
    public void setIncludeCallerData(boolean includeCallerData) {
        getFormatter().setIncludeCallerData(includeCallerData);
    }
    
    public LogstashFieldNames getFieldNames() {
        return getFormatter().getFieldNames();
    }
    
    public void setFieldNames(LogstashFieldNames fieldNames) {
        getFormatter().setFieldNames(fieldNames);
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

    public int getShortenedLoggerNameLength() {
        return getFormatter().getShortenedLoggerNameLength();
    }

    public void setShortenedLoggerNameLength(int length) {
        getFormatter().setShortenedLoggerNameLength(length);
    }
    
    public String getTimeZone() {
        return getFormatter().getTimeZone();
    }

    public void setTimeZone(String timeZoneId) {
        getFormatter().setTimeZone(timeZoneId);
    }

    public ThrowableHandlingConverter getThrowableConverter() {
        return getFormatter().getThrowableConverter();
    }

    public String getTimestampPattern() {
        return getFormatter().getTimestampPattern();
    }
    public void setTimestampPattern(String pattern) {
        getFormatter().setTimestampPattern(pattern);
    }

    public void setThrowableConverter(ThrowableHandlingConverter throwableConverter) {
        getFormatter().setThrowableConverter(throwableConverter);
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
     * <p>
     * The allowed values are:
     * <ul>
     *     <li>Null/Empty : Disable message splitting. This is also the default behavior.</li>
     *     <li>Any valid regex : Use the specified regex.</li>
     *     <li><tt>SYSTEM</tt> : Use the system-default line separator.</li>
     *     <li><tt>UNIX</tt> : Use <tt>\n</tt>.</li>
     *     <li><tt>WINDOWS</tt> : Use <tt>\r\n</tt>.</li>
     * </ul>
     * </p>
     * <p>
     * For example, if this parameter is set to the regex {@code #+}, then the logging statement:
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
     * </p>
     *
     * @param messageSplitRegex The regex used to split the message text
     */
    public void setMessageSplitRegex(String messageSplitRegex) {
        getFormatter().setMessageSplitRegex(messageSplitRegex);
    }
}
