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

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.Context;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.lang.time.FastDateFormat;
import java.io.IOException;
import java.util.Map;

/**
 *
 */
public class LogstashAccessFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper().configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
    private static final FastDateFormat ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

    public byte[] writeValueAsBytes(IAccessEvent event, Context context) throws IOException {
        return MAPPER.writeValueAsBytes(eventToNode(event, context));
    }

    public String writeValueAsString(IAccessEvent event, Context context) throws IOException {
        return MAPPER.writeValueAsString(eventToNode(event, context));
    }

    private ObjectNode eventToNode(IAccessEvent event, Context context) {
        ObjectNode eventNode = MAPPER.createObjectNode();
        eventNode.put("@timestamp", ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS.format(event.getTimeStamp()));
        eventNode.put("@version", 1);
        eventNode.put(
                "@message",
                String.format("%s - %s [%s] \"%s\" %s %s", event.getRemoteHost(), event.getRemoteUser() == null ? "-" : event.getRemoteUser(),
                        ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS.format(event.getTimeStamp()), event.getRequestURL(), event.getStatusCode(),
                        event.getContentLength()));
        createFields(event, context, eventNode);
        return eventNode;
    }

    private void createFields(IAccessEvent event, Context context, ObjectNode eventNode) {

        eventNode.put("@fields.method", event.getMethod());
        eventNode.put("@fields.protocol", event.getProtocol());
        eventNode.put("@fields.status_code", event.getStatusCode());
        eventNode.put("@fields.requested_url", event.getRequestURL());
        eventNode.put("@fields.requested_uri", event.getRequestURI());
        eventNode.put("@fields.remote_host", event.getRemoteHost());
        eventNode.put("@fields.HOSTNAME", event.getRemoteHost());
        eventNode.put("@fields.remote_user", event.getRemoteUser());
        eventNode.put("@fields.content_length", event.getContentLength());

        if (context != null) {
            addPropertiesAsFields(eventNode, context.getCopyOfPropertyMap());
        }
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
    
}
