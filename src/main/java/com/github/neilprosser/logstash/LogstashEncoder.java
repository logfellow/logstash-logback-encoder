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
package com.github.neilprosser.logstash;

import static org.apache.commons.io.IOUtils.*;
import static org.apache.commons.lang.StringUtils.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.DateTime;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.encoder.EncoderBase;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class LogstashEncoder extends EncoderBase<ILoggingEvent> {
    
    private static final ObjectMapper MAPPER = new ObjectMapper().configure(Feature.ESCAPE_NON_ASCII, true);
    private static final String HOSTNAME;
    
    private boolean immediateFlush = true;
    private String source;
    
    static {
        String hostName;
        try {
            hostName = lowerCase(InetAddress.getLocalHost().getCanonicalHostName());
        } catch (UnknownHostException e) {
            hostName = "unknown";
        }
        HOSTNAME = hostName;
    }
    
    @Override
    public void doEncode(ILoggingEvent event) throws IOException {
        
        ObjectNode eventNode = MAPPER.createObjectNode();
        eventNode.put("@timestamp", new DateTime(event.getTimeStamp()).toString());
        eventNode.put("@source_host", HOSTNAME);
        
        if (isNotEmpty(source)) {
            eventNode.put("@source", source);
        }
        
        eventNode.put("@message", event.getFormattedMessage());
        eventNode.put("@fields", createFields(event));
        
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
        
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            fieldsNode.put("stack_trace", ThrowableProxyUtil.asString(throwableProxy));
        }
        
        Map<String, String> mdc = event.getMDCPropertyMap();
        
        for (Entry<String, String> entry : mdc.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            fieldsNode.put(key, value);
        }
        
        return fieldsNode;
        
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
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
}
