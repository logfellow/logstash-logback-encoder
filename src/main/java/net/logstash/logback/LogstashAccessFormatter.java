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

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAware;

import com.fasterxml.jackson.core.JsonGenerator;

import net.logstash.logback.fieldnames.LogstashAccessFieldNames;

/**
 *
 */
public class LogstashAccessFormatter extends LogstashAbstractFormatter<IAccessEvent, LogstashAccessFieldNames> {
    public LogstashAccessFormatter(ContextAware contextAware) {
        super(contextAware, new LogstashAccessFieldNames());
    }

    @Override
    protected void writeValueToGenerator(JsonGenerator generator, IAccessEvent event, Context context) throws IOException {
        
        generator.writeStartObject();
        writeStringField(generator, fieldNames.getTimestamp(), isoDateTimeTimeZoneFormatWithMillis.format(event.getTimeStamp()));
        writeNumberField(generator, fieldNames.getVersion(), 1);
        writeStringField(generator, 
                fieldNames.getMessage(),
                String.format("%s - %s [%s] \"%s\" %s %s", event.getRemoteHost(), event.getRemoteUser() == null ? "-" : event.getRemoteUser(),
                        isoDateTimeTimeZoneFormatWithMillis.format(event.getTimeStamp()), event.getRequestURL(), event.getStatusCode(),
                        event.getContentLength()));
        
        writeFields(generator, event, context);
        generator.writeEndObject();
        generator.flush();
    }
    
    private void writeFields(JsonGenerator generator, IAccessEvent event, Context context) throws IOException {
        
        writeStringField(generator, fieldNames.getFieldsMethod(), event.getMethod());
        writeStringField(generator, fieldNames.getFieldsProtocol(), event.getProtocol());
        writeNumberField(generator, fieldNames.getFieldsStatusCode(), event.getStatusCode());
        writeStringField(generator, fieldNames.getFieldsRequestedUrl(), event.getRequestURL());
        writeStringField(generator, fieldNames.getFieldsRequestedUri(), event.getRequestURI());
        writeStringField(generator, fieldNames.getFieldsRemoteHost(), event.getRemoteHost());
        writeStringField(generator, fieldNames.getFieldsHostname(), event.getRemoteHost());
        writeStringField(generator, fieldNames.getFieldsRemoteUser(), event.getRemoteUser());
        writeNumberField(generator, fieldNames.getFieldsContentLength(), event.getContentLength());
        writeNumberField(generator, fieldNames.getFieldsElapsedTime(), event.getElapsedTime());
        writeMapStringFields(generator, fieldNames.getFieldsRequestHeaders(), event.getRequestHeaderMap());
        writeMapStringFields(generator, fieldNames.getFieldsResponseHeaders(), event.getResponseHeaderMap());
        
        writeContextPropertiesIfNecessary(generator, context);
    }
    
    private void writeContextPropertiesIfNecessary(JsonGenerator generator, Context context) throws IOException {
        if (context != null) {
            writeMapEntries(generator, context.getCopyOfPropertyMap());
        }
    }
}
