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
import java.util.Map;

import org.apache.commons.lang.time.FastDateFormat;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.Context;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SegmentedStringWriter;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 */
public class LogstashAccessFormatter {
    
    private static final JsonFactory FACTORY = new MappingJsonFactory().enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);
    private static final ObjectMapper MAPPER = new ObjectMapper(FACTORY);
    private static final FastDateFormat ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
    
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

    public byte[] writeValueAsBytes(IAccessEvent event, Context context) throws IOException {
        ByteArrayBuilder outputStream = new ByteArrayBuilder(getBufferRecycler());
        
        try {
            writeValueToOutputStream(event, context, outputStream);
            return outputStream.toByteArray();
        } finally {
            outputStream.release();
        }
    }
    
    public void writeValueToOutputStream(IAccessEvent event, Context context, OutputStream outputStream) throws IOException {
        JsonGenerator generator = FACTORY.createGenerator(outputStream);
        writeValueToGenerator(generator, event, context);
    }

    public String writeValueAsString(IAccessEvent event, Context context) throws IOException {
        SegmentedStringWriter writer = new SegmentedStringWriter(getBufferRecycler());
        
        JsonGenerator generator = FACTORY.createGenerator(writer);
        writeValueToGenerator(generator, event, context);
        return writer.getAndClear();
    }
    
    private void writeValueToGenerator(JsonGenerator generator, IAccessEvent event, Context context) throws IOException {
        
        generator.writeStartObject();
        generator.writeStringField("@timestamp", ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS.format(event.getTimeStamp()));
        generator.writeNumberField("@version", 1);
        generator.writeStringField(
                "@message",
                String.format("%s - %s [%s] \"%s\" %s %s", event.getRemoteHost(), event.getRemoteUser() == null ? "-" : event.getRemoteUser(),
                        ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS.format(event.getTimeStamp()), event.getRequestURL(), event.getStatusCode(),
                        event.getContentLength()));
        
        
        writeFields(generator, event, context);
        generator.writeEndObject();
        generator.flush();
    }
    
    private void writeFields(JsonGenerator generator, IAccessEvent event, Context context) throws IOException {
        
        generator.writeStringField("@fields.method", event.getMethod());
        generator.writeStringField("@fields.protocol", event.getProtocol());
        generator.writeNumberField("@fields.status_code", event.getStatusCode());
        generator.writeStringField("@fields.requested_url", event.getRequestURL());
        generator.writeStringField("@fields.requested_uri", event.getRequestURI());
        generator.writeStringField("@fields.remote_host", event.getRemoteHost());
        generator.writeStringField("@fields.HOSTNAME", event.getRemoteHost());
        generator.writeStringField("@fields.remote_user", event.getRemoteUser());
        generator.writeNumberField("@fields.content_length", event.getContentLength());
        generator.writeNumberField("@fields.elapsed_time", event.getElapsedTime());
        
        writeContextPropertiesIfNecessary(generator, context);
    }
    
    private void writeContextPropertiesIfNecessary(JsonGenerator generator, Context context) throws IOException {
        if (context != null) {
            writeMapEntries(generator, context.getCopyOfPropertyMap());
        }
    }

    private void writeMapEntries(JsonGenerator generator, Map<?, ?> map) throws IOException, JsonGenerationException, JsonMappingException {
        if (map != null) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                generator.writeFieldName(entry.getKey().toString());
                MAPPER.writeValue(generator, entry.getValue());
            }
        }
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
