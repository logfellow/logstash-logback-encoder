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
package net.logstash.logback.layout;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Objects;

import net.logstash.logback.composite.AbstractCompositeJsonFormatter;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.dataformat.DataFormatFactory;
import net.logstash.logback.decorate.CompositeJsonGeneratorDecorator;
import net.logstash.logback.decorate.CompositeMapperBuilderDecorator;
import net.logstash.logback.decorate.CompositeTokenStreamFactoryBuilderDecorator;
import net.logstash.logback.decorate.Decorator;
import net.logstash.logback.encoder.CompositeJsonEncoder;
import net.logstash.logback.encoder.SeparatorParser;
import net.logstash.logback.util.ReusableByteBuffer;
import net.logstash.logback.util.ThreadLocalReusableByteBuffer;

import ch.qos.logback.core.Layout;
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import ch.qos.logback.core.spi.DeferredProcessingAware;


public abstract class CompositeJsonLayout<Event extends DeferredProcessingAware> extends LayoutBase<Event> {

    private boolean immediateFlush = true;

    private Layout<Event> prefix;
    private Layout<Event> suffix;

    /**
     * Separator to use between events.
     *
     * <p>By default, this is null (for backwards compatibility), indicating no separator.
     * Note that this default is different from the default of {@link CompositeJsonEncoder#lineSeparator}.
     * In a future major release, the default will likely change to be the same as {@link CompositeJsonEncoder#lineSeparator}.</p>
     */
    private String lineSeparator;

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
     * Per-thread {@link ReusableByteBuffer}
     */
    private ThreadLocalReusableByteBuffer threadLocalBuffer;
    
    private final AbstractCompositeJsonFormatter<Event> formatter;
    
    public CompositeJsonLayout() {
        super();
        this.formatter = Objects.requireNonNull(createFormatter());
    }

    protected abstract AbstractCompositeJsonFormatter<Event> createFormatter();

    @Override
    public String doLayout(Event event) {
        if (!isStarted()) {
            throw new IllegalStateException("Layout is not started");
        }
        
        ReusableByteBuffer buffer = threadLocalBuffer.acquire();
        try {
            writeEvent(buffer, event);
            return new String(buffer.toByteArray());
        
        } catch (IOException e) {
            addWarn("Error formatting logging event", e);
            return null;
            
        } finally {
            threadLocalBuffer.release();
        }
    }

    private void writeEvent(OutputStream outputStream, Event event) throws IOException {
        try (Writer writer = new OutputStreamWriter(outputStream)) {
            writeLayout(prefix, writer, event);
            formatter.writeEvent(event, outputStream);
            writeLayout(suffix, writer, event);
           
            if (lineSeparator != null) {
                writer.write(lineSeparator);
            }
            writer.flush();
        }
    }
    
    private void writeLayout(Layout<Event> wrapped, Writer writer, Event event) throws IOException {
        if (wrapped == null) {
            return;
        }
        
        String str = wrapped.doLayout(event);
        if (str != null) {
            writer.write(str);
            writer.flush();
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
        
        startWrapped(prefix);
        startWrapped(suffix);
        
        this.threadLocalBuffer = new ThreadLocalReusableByteBuffer(minBufferSize);
    }

    private void startWrapped(Layout<Event> wrapped) {
        if (wrapped instanceof PatternLayoutBase<Event> layout) {
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
        if (wrapped != null && !wrapped.isStarted()) {
            wrapped.start();
        }
    }

    @Override
    public void stop() {
        if (!isStarted()) {
            return;
        }
        
        super.stop();
        formatter.stop();
        stopWrapped(prefix);
        stopWrapped(suffix);
        
        this.threadLocalBuffer = null;
    }

    private void stopWrapped(Layout<Event> wrapped) {
        if (wrapped != null && !wrapped.isStarted()) {
            wrapped.stop();
        }
    }

    public JsonProviders<Event> getProviders() {
        return formatter.getProviders();
    }

    public void setProviders(JsonProviders<Event> jsonProviders) {
        formatter.setProviders(jsonProviders);
    }

    public boolean isImmediateFlush() {
        return immediateFlush;
    }

    public void setImmediateFlush(boolean immediateFlush) {
        this.immediateFlush = immediateFlush;
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

    public void setFindAndRegisterJacksonModules(boolean findAndRegisterJacksonModules) {
        formatter.setFindAndRegisterJacksonModules(findAndRegisterJacksonModules);
    }

    protected AbstractCompositeJsonFormatter<Event> getFormatter() {
        return formatter;
    }

    public Layout<Event> getPrefix() {
        return prefix;
    }
    public void setPrefix(Layout<Event> prefix) {
        this.prefix = prefix;
    }

    public Layout<Event> getSuffix() {
        return suffix;
    }
    public void setSuffix(Layout<Event> suffix) {
        this.suffix = suffix;
    }
    public String getLineSeparator() {
        return lineSeparator;
    }

    /**
     * Sets which lineSeparator to use between events.
     *
     * The following values have special meaning:
     * <ul>
     * <li>{@code null} or empty string = no new line. (default)</li>
     * <li>"{@code SYSTEM}" = operating system new line.</li>
     * <li>"{@code UNIX}" = unix line ending ({@code \n}).</li>
     * <li>"{@code WINDOWS}" = windows line ending ({@code \r\n}).</li>
     * </ul>
     *
     * Any other value will be used as given as the lineSeparator.
     * 
     * @param lineSeparator the separator format
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
     * @param minBufferSize the minimum buffer size (in bytes)
     */
    public void setMinBufferSize(int minBufferSize) {
        this.minBufferSize = minBufferSize;
    }
}
