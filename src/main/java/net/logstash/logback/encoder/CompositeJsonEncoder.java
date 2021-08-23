/*
 * Copyright 2013-2021 the original author or authors.
 *
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

import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.EncoderBase;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import net.logstash.logback.composite.CompositeJsonFormatter;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.decorate.JsonFactoryDecorator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;
import net.logstash.logback.util.ReusableByteBuffer;
import net.logstash.logback.util.ReusableByteBufferPool;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public abstract class CompositeJsonEncoder<Event extends DeferredProcessingAware>
        extends EncoderBase<Event> implements StreamingEncoder<Event> {
    private static final byte[] EMPTY_BYTES = new byte[0];

    /**
     * The minimum size of the byte buffer used when encoding events.
     *
     * <p>The buffer automatically grows above the {@code #minBufferSize} when needed to
     * accommodate with larger events. However, only the first {@code minBufferSize} bytes
     * will be reused by subsequent invocations. It is therefore strongly advised to set
     * the minimum size at least equal to the average size of the encoded events to reduce
     * unnecessary memory allocations and reduce pressure on the garbage collector.
     */
    private int minBufferSize = 1024;

    /**
     * Provides reusable byte buffers (initialized when the encoder is started).
     */
    private ReusableByteBufferPool bufferPool;

    private Encoder<Event> prefix;
    private Encoder<Event> suffix;

    private final CompositeJsonFormatter<Event> formatter;

    private String lineSeparator = System.lineSeparator();

    private byte[] lineSeparatorBytes;

    private Charset charset;

    public CompositeJsonEncoder() {
        super();
        this.formatter = createFormatter();
    }

    protected abstract CompositeJsonFormatter<Event> createFormatter();

    @Override
    public void encode(Event event, OutputStream outputStream) throws IOException {
        if (!isStarted()) {
            throw new IllegalStateException("Encoder is not started");
        }

        encode(prefix, event, outputStream);
        formatter.writeEventToOutputStream(event, outputStream);
        encode(suffix, event, outputStream);

        outputStream.write(lineSeparatorBytes);
    }

    @Override
    public byte[] encode(Event event) {
        if (!isStarted()) {
            throw new IllegalStateException("Encoder is not started");
        }

        ReusableByteBuffer buffer = bufferPool.acquire();
        try {
            encode(event, buffer);
            return buffer.toByteArray();
        } catch (IOException e) {
            addWarn("Error encountered while encoding log event. Event: " + event, e);
            return EMPTY_BYTES;
        } finally {
            bufferPool.release(buffer);
        }
    }

    private void encode(Encoder<Event> encoder, Event event, OutputStream outputStream) throws IOException {
        if (encoder != null) {
            byte[] data = encoder.encode(event);
            if (data != null) {
                outputStream.write(data);
            }
        }
    }

    @Override
    public void start() {
        if (isStarted()) {
            return;
        }

        super.start();
        this.bufferPool = new ReusableByteBufferPool(this.minBufferSize);
        formatter.setContext(getContext());
        formatter.start();
        charset = Charset.forName(formatter.getEncoding());
        lineSeparatorBytes = this.lineSeparator == null
                ? EMPTY_BYTES
                : this.lineSeparator.getBytes(charset);
        startWrapped(prefix);
        startWrapped(suffix);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void startWrapped(Encoder<Event> wrapped) {
        if (wrapped instanceof LayoutWrappingEncoder) {
            /*
             * Convenience hack to ensure the same charset is used in most cases.
             *
             * The charset for other encoders must be configured
             * on the wrapped encoder configuration.
             */
            LayoutWrappingEncoder<Event> layoutWrappedEncoder = (LayoutWrappingEncoder<Event>) wrapped;
            layoutWrappedEncoder.setCharset(charset);

            if (layoutWrappedEncoder.getLayout() instanceof PatternLayoutBase) {
                /*
                 * Don't ensure exception output (for ILoggingEvents)
                 * or line separation (for IAccessEvents)
                 */
                PatternLayoutBase layout = (PatternLayoutBase) layoutWrappedEncoder.getLayout();
                layout.setPostCompileProcessor(null);
                /*
                 * The pattern will be re-parsed during start.
                 * Needed so that the pattern is re-parsed without
                 * the postCompileProcessor.
                 */
                layout.start();
            }
        }

        if (wrapped != null && !wrapped.isStarted()) {
            wrapped.start();
        }
    }

    @Override
    public void stop() {
        if (isStarted()) {
            super.stop();
            formatter.stop();
            stopWrapped(prefix);
            stopWrapped(suffix);
        }
    }

    private void stopWrapped(Encoder<Event> wrapped) {
        if (wrapped != null && wrapped.isStarted()) {
            wrapped.stop();
        }
    }

    @Override
    public byte[] headerBytes() {
        return EMPTY_BYTES;
    }

    @Override
    public byte[] footerBytes() {
        return EMPTY_BYTES;
    }

    public JsonProviders<Event> getProviders() {
        return formatter.getProviders();
    }

    public void setProviders(JsonProviders<Event> jsonProviders) {
        formatter.setProviders(jsonProviders);
    }

    public JsonFactoryDecorator getJsonFactoryDecorator() {
        return formatter.getJsonFactoryDecorator();
    }

    public void setJsonFactoryDecorator(JsonFactoryDecorator jsonFactoryDecorator) {
        formatter.setJsonFactoryDecorator(jsonFactoryDecorator);
    }

    public JsonGeneratorDecorator getJsonGeneratorDecorator() {
        return formatter.getJsonGeneratorDecorator();
    }

    public String getEncoding() {
        return formatter.getEncoding();
    }

    /**
     * The character encoding to use (default = "{@code UTF-8}").
     * Must an encoding supported by {@link com.fasterxml.jackson.core.JsonEncoding}
     * 
     * @param encodingName encoding name
     */
    public void setEncoding(String encodingName) {
        formatter.setEncoding(encodingName);
    }

    public void setFindAndRegisterJacksonModules(boolean findAndRegisterJacksonModules) {
        formatter.setFindAndRegisterJacksonModules(findAndRegisterJacksonModules);
    }

    public void setJsonGeneratorDecorator(JsonGeneratorDecorator jsonGeneratorDecorator) {
        formatter.setJsonGeneratorDecorator(jsonGeneratorDecorator);
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    /**
     * Sets which lineSeparator to use between events.
     * <p>
     *
     * The following values have special meaning:
     * <ul>
     * <li>{@code null} or empty string = no new line.</li>
     * <li>"{@code SYSTEM}" = operating system new line (default).</li>
     * <li>"{@code UNIX}" = unix line ending ({@code \n}).</li>
     * <li>"{@code WINDOWS}" = windows line ending {@code \r\n}).</li>
     * </ul>
     * <p>
     * Any other value will be used as given as the lineSeparator.
     * 
     * @param lineSeparator the line separator
     */
    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = SeparatorParser.parseSeparator(lineSeparator);
    }

    public int getMinBufferSize() {
        return minBufferSize;
    }

    /**
     * The minimum size of the byte buffer used when encoding events.
     *
     * <p>The buffer automatically grows above the {@code #minBufferSize} when needed to
     * accommodate with larger events. However, only the first {@code minBufferSize} bytes
     * will be reused by subsequent invocations. It is therefore strongly advised to set
     * the minimum size at least equal to the average size of the encoded events to reduce
     * unnecessary memory allocations and reduce pressure on the garbage collector.
     *
     * <p>Note: changes to the buffer size will not be taken into account after the encoder
     *          is started.
     *
     * @param minBufferSize minimum size of the byte buffer (in bytes)
     */
    public void setMinBufferSize(int minBufferSize) {
        this.minBufferSize = minBufferSize;
    }

    protected CompositeJsonFormatter<Event> getFormatter() {
        return formatter;
    }

    public Encoder<Event> getPrefix() {
        return prefix;
    }
    public void setPrefix(Encoder<Event> prefix) {
        this.prefix = prefix;
    }

    public Encoder<Event> getSuffix() {
        return suffix;
    }
    public void setSuffix(Encoder<Event> suffix) {
        this.suffix = suffix;
    }
}
