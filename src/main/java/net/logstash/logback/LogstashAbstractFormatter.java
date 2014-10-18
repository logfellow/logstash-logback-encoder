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

import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SegmentedStringWriter;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.logstash.logback.fieldnames.LogstashCommonFieldNames;
import org.apache.commons.lang.time.FastDateFormat;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.util.Map;

abstract class LogstashAbstractFormatter<EventType extends DeferredProcessingAware, FieldNamesType extends LogstashCommonFieldNames> {
    protected static final JsonFactory FACTORY = new MappingJsonFactory().enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);
    protected static final ObjectMapper MAPPER = new ObjectMapper(FACTORY);
    protected static final FastDateFormat ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

    /**
     * This <code>ThreadLocal</code> contains a {@link java.lang.ref.SoftReference} to a {@link BufferRecycler} used to provide a low-cost
     * buffer recycling between writer instances.
     */
    private final ThreadLocal<SoftReference<BufferRecycler>> recycler = new ThreadLocal<SoftReference<BufferRecycler>>() {
        protected SoftReference<BufferRecycler> initialValue() {
            final BufferRecycler bufferRecycler = new BufferRecycler();
            return new SoftReference<BufferRecycler>(bufferRecycler);
        }
    };

    /**
     * The field names to use when writing the standard event fields
     */
    protected FieldNamesType fieldNames;

    protected LogstashAbstractFormatter(FieldNamesType fieldNames) {
        this.fieldNames = fieldNames;
    }

    public byte[] writeValueAsBytes(EventType event, Context context) throws IOException {
        ByteArrayBuilder outputStream = new ByteArrayBuilder(getBufferRecycler());

        try {
            writeValueToOutputStream(event, context, outputStream);
            return outputStream.toByteArray();
        } finally {
            outputStream.release();
        }
    }

    public void writeValueToOutputStream(EventType event, Context context, OutputStream outputStream) throws IOException {
        JsonGenerator generator = FACTORY.createGenerator(outputStream);
        writeValueToGenerator(generator, event, context);
    }

    public String writeValueAsString(EventType event, Context context) throws IOException {
        SegmentedStringWriter writer = new SegmentedStringWriter(getBufferRecycler());

        JsonGenerator generator = FACTORY.createGenerator(writer);
        writeValueToGenerator(generator, event, context);
        return writer.getAndClear();
    }

    protected abstract void writeValueToGenerator(JsonGenerator generator, EventType event, Context context) throws IOException;

    protected void writeMapEntries(JsonGenerator generator, Map<?, ?> map) throws IOException, JsonMappingException {
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
    }

    public FieldNamesType getFieldNames() {
        return fieldNames;
    }

    public void setFieldNames(FieldNamesType fieldNames) {
        this.fieldNames = fieldNames;
    }
}
