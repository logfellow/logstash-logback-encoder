/*
 * Copyright 2013-2025 the original author or authors.
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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Objects;

import net.logstash.logback.composite.AbstractCompositeJsonFormatter;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.dataformat.DataFormatFactory;
import net.logstash.logback.decorate.CompositeJsonGeneratorDecorator;
import net.logstash.logback.decorate.CompositeMapperBuilderDecorator;
import net.logstash.logback.decorate.CompositeTokenStreamFactoryBuilderDecorator;
import net.logstash.logback.decorate.Decorator;
import net.logstash.logback.util.ReusableByteBuffer;
import net.logstash.logback.util.ThreadLocalReusableByteBuffer;

import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.EncoderBase;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import ch.qos.logback.core.spi.DeferredProcessingAware;

public abstract class CompositeJsonEncoder<Event extends DeferredProcessingAware>
        extends EncoderBase<Event> implements StreamingEncoder<Event> {
    private static final byte[] EMPTY_BYTES = new byte[0];

    /**
     * The minimum size of the byte buffer used when encoding events using {@link #encode(DeferredProcessingAware)}.
     *
     * <p>The buffer automatically grows above the {@code #minBufferSize} when needed to
     * accommodate with larger events. However, only the first {@code minBufferSize} bytes
     * will be reused by subsequent invocations. It is therefore strongly advised to set
     * the minimum size at least equal to the average size of the encoded events to reduce
     * unnecessary memory allocations and reduce pressure on the garbage collector.
     */
    private int minBufferSize = 1024;

    /**
     * Per-thread {@link ReusableByteBuffer} instance used when calling {@link #encode(DeferredProcessingAware)}
     */
    private ThreadLocalReusableByteBuffer threadLocalBuffer;
    
    private Encoder<Event> prefix;
    private Encoder<Event> suffix;

    private final AbstractCompositeJsonFormatter<Event> formatter;
    
    private String lineSeparator = System.lineSeparator();

    private byte[] lineSeparatorBytes;

    private Charset charset;

    public CompositeJsonEncoder() {
        super();
        this.formatter = Objects.requireNonNull(createFormatter());
    }

    protected abstract AbstractCompositeJsonFormatter<Event> createFormatter();

    @Override
    public void encode(Event event, OutputStream outputStream) throws IOException {
        if (!isStarted()) {
            throw new IllegalStateException("Encoder is not started");
        }
        
        encode(outputStream, event);
    }
    
    @Override
    public byte[] encode(Event event) {
        if (!isStarted()) {
            throw new IllegalStateException("Encoder is not started");
        }
        
        ReusableByteBuffer buffer = threadLocalBuffer.acquire();
        
        try {
            encode(buffer, event);
            return buffer.toByteArray();
            
        } catch (IOException e) {
            addWarn("Error encountered while encoding log event. Event: " + event, e);
            return EMPTY_BYTES;
            
        } finally {
            threadLocalBuffer.release();
        }
    }
    
    private void encode(OutputStream outputStream, Event event) throws IOException {
        encode(prefix, event, outputStream);
        formatter.writeEvent(event, outputStream);
        encode(suffix, event, outputStream);
        outputStream.write(lineSeparatorBytes);
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

        formatter.setContext(getContext());
        formatter.start();
        charset = Charset.forName(formatter.getEncoding());
        lineSeparatorBytes = this.lineSeparator == null
                ? EMPTY_BYTES
                : this.lineSeparator.getBytes(charset);
        startWrapped(prefix);
        startWrapped(suffix);
        
        this.threadLocalBuffer = new ThreadLocalReusableByteBuffer(minBufferSize);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void startWrapped(Encoder<Event> wrapped) {
        if (wrapped instanceof LayoutWrappingEncoder<Event> layoutWrappedEncoder) {
            /*
             * Convenience hack to ensure the same charset is used in most cases.
             *
             * The charset for other encoders must be configured
             * on the wrapped encoder configuration.
             */
            layoutWrappedEncoder.setCharset(charset);

            if (layoutWrappedEncoder.getLayout() instanceof PatternLayoutBase layout) {
                /*
                 * Don't ensure exception output (for ILoggingEvents)
                 * or line separation (for IAccessEvents)
                 */
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
            
            threadLocalBuffer = null;
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

    public String getDataFormat() {
        return formatter.getDataFormat();
    }

    public void setDataFormat(String dataFormat) {
        formatter.setDataFormat(dataFormat);
    }

    public DataFormatFactory getDataFormatFactory() {
        return formatter.getDataFormatFactory();
    }

    public void setDataFormatFactory(DataFormatFactory dataFormatFactory) {
        formatter.setDataFormatFactory(dataFormatFactory);
    }

    public void addDecorator(Decorator<?> decorator) {
        formatter.addDecorator(decorator);
    }

    public CompositeTokenStreamFactoryBuilderDecorator getTokenStreamFactoryBuilderDecorator() {
        return formatter.getTokenStreamFactoryBuilderDecorator();
    }

    public CompositeMapperBuilderDecorator getMapperBuilderDecorator() {
        return formatter.getMapperBuilderDecorator();
    }

    public CompositeJsonGeneratorDecorator getJsonGeneratorDecorator() {
        return formatter.getJsonGeneratorDecorator();
    }

    public String getEncoding() {
        return formatter.getEncoding();
    }

    /**
     * The character encoding to use (default = "{@code UTF-8}").
     * Must an encoding supported by {@link tools.jackson.core.JsonEncoding}
     * 
     * @param encodingName encoding name
     */
    public void setEncoding(String encodingName) {
        formatter.setEncoding(encodingName);
    }

    public void setFindAndRegisterJacksonModules(boolean findAndRegisterJacksonModules) {
        formatter.setFindAndRegisterJacksonModules(findAndRegisterJacksonModules);
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

    protected AbstractCompositeJsonFormatter<Event> getFormatter() {
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
