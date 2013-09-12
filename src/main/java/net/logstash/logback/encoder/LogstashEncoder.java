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
package net.logstash.logback.encoder;

import static org.apache.commons.io.IOUtils.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang.time.FastDateFormat;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.encoder.EncoderBase;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Marker;

public class LogstashEncoder extends EncoderBase<ILoggingEvent> {
    
    private static final ObjectMapper MAPPER = new ObjectMapper().configure(Feature.ESCAPE_NON_ASCII, true);
    private static final FastDateFormat ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
    private static final StackTraceElement DEFAULT_CALLER_DATA = new StackTraceElement("", "", "", 0);
    
    private boolean immediateFlush = true;
    
    /**
     * If true, the caller information is included in the logged data.
     * Note: calculating the caller data is an expensive operation.
     */
    private boolean includeCallerInfo = true;
    
    @Override
    public void doEncode(ILoggingEvent event) throws IOException {
        
        ObjectNode eventNode = MAPPER.createObjectNode();
        eventNode.put("@timestamp", ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS.format(event.getTimeStamp()));
        eventNode.put("@message", event.getFormattedMessage());
        eventNode.put("@fields", createFields(event));
        eventNode.put("@tags", createTags(event));

        write(MAPPER.writeValueAsBytes(eventNode), outputStream);
        write(CoreConstants.LINE_SEPARATOR, outputStream);
        
        if (immediateFlush) {
            outputStream.flush();
        }
        
    }

    private ObjectNode createFields(ILoggingEvent event) {
        
        ObjectNode fieldsNode = MAPPER.createObjectNode();
        fieldsNode.put("logger_name", event.getLoggerName());
        fieldsNode.put("thread_name", event.getThreadName());
        fieldsNode.put("level", event.getLevel().toString());
        fieldsNode.put("level_value", event.getLevel().toInt());
        
        if (includeCallerInfo) {
            StackTraceElement callerData = extractCallerData(event);
            fieldsNode.put("caller_class_name", callerData.getClassName());
            fieldsNode.put("caller_method_name", callerData.getMethodName());
            fieldsNode.put("caller_file_name", callerData.getFileName());
            fieldsNode.put("caller_line_number", callerData.getLineNumber());
        }
        
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            fieldsNode.put("stack_trace", ThrowableProxyUtil.asString(throwableProxy));
        }
        
        Context context = getContext();
        if (context != null) {
            addPropertiesAsFields(fieldsNode, context.getCopyOfPropertyMap());
        }
        addPropertiesAsFields(fieldsNode, event.getMDCPropertyMap());
        
        return fieldsNode;
        
    }

    private ArrayNode createTags(ILoggingEvent event) {
        ArrayNode node = null;
        final Marker marker = event.getMarker();

        if (marker != null) {
            node = MAPPER.createArrayNode();
            node.add(marker.getName());

            if (marker.hasReferences()) {
                final Iterator i = event.getMarker().iterator();

                while (i.hasNext()) {
                    Marker next = (Marker) i.next();

                    // attached markers will never be null as provided by the MarkerFactory.
                    node.add(next.getName());
                }
            }
        }

        return node;
    }
    
    private void addPropertiesAsFields(final ObjectNode fieldsNode, final Map<String, String> properties) {
        if (properties != null) {
            for (Entry<String, String> entry : properties.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                fieldsNode.put(key, value);
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
    
    @Override
    public void close() throws IOException {
        write(LINE_SEPARATOR, outputStream);
    }
    
    public boolean isImmediateFlush() {
        return immediateFlush;
    }
    
    public void setImmediateFlush(boolean immediateFlush) {
        this.immediateFlush = immediateFlush;
    }

    public boolean isIncludeCallerInfo() {
        return includeCallerInfo;
    }

    public void setIncludeCallerInfo(boolean includeCallerInfo) {
        this.includeCallerInfo = includeCallerInfo;
    }
    
    
    
}
