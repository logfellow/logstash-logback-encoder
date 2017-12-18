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
package net.logstash.logback;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.logstash.logback.composite.ContextJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.composite.GlobalCustomFieldsJsonProvider;
import net.logstash.logback.composite.JsonProvider;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.composite.LogstashVersionJsonProvider;
import net.logstash.logback.composite.loggingevent.CallerDataJsonProvider;
import net.logstash.logback.composite.loggingevent.ContextMapJsonProvider;
import net.logstash.logback.composite.loggingevent.JsonMessageJsonProvider;
import net.logstash.logback.composite.loggingevent.LogLevelJsonProvider;
import net.logstash.logback.composite.loggingevent.LogLevelValueJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggerNameJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggingEventCompositeJsonFormatter;
import net.logstash.logback.composite.loggingevent.LoggingEventFormattedTimestampJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders;
import net.logstash.logback.composite.loggingevent.LogstashMarkersJsonProvider;
import net.logstash.logback.composite.loggingevent.MdcJsonProvider;
import net.logstash.logback.composite.loggingevent.MessageJsonProvider;
import net.logstash.logback.composite.loggingevent.StackTraceJsonProvider;
import net.logstash.logback.composite.loggingevent.TagsJsonProvider;
import net.logstash.logback.composite.loggingevent.ThreadNameJsonProvider;
import net.logstash.logback.fieldnames.LogstashFieldNames;
import net.logstash.logback.marker.Markers;

import org.slf4j.MDC;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.ContextAware;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A {@link LoggingEventCompositeJsonFormatter} that contains a common
 * pre-defined set of {@link JsonProvider}s.
 * 
 * The included providers are configured via properties on this
 * formatter, rather than configuring the providers directly.
 * This leads to a somewhat simpler configuration definitions. 
 * 
 * You cannot remove any of the pre-defined providers, but
 * you can add additional providers via {@link #addProvider(JsonProvider)}.
 * 
 * If you would like full control over the providers, you
 * should instead use {@link LoggingEventCompositeJsonFormatter} directly.
 */
public class LogstashFormatter extends LoggingEventCompositeJsonFormatter {
    
    /**
     * The field names to use when writing the standard event fields
     */
    protected LogstashFieldNames fieldNames;
    
    private final LoggingEventFormattedTimestampJsonProvider timestampProvider = new LoggingEventFormattedTimestampJsonProvider();
    private final LogstashVersionJsonProvider<ILoggingEvent> versionProvider = new LogstashVersionJsonProvider<ILoggingEvent>();
    private final MessageJsonProvider messageProvider = new MessageJsonProvider();
    private final LoggerNameJsonProvider loggerNameProvider = new LoggerNameJsonProvider();
    private final ThreadNameJsonProvider threadNameProvider = new ThreadNameJsonProvider();
    private final LogLevelJsonProvider logLevelProvider = new LogLevelJsonProvider();
    private final LogLevelValueJsonProvider logLevelValueProvider = new LogLevelValueJsonProvider();
    
    /**
     * When not null, the caller information is included in the logged data.
     * Note: calculating the caller data is an expensive operation.
     */
    private CallerDataJsonProvider callerDataProvider;
    private final StackTraceJsonProvider stackTraceProvider = new StackTraceJsonProvider();
    private ContextJsonProvider<ILoggingEvent> contextProvider = new ContextJsonProvider<ILoggingEvent>();
    /**
     * @deprecated When logging, prefer using a {@link Markers#appendArray(String, Object...)} marker
     *             with fieldName = "json_message" and objects = an array of arguments instead.
     */
    @Deprecated
    private final JsonMessageJsonProvider jsonMessageProvider = new JsonMessageJsonProvider();
    /**
     * When not null, {@link MDC} properties will be included according
     * to the logic in {@link MdcJsonProvider}.
     */
    private MdcJsonProvider mdcProvider = new MdcJsonProvider();
    /**
     * When not null, if the last argument to the log line is a map, then it will be embedded in the logstash json.
     * 
     * @deprecated When logging, prefer using a {@link Markers#appendEntries(Map)} marker instead.
     */
    @Deprecated
    private ContextMapJsonProvider contextMapProvider;
    private GlobalCustomFieldsJsonProvider<ILoggingEvent> globalCustomFieldsProvider;
    private final TagsJsonProvider tagsProvider = new TagsJsonProvider();
    private final LogstashMarkersJsonProvider logstashMarkersProvider = new LogstashMarkersJsonProvider();
    
    public LogstashFormatter(ContextAware declaredOrigin) {
        this(declaredOrigin, false);
    }
    
    public LogstashFormatter(ContextAware declaredOrigin,boolean includeCallerInfo) {
        this(declaredOrigin, includeCallerInfo, null);
    }
    
    public LogstashFormatter(ContextAware declaredOrigin,boolean includeCallerInfo, JsonNode customFields) {
        super(declaredOrigin);
        
        this.fieldNames = new LogstashFieldNames();

        setIncludeCallerInfo(includeCallerInfo);
        setCustomFields(customFields);
        
        getProviders().addTimestamp(this.timestampProvider);
        getProviders().addVersion(this.versionProvider);
        getProviders().addMessage(this.messageProvider);
        getProviders().addLoggerName(this.loggerNameProvider);
        getProviders().addThreadName(this.threadNameProvider);
        getProviders().addLogLevel(this.logLevelProvider);
        getProviders().addLogLevelValue(this.logLevelValueProvider);
        getProviders().addCallerData(this.callerDataProvider);
        getProviders().addStackTrace(this.stackTraceProvider);
        getProviders().addContext(this.contextProvider);
        getProviders().addJsonMessage(this.jsonMessageProvider);
        getProviders().addMdc(this.mdcProvider);
        getProviders().addContextMap(this.contextMapProvider);
        getProviders().addGlobalCustomFields(this.globalCustomFieldsProvider);
        getProviders().addTags(this.tagsProvider);
        getProviders().addLogstashMarkers(this.logstashMarkersProvider);
    }
    
    @Override
    public void start() {
        configureProviderFieldNames();
        super.start();
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void configureProviderFieldNames() {
        for (JsonProvider<ILoggingEvent> provider : getProviders().getProviders()) {
            if (provider instanceof FieldNamesAware) {
                ((FieldNamesAware) provider).setFieldNames(fieldNames);
            }
        }
    }
    
    public boolean isIncludeCallerData() {
        return callerDataProvider != null;
    }
    
    public void setIncludeCallerData(boolean includeCallerData) {
        if (isIncludeCallerData() != includeCallerData) {
            getProviders().removeProvider(callerDataProvider);
            if (includeCallerData) {
                callerDataProvider = new CallerDataJsonProvider();
                getProviders().addCallerData(callerDataProvider);
            } else {
                callerDataProvider = null;
            }
        }
    }

    /**
     * @deprecated use {@link #isIncludeCallerData()} (to use the same name that logback uses)
     */
    @Deprecated
    public boolean isIncludeCallerInfo() {
        return isIncludeCallerData();
    }
    
    /**
     * @deprecated use {@link #setIncludeCallerData(boolean)} (to use the same name that logback uses)
     */
    @Deprecated
    public void setIncludeCallerInfo(boolean includeCallerInfo) {
        setIncludeCallerData(includeCallerInfo);
    }
    
    public String getCustomFieldsAsString() {
        return globalCustomFieldsProvider == null
                ? null
                : globalCustomFieldsProvider.getCustomFields();
    }
    
    public void setCustomFieldsFromString(String customFields) {
        if (customFields == null || customFields.length() == 0) {
            getProviders().removeProvider(globalCustomFieldsProvider);
            globalCustomFieldsProvider = null;
        } else {
            if (globalCustomFieldsProvider == null) {
                getProviders().addGlobalCustomFields(globalCustomFieldsProvider = new GlobalCustomFieldsJsonProvider<ILoggingEvent>());
            }
            globalCustomFieldsProvider.setCustomFields(customFields);
        }
    }
    
    public void setCustomFields(JsonNode customFields) {
        if (customFields == null) {
            getProviders().removeProvider(globalCustomFieldsProvider);
            globalCustomFieldsProvider = null;
        } else {
            if (globalCustomFieldsProvider == null) {
                getProviders().addGlobalCustomFields(globalCustomFieldsProvider = new GlobalCustomFieldsJsonProvider<ILoggingEvent>());
            }
            globalCustomFieldsProvider.setCustomFieldsNode(customFields);
        }
    }
    
    public JsonNode getCustomFields() {
        return globalCustomFieldsProvider == null
                ? null
                : globalCustomFieldsProvider.getCustomFieldsNode();
    }
    
    public int getShortenedLoggerNameLength() {
        return loggerNameProvider.getShortenedLoggerNameLength();
    }

    public void setShortenedLoggerNameLength(int length) {
        loggerNameProvider.setShortenedLoggerNameLength(length);
    }
    
    public boolean isIncludeMdc() {
        return this.mdcProvider != null;
    }
    
    public void setIncludeMdc(boolean includeMdc) {
        if (isIncludeMdc() != includeMdc) {
            getProviders().removeProvider(mdcProvider);
            if (includeMdc) {
                mdcProvider = new MdcJsonProvider();
                getProviders().addMdc(mdcProvider);
            } else {
                mdcProvider = null;
            }
        }
    }
    
    public List<String> getIncludeMdcKeyNames() {
        return isIncludeMdc()
                ? mdcProvider.getIncludeMdcKeyNames()
                : Collections.<String>emptyList();
    }
    public void addIncludeMdcKeyName(String includedMdcKeyName) {
        if (isIncludeMdc()) {
            mdcProvider.addIncludeMdcKeyName(includedMdcKeyName);
        }
    }
    public void setIncludeMdcKeyNames(List<String> includeMdcKeyNames) {
        if (isIncludeMdc()) {
            mdcProvider.setIncludeMdcKeyNames(includeMdcKeyNames);
        }
    }
    
    public List<String> getExcludeMdcKeyNames() {
        return isIncludeMdc()
                ? mdcProvider.getExcludeMdcKeyNames()
                : Collections.<String>emptyList();
    }
    public void addExcludeMdcKeyName(String excludedMdcKeyName) {
        if (isIncludeMdc()) {
            mdcProvider.addExcludeMdcKeyName(excludedMdcKeyName);
        }
    }
    public void setExcludeMdcKeyNames(List<String> excludeMdcKeyNames) {
        if (isIncludeMdc()) {
            mdcProvider.setExcludeMdcKeyNames(excludeMdcKeyNames);
        }
    }
    
    public boolean isIncludeContext() {
        return contextProvider != null;
    }
    
    public void setIncludeContext(boolean includeContext) {
        if (isIncludeContext() != includeContext) {
            getProviders().removeProvider(contextProvider);
            if (includeContext) {
                contextProvider = new ContextJsonProvider<ILoggingEvent>();
                getProviders().addContext(contextProvider);
            } else {
                contextProvider = null;
            }
        }
    }
    
    public ThrowableHandlingConverter getThrowableConverter() {
        return this.stackTraceProvider.getThrowableConverter();
    }

    public void setThrowableConverter(ThrowableHandlingConverter throwableConverter) {
        this.stackTraceProvider.setThrowableConverter(throwableConverter);
    }
    
    public int getVersion() {
        return this.versionProvider.getVersion();
    }
    public void setVersion(int version) {
        this.versionProvider.setVersion(version);
    }
    
    public boolean isWriteVersionAsString() {
        return this.versionProvider.isWriteAsString();
    }
    public void setWriteVersionAsString(boolean writeVersionAsString) {
        this.versionProvider.setWriteAsString(writeVersionAsString);
    }
    
    /**
     * @deprecated When logging, prefer using a {@link Markers#appendEntries(Map)} marker instead.
     */
    @Deprecated
    public boolean isEnableContextMap() {
        return contextMapProvider != null;
    }
    
    /**
     * @deprecated When logging, prefer using a {@link Markers#appendEntries(Map)} marker instead.
     */
    @Deprecated
    public void setEnableContextMap(boolean enableContextMap) {
        if (isEnableContextMap() != enableContextMap) {
            getProviders().removeProvider(contextMapProvider);
            if (enableContextMap) {
                contextMapProvider = new ContextMapJsonProvider();
                getProviders().addContextMap(contextMapProvider);
            } else {
                contextMapProvider = null;
            }
        }
    }
    
    public void addProvider(JsonProvider<ILoggingEvent> provider) {
        getProviders().addProvider(provider);
    }
    
    @Override
    public LoggingEventJsonProviders getProviders() {
        return (LoggingEventJsonProviders) super.getProviders();
    }
    
    public LogstashFieldNames getFieldNames() {
        return fieldNames;
    }

    public void setFieldNames(LogstashFieldNames fieldNames) {
        this.fieldNames = fieldNames;
    }

    public String getTimeZone() {
        return timestampProvider.getTimeZone();
    }
    public void setTimeZone(String timeZoneId) {
        this.timestampProvider.setTimeZone(timeZoneId);
    }
    public String getTimestampPattern() {
        return timestampProvider.getPattern();
    }
    public void setTimestampPattern(String pattern) {
        timestampProvider.setPattern(pattern);
    }
    
    @Override
    public void setProviders(JsonProviders<ILoggingEvent> jsonProviders) {
        if (super.getProviders() != null && !super.getProviders().getProviders().isEmpty()) {
            addError("Unable to set providers when using predefined composites.");
        } else {
            super.setProviders(jsonProviders);
        }
    }
}
