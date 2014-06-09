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

import org.apache.commons.lang.time.FastDateFormat;
import org.slf4j.Marker;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAware;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 
 */
public class LogstashFormatter {
    
    private static final ObjectMapper MAPPER = new ObjectMapper().configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
    private static final FastDateFormat ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
    private static final StackTraceElement DEFAULT_CALLER_DATA = new StackTraceElement("", "", "", 0);
    
    /**
     * If true, the caller information is included in the logged data.
     * Note: calculating the caller data is an expensive operation.
     */
    private boolean includeCallerInfo;
    private JsonNode customFields;
    
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
        return MAPPER.writeValueAsBytes(eventToNode(event, context));
    }
    
    public String writeValueAsString(ILoggingEvent event, Context context) throws IOException {
        return MAPPER.writeValueAsString(eventToNode(event, context));
    }
    
    private ObjectNode eventToNode(ILoggingEvent event, Context context) throws IOException {
        ObjectNode eventNode = MAPPER.createObjectNode();
        eventNode.put("@timestamp", ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS.format(event.getTimeStamp()));
        eventNode.put("@version", 1);
        eventNode.put("message", event.getFormattedMessage());
        createFields(event, context, eventNode);
        eventNode.put("tags", createTags(event));
        return eventNode;
    }
    
    private void createFields(ILoggingEvent event, Context context, ObjectNode eventNode) throws IOException {
        final Marker marker = event.getMarker();

        eventNode.put("logger_name", event.getLoggerName());
        eventNode.put("thread_name", event.getThreadName());
        eventNode.put("level", event.getLevel().toString());
        eventNode.put("level_value", event.getLevel().toInt());
        
        if (includeCallerInfo) {
            StackTraceElement callerData = extractCallerData(event);
            eventNode.put("caller_class_name", callerData.getClassName());
            eventNode.put("caller_method_name", callerData.getMethodName());
            eventNode.put("caller_file_name", callerData.getFileName());
            eventNode.put("caller_line_number", callerData.getLineNumber());
        }
        
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            eventNode.put("stack_trace", ThrowableProxyUtil.asString(throwableProxy));
        }
        
        if (context != null) {
            addPropertiesAsFields(eventNode, context.getCopyOfPropertyMap());
        }
        if (marker != null && marker.contains("JSON")) {
          eventNode.put("json_message", getJsonNode(event));
        }
        addPropertiesAsFields(eventNode, event.getMDCPropertyMap());
        
        addCustomFields(eventNode);
        
    }
    
    private ArrayNode createTags(ILoggingEvent event) {
        ArrayNode node = null;
        final Marker marker = event.getMarker();
        
        if (marker != null) {
            node = MAPPER.createArrayNode();
            if (!marker.getName().equals("JSON")) {
                node.add(marker.getName());
            }

            if (marker.hasReferences()) {
                final Iterator<?> i = event.getMarker().iterator();
                
                while (i.hasNext()) {
                    Marker next = (Marker) i.next();
                    
                    // attached markers will never be null as provided by the MarkerFactory.
                    if (!next.getName().equals("JSON")) {
                        node.add(next.getName());
                    }
                }
            }
        }
        
        return node;
    }
    
    private void addPropertiesAsFields(final ObjectNode fieldsNode, final Map<String, String> properties) {
        if (properties != null) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                fieldsNode.put(key, value);
            }
        }
    }

    private JsonNode getJsonNode(ILoggingEvent event) throws IOException {
        final Object[] args = event.getArgumentArray();

		return MAPPER.readTree((String)args[0]);
    }
        
    private StackTraceElement extractCallerData(final ILoggingEvent event) {
        final StackTraceElement[] ste = event.getCallerData();
        if (ste == null || ste.length == 0) {
            return DEFAULT_CALLER_DATA;
        }
        return ste[0];
    }
    
    private void addCustomFields(ObjectNode eventNode) {
        if (customFields != null) {
            Iterator<String> i = customFields.fieldNames();
            while (i.hasNext()) {
                String k = i.next();
                JsonNode v = customFields.get(k);
                eventNode.put(k, v);
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
        return new ObjectMapper().getFactory().createParser(customFields).readValueAsTree();
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
}
