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
package net.logstash.logback.composite;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.ref.SoftReference;
import java.util.ServiceConfigurationError;

import net.logstash.logback.LifeCycleManager;
import net.logstash.logback.decorate.JsonFactoryDecorator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;
import net.logstash.logback.decorate.NullJsonFactoryDecorator;
import net.logstash.logback.decorate.NullJsonGeneratorDecorator;
import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.spi.LifeCycle;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SegmentedStringWriter;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Formats logstash Events as JSON using {@link JsonProvider}s.
 * <p>
 *
 * The {@link CompositeJsonFormatter} starts the JSON object ('{'),
 * then delegates writing the contents of the object to the {@link JsonProvider}s,
 * and then ends the JSON object ('}').
 *
 * @param <Event> type of event ({@link ILoggingEvent} or {@link IAccessEvent}).
 */
public abstract class CompositeJsonFormatter<Event extends DeferredProcessingAware>
        extends ContextAwareBase implements LifeCycle {

    /**
     * This <code>ThreadLocal</code> contains a {@link java.lang.ref.SoftReference}
     * to a {@link BufferRecycler} used to provide a low-cost
     * buffer recycling between writer instances.
     */
    private final ThreadLocal<SoftReference<BufferRecycler>> recycler = new ThreadLocal<SoftReference<BufferRecycler>>() {
        protected SoftReference<BufferRecycler> initialValue() {
            final BufferRecycler bufferRecycler = new BufferRecycler();
            return new SoftReference<BufferRecycler>(bufferRecycler);
        }
    };

    /**
     * Used to create the necessary {@link JsonGenerator}s for generating JSON.
     */
    private JsonFactory jsonFactory;

    /**
     * Decorates the {@link #jsonFactory}.
     * Allows customization of the {@link #jsonFactory}.
     */
    private JsonFactoryDecorator jsonFactoryDecorator = new NullJsonFactoryDecorator();

    /**
     * Decorates the generators generated by the {@link #jsonFactory}.
     * Allows customization of the generators.
     */
    private JsonGeneratorDecorator jsonGeneratorDecorator = new NullJsonGeneratorDecorator();

    /**
     * The providers that are used to populate the output JSON object.
     */
    private JsonProviders<Event> jsonProviders = new JsonProviders<Event>();

    /**
     * Manages the lifecycle of subcomponents
     */
    private final LifeCycleManager lifecycleManager = new LifeCycleManager();

    private JsonEncoding encoding = JsonEncoding.UTF8;

    private boolean findAndRegisterJacksonModules = true;

    private volatile boolean started;

    public CompositeJsonFormatter(ContextAware declaredOrigin) {
        super(declaredOrigin);
    }

    @Override
    public void start() {
        if (jsonProviders.getProviders().isEmpty()) {
            addError("No providers configured");
        }
        jsonFactory = createJsonFactory();
        jsonProviders.setContext(context);
        jsonProviders.setJsonFactory(jsonFactory);
        lifecycleManager.start(jsonProviders);
        started = true;
    }

    @Override
    public void stop() {
        lifecycleManager.stop(jsonProviders);
        started = false;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    private JsonFactory createJsonFactory() {
        ObjectMapper objectMapper = new ObjectMapper()
                /*
                 * Assume empty beans are ok.
                 */
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        if (findAndRegisterJacksonModules) {
            try {
                objectMapper.findAndRegisterModules();
            } catch (ServiceConfigurationError serviceConfigurationError) {
                addError("Error occurred while dynamically loading jackson modules", serviceConfigurationError);
            }
        }

        JsonFactory jsonFactory = objectMapper
                .getFactory()
                /*
                 * When generators are flushed, don't flush the underlying outputStream.
                 *
                 * This allows some streaming optimizations when using an encoder.
                 *
                 * The encoder generally determines when the stream should be flushed
                 * by an 'immediateFlush' property.
                 *
                 * The 'immediateFlush' property of the encoder can be set to false
                 * when the appender performs the flushes at appropriate times
                 * (such as the end of a batch in the AbstractLogstashTcpSocketAppender).
                 */
                .disable(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM);

        return this.jsonFactoryDecorator.decorate(jsonFactory);
    }

    public byte[] writeEventAsBytes(Event event) throws IOException {
        ByteArrayBuilder outputStream = new ByteArrayBuilder(getBufferRecycler());

        try {
            writeEventToOutputStream(event, outputStream);
            outputStream.flush();
            return outputStream.toByteArray();
        } finally {
            outputStream.release();
        }
    }

    public void writeEventToOutputStream(Event event, OutputStream outputStream) throws IOException {
        try (JsonGenerator generator = createGenerator(outputStream)) {
            writeEventToGenerator(generator, event);
        }
        /*
         * Do not flush the outputStream.
         *
         * Allow something higher in the stack (e.g. the encoder/appender)
         * to determine appropriate times to flush.
         */
    }

    public String writeEventAsString(Event event) throws IOException {
        SegmentedStringWriter writer = new SegmentedStringWriter(getBufferRecycler());

        try (JsonGenerator generator = createGenerator(writer)) {
            writeEventToGenerator(generator, event);
            writer.flush();
            return writer.getAndClear();
        }
    }

    protected void writeEventToGenerator(JsonGenerator generator, Event event) throws IOException {
        if (!isStarted()) {
            throw new IllegalStateException("Encoding attempted before starting.");
        }
        generator.writeStartObject();
        jsonProviders.writeTo(generator, event);
        generator.writeEndObject();
        generator.flush();
    }

    protected void prepareForDeferredProcessing(Event event) {
        event.prepareForDeferredProcessing();
        jsonProviders.prepareForDeferredProcessing(event);
    }

    private JsonGenerator createGenerator(OutputStream outputStream) throws IOException {
        return this.jsonGeneratorDecorator.decorate(jsonFactory.createGenerator(outputStream, encoding));
    }

    private JsonGenerator createGenerator(Writer writer) throws IOException {
        return this.jsonGeneratorDecorator.decorate(jsonFactory.createGenerator(writer));
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

    public JsonFactory getJsonFactory() {
        return jsonFactory;
    }

    public JsonFactoryDecorator getJsonFactoryDecorator() {
        return jsonFactoryDecorator;
    }

    public void setJsonFactoryDecorator(JsonFactoryDecorator jsonFactoryDecorator) {
        this.jsonFactoryDecorator = jsonFactoryDecorator;
    }

    public JsonGeneratorDecorator getJsonGeneratorDecorator() {
        return jsonGeneratorDecorator;
    }

    public void setJsonGeneratorDecorator(JsonGeneratorDecorator jsonGeneratorDecorator) {
        this.jsonGeneratorDecorator = jsonGeneratorDecorator;
    }

    public JsonProviders<Event> getProviders() {
        return jsonProviders;
    }

    public String getEncoding() {
        return encoding.getJavaName();
    }

    public void setEncoding(String encodingName) {
        for (JsonEncoding encoding: JsonEncoding.values()) {
            if (encoding.getJavaName().equals(encodingName) || encoding.name().equals(encodingName)) {
                this.encoding = encoding;
                return;
            }
        }
        throw new IllegalArgumentException("Unknown encoding " + encodingName);
    }

    public void setProviders(JsonProviders<Event> jsonProviders) {
        this.jsonProviders = jsonProviders;
    }

    public boolean isFindAndRegisterJacksonModules() {
        return findAndRegisterJacksonModules;
    }

    public void setFindAndRegisterJacksonModules(boolean findAndRegisterJacksonModules) {
        this.findAndRegisterJacksonModules = findAndRegisterJacksonModules;
    }
}
