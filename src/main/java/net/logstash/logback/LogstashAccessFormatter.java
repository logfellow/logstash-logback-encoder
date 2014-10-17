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

import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.fieldnames.LogstashAccessFieldNames;

/**
 *
 */
public class LogstashAccessFormatter extends LogstashAbstractFormatter<IAccessEvent, LogstashAccessFieldNames> {
    public LogstashAccessFormatter() {
        super(new LogstashAccessFieldNames());
    }

    @Override
    protected void writeValueToGenerator(JsonGenerator generator, IAccessEvent event, Context context) throws IOException {
        
        generator.writeStartObject();
        generator.writeStringField(fieldNames.getTimestamp(), ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS.format(event.getTimeStamp()));
        generator.writeNumberField(fieldNames.getVersion(), 1);
        generator.writeStringField(
                fieldNames.getMessage(),
                String.format("%s - %s [%s] \"%s\" %s %s", event.getRemoteHost(), event.getRemoteUser() == null ? "-" : event.getRemoteUser(),
                        ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS.format(event.getTimeStamp()), event.getRequestURL(), event.getStatusCode(),
                        event.getContentLength()));
        
        writeFields(generator, event, context);
        generator.writeEndObject();
        generator.flush();
    }
    
    private void writeFields(JsonGenerator generator, IAccessEvent event, Context context) throws IOException {
        
        generator.writeStringField(fieldNames.getFieldsMethod(), event.getMethod());
        generator.writeStringField(fieldNames.getFieldsProtocol(), event.getProtocol());
        generator.writeNumberField(fieldNames.getFieldsStatusCode(), event.getStatusCode());
        generator.writeStringField(fieldNames.getFieldsRequestedUrl(), event.getRequestURL());
        generator.writeStringField(fieldNames.getFieldsRequestedUri(), event.getRequestURI());
        generator.writeStringField(fieldNames.getFieldsRemoteHost(), event.getRemoteHost());
        generator.writeStringField(fieldNames.getFieldsHostname(), event.getRemoteHost());
        generator.writeStringField(fieldNames.getFieldsRemoteUser(), event.getRemoteUser());
        generator.writeNumberField(fieldNames.getFieldsContentLength(), event.getContentLength());
        generator.writeNumberField(fieldNames.getFieldsElapsedTime(), event.getElapsedTime());
        
        writeContextPropertiesIfNecessary(generator, context);
    }
    
    private void writeContextPropertiesIfNecessary(JsonGenerator generator, Context context) throws IOException {
        if (context != null) {
            writeMapEntries(generator, context.getCopyOfPropertyMap());
        }
    }
}
