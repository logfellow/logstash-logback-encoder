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

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.logstash.logback.fieldnames.LogstashFieldNames;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;

import org.slf4j.MDC;
import org.slf4j.Marker;

import ch.qos.logback.classic.pattern.Abbreviator;
import ch.qos.logback.classic.pattern.TargetLengthBasedClassNameAbbreviator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAware;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 
 */
public class LogstashFormatter extends LogstashAbstractFormatter<ILoggingEvent, LogstashFieldNames> {
    
    /**
     * Name of the {@link Marker} that indicates that the log event arguments should be appended to the
     * logstash json as an array with field value "json_message".
     * 
     * @deprecated When logging, prefer using a {@link Markers#appendArray(String, Object...)} marker
     *             with fieldName = "json_message" and objects = an array of arguments instead.
     */
    @Deprecated
    private static final String JSON_MARKER_NAME = "JSON";

    private static final StackTraceElement DEFAULT_CALLER_DATA = new StackTraceElement("", "", "", 0);
    
    /**
     * If true, the caller information is included in the logged data.
     * Note: calculating the caller data is an expensive operation.
     */
    private boolean includeCallerInfo;
    
    /**
     * When true, if the last argument to the log line is a map, then it will be embedded in the logstash json.
     * 
     * @deprecated When logging, prefer using a {@link Markers#appendEntries(Map)} marker instead.
     */
    @Deprecated
    private boolean enableContextMap;
    
    /**
     * The un-parsed custom fields string to use to initialize customFields
     * when the formatter is started.
     */
    private String customFieldString;

    /**
     * When non-null, the fields in this JsonNode will be embedded in the logstash json.
     */
    private JsonNode customFields;

    /**
     * When set to anything >= 0 we will try to abbreviate the logger name
     */
    private int shortenedLoggerNameLength = -1;

    /**
     * Abbreviator that will shorten the logger classname if shortenedLoggerNameLength is set
     */
    private Abbreviator abbreviator;

    /**
     * When true, logback's {@link Context} properties will be included.
     */
    private boolean includeContext = true;
    
    /**
     * When true, {@link MDC} properties will be included.
     */
    private boolean includeMdc = true;

    public LogstashFormatter(ContextAware contextAware) {
        this(contextAware, false);
    }
    
    public LogstashFormatter(ContextAware contextAware, boolean includeCallerInfo) {
        this(contextAware, includeCallerInfo, null);
        this.includeCallerInfo = includeCallerInfo;
    }
    
    public LogstashFormatter(ContextAware contextAware, boolean includeCallerInfo, JsonNode customFields) {
        super(contextAware, new LogstashFieldNames());

        this.includeCallerInfo = includeCallerInfo;
        this.customFields = customFields;
    }
    
    @Override
    public void start() {
        super.start();
        initializeCustomFields();
    }

    private void initializeCustomFields() {
        if (this.customFieldString != null) {
            try {
                this.customFields = getJsonFactory().createParser(customFieldString).readValueAsTree();
            } catch (IOException e) {
                contextAware.addError("Failed to parse custom fields [" + customFieldString + "]", e);
            }
        }
    }

    @Override
    protected void writeValueToGenerator(JsonGenerator generator, ILoggingEvent event, Context context) throws IOException {
        
        generator.writeStartObject();
        writeLogstashFields(generator, event);
        writeLoggerFields(generator, event);
        writeCallerDataFieldsIfNecessary(generator, event);
        writeStackTraceFieldIfNecessary(generator, event);
        writeContextPropertiesIfNecessary(generator, context);
        writeJsonMessageFieldIfNecessary(generator, event);
        writeMdcPropertiesIfNecessary(generator, event);
        writeContextMapFieldsIfNecessary(generator, event);
        writeGlobalCustomFields(generator);
        writeTagsIfNecessary(generator, event);
        writeLogstashMarkerIfNecessary(generator, event.getMarker());
        generator.writeEndObject();
        generator.flush();
    }
    
    private void writeLogstashFields(JsonGenerator generator, ILoggingEvent event) throws IOException {
        writeStringField(generator, fieldNames.getTimestamp(), ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS.format(event.getTimeStamp()));
        writeNumberField(generator, fieldNames.getVersion(), 1);
        writeStringField(generator, fieldNames.getMessage(), event.getFormattedMessage());
    }
    
    private void writeLoggerFields(JsonGenerator generator, ILoggingEvent event) throws IOException {
        // according to documentation (http://logback.qos.ch/manual/layouts.html#conversionWord) length can be >=0
        if (shortenedLoggerNameLength >= 0) {
            writeStringField(generator, fieldNames.getLogger(), abbreviator.abbreviate(event.getLoggerName()));
        } else {
            writeStringField(generator, fieldNames.getLogger(), event.getLoggerName());
        }
        writeStringField(generator, fieldNames.getThread(), event.getThreadName());
        writeStringField(generator, fieldNames.getLevel(), event.getLevel().toString());
        writeNumberField(generator, fieldNames.getLevelValue(), event.getLevel().toInt());
    }
    
    private void writeCallerDataFieldsIfNecessary(JsonGenerator generator, ILoggingEvent event) throws IOException {
        if (includeCallerInfo) {
            StackTraceElement callerData = extractCallerData(event);
            if (fieldNames.getCaller() != null) {
                generator.writeObjectFieldStart(fieldNames.getCaller());
            }
            writeStringField(generator, fieldNames.getCallerClass(), callerData.getClassName());
            writeStringField(generator, fieldNames.getCallerMethod(), callerData.getMethodName());
            writeStringField(generator, fieldNames.getCallerFile(), callerData.getFileName());
            writeNumberField(generator, fieldNames.getCallerLine(), callerData.getLineNumber());
            if (fieldNames.getCaller() != null) {
                generator.writeEndObject();
            }
        }
    }
    
    private void writeStackTraceFieldIfNecessary(JsonGenerator generator, ILoggingEvent event) throws IOException {
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            writeStringField(generator, fieldNames.getStackTrace(), ThrowableProxyUtil.asString(throwableProxy));
        }
    }
    
    private void writeContextPropertiesIfNecessary(JsonGenerator generator, Context context) throws IOException {
        if (context != null && includeContext) {
            if (fieldNames.getContext() != null) {
                generator.writeObjectFieldStart(fieldNames.getContext());
            }
            writeMapEntries(generator, context.getCopyOfPropertyMap());
            
            if (fieldNames.getContext() != null) {
                generator.writeEndObject();
            }
            
        }
    }
    
    /**
     * Writes the event arguments as a json array to the field named "json_message"
     * 
     * @deprecated When logging, prefer using a {@link Markers#appendArray(String, Object...)} marker
     *             with fieldName = "json_message" and objects = an array of arguments instead.
     */
    @Deprecated
    private void writeJsonMessageFieldIfNecessary(JsonGenerator generator, ILoggingEvent event) throws IOException {
        final Marker marker = event.getMarker();
        if (marker != null && marker.contains(JSON_MARKER_NAME)) {
            generator.writeFieldName("json_message");
            generator.writeObject(event.getArgumentArray());
        }
    }
    
    private void writeMdcPropertiesIfNecessary(JsonGenerator generator, ILoggingEvent event) throws IOException {
        if (includeMdc) {
            Map<String, String> mdcProperties = event.getMDCPropertyMap();
            if (mdcProperties != null && !mdcProperties.isEmpty()) {
                if (fieldNames.getMdc() != null) {
                    generator.writeObjectFieldStart(fieldNames.getMdc());
                }
                writeMapEntries(generator, mdcProperties);
                if (fieldNames.getMdc() != null) {
                    generator.writeEndObject();
                }
            }
        }
    }
    
    /**
     * If {@link #enableContextMap} is true, and the last event argument is a map, then
     * embeds the map entries in the logstash json
     * 
     * @deprecated When logging, prefer using a {@link Markers#appendEntries(Map)} marker instead.
     */
    @Deprecated
    private void writeContextMapFieldsIfNecessary(JsonGenerator generator, ILoggingEvent event) throws IOException {
        if (enableContextMap) {
            Object[] args = event.getArgumentArray();
            if (args != null && args.length > 0 && args[args.length - 1] instanceof Map) {
                Map<?, ?> contextMap = (Map<?, ?>) args[args.length - 1];
                writeMapEntries(generator, contextMap);
            }
        }
    }

    private void writeGlobalCustomFields(JsonGenerator generator) throws IOException {
        writeFieldsOfNode(generator, customFields);
    }
    
    private void writeTagsIfNecessary(JsonGenerator generator, ILoggingEvent event) throws IOException {
        /*
         * Don't write the tags field unless we actually have a tag to write.
         */
        boolean hasWrittenStart = false;
        
        final Marker marker = event.getMarker();
        
        if (marker != null) {
            hasWrittenStart = writeTagIfNecessary(generator, hasWrittenStart, marker);
        }
        
        if (hasWrittenStart) {
            generator.writeEndArray();
        }
    }
    
    private boolean writeTagIfNecessary(JsonGenerator generator, boolean hasWrittenStart, final Marker marker) throws IOException {
        if (!marker.getName().equals(JSON_MARKER_NAME) && !isLogstashMarker(marker)) {
            if (!hasWrittenStart) {
                generator.writeArrayFieldStart(fieldNames.getTags());
                hasWrittenStart = true;
            }
            generator.writeString(marker.getName());
        }
        if (marker.hasReferences()) {
            
            for (Iterator<?> i = marker.iterator(); i.hasNext();) {
                Marker next = (Marker) i.next();
                
                hasWrittenStart |= writeTagIfNecessary(generator, hasWrittenStart, next);
            }
        }
        return hasWrittenStart;
    }
    
    private boolean isLogstashMarker(Marker marker) {
        return marker instanceof LogstashMarker;
    }
    
    private void writeLogstashMarkerIfNecessary(JsonGenerator generator, Marker marker) throws IOException {
        if (marker != null) {
            if (isLogstashMarker(marker)) {
                ((LogstashMarker) marker).writeTo(generator);
            }
            
            if (marker.hasReferences()) {
                for (Iterator<?> i = marker.iterator(); i.hasNext();) {
                    Marker next = (Marker) i.next();
                    writeLogstashMarkerIfNecessary(generator, next);
                }
            }
        }
    }
    
    private StackTraceElement extractCallerData(final ILoggingEvent event) {
        final StackTraceElement[] ste = event.getCallerData();
        if (ste == null || ste.length == 0) {
            return DEFAULT_CALLER_DATA;
        }
        return ste[0];
    }
    
    /**
     * Writes the fields of the given node into the generator.
     */
    private void writeFieldsOfNode(JsonGenerator generator, JsonNode node) throws IOException {
        if (node != null) {
            for (Iterator<Entry<String, JsonNode>> fields = node.fields(); fields.hasNext();) {
                Entry<String, JsonNode> field = fields.next();
                generator.writeFieldName(field.getKey());
                generator.writeTree(field.getValue());
            }
        }
    }
    
    public boolean isIncludeCallerInfo() {
        return includeCallerInfo;
    }
    
    public void setIncludeCallerInfo(boolean includeCallerInfo) {
        this.includeCallerInfo = includeCallerInfo;
    }
    
    public void setCustomFieldsFromString(String customFields) {
        this.customFieldString = customFields;
        if (isStarted()) {
            initializeCustomFields();
        }
    }
    
    public void setCustomFields(JsonNode customFields) {
        this.customFields = customFields;
    }
    
    public JsonNode getCustomFields() {
        return this.customFields;
    }
    
    public int getShortenedLoggerNameLength() {
		return shortenedLoggerNameLength;
	}

    public void setShortenedLoggerNameLength(int length) {
        this.shortenedLoggerNameLength = length;
        abbreviator = new CachingAbbreviator(new TargetLengthBasedClassNameAbbreviator(this.shortenedLoggerNameLength));
    }
    
    public boolean isIncludeMdc() {
        return includeMdc;
    }
    
    public void setIncludeMdc(boolean includeMdc) {
        this.includeMdc = includeMdc;
    }
    
    public boolean isIncludeContext() {
        return includeContext;
    }
    
    public void setIncludeContext(boolean includeContext) {
        this.includeContext = includeContext;
    }
    
    /**
     * @deprecated When logging, prefer using a {@link Markers#appendEntries(Map)} marker instead.
     */
    @Deprecated
    public boolean isEnableContextMap() {
        return enableContextMap;
    }
    
    /**
     * @deprecated When logging, prefer using a {@link Markers#appendEntries(Map)} marker instead.
     */
    @Deprecated
    public void setEnableContextMap(boolean enableContextMap) {
        this.enableContextMap = enableContextMap;
    }
}
