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
    
    /**
     * @deprecated use {@link #isIncludeCallerData()} (to use the same name that logback uses)
     */
    @Deprecated
    public boolean isIncludeCallerInfo() {
        return getFormatter().isIncludeCallerInfo();
    }
    
    /**
     * @deprecated use {@link #setIncludeCallerData(boolean)} (to use the same name that logback uses)
     */
    @Deprecated
    public void setIncludeCallerInfo(boolean includeCallerInfo) {
        getFormatter().setIncludeCallerInfo(includeCallerInfo);
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
    
    public boolean isIncludeContext() {
        return getFormatter().isIncludeContext();
    }
    
    public void setIncludeContext(boolean includeContext) {
        getFormatter().setIncludeContext(includeContext);
    }

    public int getShortenedLoggerNameLength() {
        return getFormatter().getShortenedLoggerNameLength();
    }

    public void setShortenedLoggerNameLength(int length) {
        getFormatter().setShortenedLoggerNameLength(length);
    }
    
    @Deprecated
    public void setEnableContextMap(boolean enableContextMap) {
        getFormatter().setEnableContextMap(enableContextMap);
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
    
    public int getVersion() {
        return getFormatter().getVersion();
    }
    public void setVersion(int version) {
        getFormatter().setVersion(version);
    }
    
    public boolean isWriteVersionAsString() {
        return getFormatter().isWriteVersionAsString();
    }
    public void setWriteVersionAsString(boolean writeVersionAsString) {
        getFormatter().setWriteVersionAsString(writeVersionAsString);
    }

}
