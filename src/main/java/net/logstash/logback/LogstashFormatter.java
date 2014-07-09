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
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.time.FastDateFormat;
import org.slf4j.Marker;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAware;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.SegmentedStringWriter;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 
 */
public class LogstashFormatter {
    
    private static final JsonFactory FACTORY = new MappingJsonFactory().enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);
    private static final ObjectMapper MAPPER = new ObjectMapper(FACTORY);
    private static final FastDateFormat ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
    private static final StackTraceElement DEFAULT_CALLER_DATA = new StackTraceElement("", "", "", 0);
    
    /**
     * If true, the caller information is included in the logged data.
     * Note: calculating the caller data is an expensive operation.
     */
    private boolean includeCallerInfo;
    private boolean enableContextMap;
    private JsonNode customFields;
    
    /**
     * This <code>ThreadLocal</code> contains a {@link java.lang.ref.SoftReference}
     * to a {@link BufferRecycler} used to provide a low-cost
     * buffer recycling between writer instances.
     */
    private final ThreadLocal<SoftReference<BufferRecycler>> recycler = new ThreadLocal<SoftReference<BufferRecycler>>() {
        protected SoftReference<BufferRecycler> initialValue() {
            BufferRecycler bufferRecycler = new BufferRecycler();
            return new SoftReference<BufferRecycler>(bufferRecycler);
        };
    };

    public LogstashFormatter() {
        this(false);
    }
    
    public LogstashFormatter(boolean includeCallerInfo) {
        this.includeCallerInfo = includeCallerInfo;
    }
    
    public LogstashFormatter(boolean includeCallerInfo, JsonNode customFields) {
        this.includeCallerInfo = includeCallerInfo;
        this.customFields = customFields;
    }
    
    public byte[] writeValueAsBytes(ILoggingEvent event, Context context) throws IOException {
        ByteArrayBuilder outputStream = new ByteArrayBuilder(getBufferRecycler());
        
        try {
            writeValueToOutputStream(event, context, outputStream);
            return outputStream.toByteArray();
        } finally {
            outputStream.release();
        }
    }
    
    public void writeValueToOutputStream(ILoggingEvent event, Context context, OutputStream outputStream) throws IOException {
        JsonGenerator generator = FACTORY.createGenerator(outputStream);
        writeValueToGenerator(generator, event, context);
    }

    public String writeValueAsString(ILoggingEvent event, Context context) throws IOException {
        SegmentedStringWriter writer = new SegmentedStringWriter(getBufferRecycler());
        
        JsonGenerator generator = FACTORY.createGenerator(writer);
        writeValueToGenerator(generator, event, context);
        return writer.getAndClear();
    }
    
    private void writeValueToGenerator(JsonGenerator generator, ILoggingEvent event, Context context) throws IOException {
        
        generator.writeStartObject();
        writeLogstashFields(generator, event);
        writeLoggerFields(generator, event);
        writeCallerDataFieldsIfNecessary(generator, event);
        writeStackTraceFieldIfNecessary(generator, event);
        writeContextPropertiesIfNecessary(generator, context);
        writeJsonMessageFieldIfNecessary(generator, event);
        writeMdcPropertiesIfNecessary(generator, event);
        writeContextMapFieldsIfNecessary(generator, event);
        writeCustomFields(generator);
        writeTagsIfNecessary(generator, event);
        generator.writeEndObject();
        generator.flush();
    }

    private void writeLogstashFields(JsonGenerator generator, ILoggingEvent event) throws IOException {
        generator.writeStringField("@timestamp", ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS.format(event.getTimeStamp()));
        generator.writeNumberField("@version", 1);
        generator.writeStringField("message", event.getFormattedMessage());
    }
    
    private void writeLoggerFields(JsonGenerator generator, ILoggingEvent event) throws IOException {
        generator.writeStringField("logger_name", event.getLoggerName());
        generator.writeStringField("thread_name", event.getThreadName());
        generator.writeStringField("level", event.getLevel().toString());
        generator.writeNumberField("level_value", event.getLevel().toInt());
    }

    private void writeCallerDataFieldsIfNecessary(JsonGenerator generator, ILoggingEvent event) throws IOException {
        if (includeCallerInfo) {
            StackTraceElement callerData = extractCallerData(event);
            generator.writeStringField("caller_class_name", callerData.getClassName());
            generator.writeStringField("caller_method_name", callerData.getMethodName());
            generator.writeStringField("caller_file_name", callerData.getFileName());
            generator.writeNumberField("caller_line_number", callerData.getLineNumber());
        }
    }

    private void writeStackTraceFieldIfNecessary(JsonGenerator generator, ILoggingEvent event) throws IOException {
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            generator.writeStringField("stack_trace", ThrowableProxyUtil.asString(throwableProxy));
        }
    }

    private void writeContextPropertiesIfNecessary(JsonGenerator generator, Context context) throws IOException {
        if (context != null) {
            writePropertiesAsFields(generator, context.getCopyOfPropertyMap());
        }
    }

    private void writeJsonMessageFieldIfNecessary(JsonGenerator generator, ILoggingEvent event) throws IOException {
        final Marker marker = event.getMarker();
        if (marker != null && marker.contains("JSON")) {
            generator.writeFieldName("json_message");
            generator.writeTree(getJsonNode(event));
        }
    }

    private void writeMdcPropertiesIfNecessary(JsonGenerator generator, ILoggingEvent event) throws IOException {
        writePropertiesAsFields(generator, event.getMDCPropertyMap());
    }

    private void writeContextMapFieldsIfNecessary(JsonGenerator generator, ILoggingEvent event) throws IOException {
        if (enableContextMap) {
            Object[] args = event.getArgumentArray();
            if (args != null && args.length > 0 && args[args.length - 1] instanceof Map) {
                Map<?, ?> contextMap = (Map<?, ?>) args[args.length - 1];
                
                ObjectNode context = MAPPER.convertValue(contextMap, ObjectNode.class);
                
                writeFieldsOfNode(generator, context);
            }
        }
    }

    private void writeCustomFields(JsonGenerator generator) throws IOException {
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
        if (!marker.getName().equals("JSON")) {
            if (!hasWrittenStart) {
                generator.writeArrayFieldStart("tags");
                hasWrittenStart = true;
            }
            generator.writeString(marker.getName());
        }
        if (marker.hasReferences()) {
            
            for (Iterator<?> i = marker.iterator(); i.hasNext(); ) {
                Marker next = (Marker) i.next();
                
                hasWrittenStart |= writeTagIfNecessary(generator, hasWrittenStart, next);
            }
        }
        return hasWrittenStart;
    }
    
    private void writePropertiesAsFields(JsonGenerator generator, Map<String, String> properties) throws IOException {
        if (properties != null) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                generator.writeStringField(key, value);
            }
        }
    }
    
    private JsonNode getJsonNode(ILoggingEvent event) {
        final Object[] args = event.getArgumentArray();
        
        return MAPPER.convertValue(args, JsonNode.class);
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
            for (Iterator<Entry<String, JsonNode>> fields = node.fields(); fields.hasNext(); ) {
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
    
    public static JsonNode parseCustomFields(String customFields) throws JsonParseException, JsonProcessingException, IOException {
        return FACTORY.createParser(customFields).readValueAsTree();
    }
    
    public void setCustomFieldsFromString(String customFields, ContextAware contextAware) {
        try {
            setCustomFields(parseCustomFields(customFields));
        } catch (IOException e) {
            contextAware.addError("Failed to parse custom fields [" + customFields + "]", e);
        }
    }
    
    public void setCustomFields(JsonNode customFields) {
        this.customFields = customFields;
    }
    
    public JsonNode getCustomFields() {
        return this.customFields;
    }
    
    public boolean isEnableContextMap() {
        return enableContextMap;
    }
    
    public void setEnableContextMap(boolean enableContextMap) {
        this.enableContextMap = enableContextMap;
    }
    
    private BufferRecycler getBufferRecycler() {
        SoftReference<BufferRecycler> bufferRecyclerReference = recycler.get();
        BufferRecycler bufferRecycler = bufferRecyclerReference.get();
        if (bufferRecycler == null) {
            recycler.remove();
            return getBufferRecycler();
        }
        return bufferRecycler;
    };
}
