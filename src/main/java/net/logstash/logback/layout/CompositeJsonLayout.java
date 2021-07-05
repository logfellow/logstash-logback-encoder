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
package net.logstash.logback.layout;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import net.logstash.logback.composite.CompositeJsonFormatter;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.decorate.JsonFactoryDecorator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;
import net.logstash.logback.encoder.CompositeJsonEncoder;
import net.logstash.logback.encoder.SeparatorParser;
import net.logstash.logback.util.ReusableByteBuffer;
import net.logstash.logback.util.ReusableByteBuffers;

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
     * Note that this default is different than the default of {@link CompositeJsonEncoder#lineSeparator}.
     * In a future major release, the default will likely change to be the same as {@link CompositeJsonEncoder#lineSeparator}.</p>
     */
    private String lineSeparator;

    /**
     * The minimum size of the byte buffer used when encoding events in logback versions 
     * greater than or equal to 1.2.0. The buffer is reused by subsequent invocations of
     * the encoder. 
     * 
     * <p>The buffer automatically grows above the {@code #minBufferSize} when needed to 
     * accommodate with larger events. However, only the first {@code minBufferSize} bytes 
     * will be reused by subsequent invocations. It is therefore strongly advised to set
     * the minimum size at least equal to the average size of the encoded events to reduce
     * unnecessary memory allocations and reduce pressure on the garbage collector.
     */
    private int minBufferSize = 1024;
    
    /**
     * Provides reusable byte buffers (initialized when layout is started)
     */
    private ReusableByteBuffers bufferPool;
    
    
    private final CompositeJsonFormatter<Event> formatter;
    
    public CompositeJsonLayout() {
        super();
        this.formatter = createFormatter();
    }
    
    protected abstract CompositeJsonFormatter<Event> createFormatter();

    @Override
    public String doLayout(Event event) {
        if (!isStarted()) {
            throw new IllegalStateException("Layout is not started");
        }
        
        ReusableByteBuffer buffer = this.bufferPool.getBuffer();
        try(OutputStreamWriter writer = new OutputStreamWriter(buffer)) {
            writeLayout(   prefix, writer, event);
            writeFormatter(        writer, event);
            writeLayout(   suffix, writer, event);
            
            if (lineSeparator!=null) {
                writer.write(lineSeparator);
            }
            writer.flush();

            return new String(buffer.toByteArray());
        }
        catch (IOException e) {
            addWarn("Error formatting logging event", e);
            return null;
        }
        finally {
            bufferPool.releaseBuffer(buffer);
        }
    }

    
    private void writeLayout(Layout<Event> wrapped, Writer writer, Event event) throws IOException {
        if (wrapped==null) {
            return;
        }
        
        String str = wrapped.doLayout(event);
        if (str!=null) {
            writer.write(str);
        }
    }
    
    private void writeFormatter(Writer writer, Event event) throws IOException {
        this.formatter.writeEventToWriter(event, writer);
    }

    
    @Override
    public void start() {
        super.start();
        this.bufferPool = new ReusableByteBuffers(this.minBufferSize);
        formatter.setContext(getContext());
        formatter.start();
        startWrapped(prefix);
        startWrapped(suffix);
    }

    private void startWrapped(Layout<Event> wrapped) {
        if (wrapped instanceof PatternLayoutBase) {
            /*
             * Don't ensure exception output (for ILoggingEvents)
             * or line separation (for IAccessEvents) 
             */
            PatternLayoutBase<Event> layout = (PatternLayoutBase<Event>) wrapped;
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
        super.stop();
        formatter.stop();
        stopWrapped(prefix);
        stopWrapped(suffix);
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
    
    public JsonFactoryDecorator getJsonFactoryDecorator() {
        return formatter.getJsonFactoryDecorator();
    }

    public void setJsonFactoryDecorator(JsonFactoryDecorator jsonFactoryDecorator) {
        formatter.setJsonFactoryDecorator(jsonFactoryDecorator);
    }

    public JsonGeneratorDecorator getJsonGeneratorDecorator() {
        return formatter.getJsonGeneratorDecorator();
    }

    public void setJsonGeneratorDecorator(JsonGeneratorDecorator jsonGeneratorDecorator) {
        formatter.setJsonGeneratorDecorator(jsonGeneratorDecorator);
    }

    public void setFindAndRegisterJacksonModules(boolean findAndRegisterJacksonModules) {
        formatter.setFindAndRegisterJacksonModules(findAndRegisterJacksonModules);
    }

    protected CompositeJsonFormatter<Event> getFormatter() {
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
     * <p>
     *
     * The following values have special meaning:
     * <ul>
     * <li><tt>null</tt> or empty string = no new line. (default)</li>
     * <li>"<tt>SYSTEM</tt>" = operating system new line.</li>
     * <li>"<tt>UNIX</tt>" = unix line ending (\n).</li>
     * <li>"<tt>WINDOWS</tt>" = windows line ending (\r\n).</li>
     * </ul>
     * <p>
     * Any other value will be used as given as the lineSeparator.
     */
    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = SeparatorParser.parseSeparator(lineSeparator);
    }

    public int getMinBufferSize() {
        return minBufferSize;
    }
    
    /**
     * The minimum size of the byte buffer used when encoding events in logback versions 
     * greater than or equal to 1.2.0. The buffer is reused by subsequent invocations of
     * the encoder. 
     * 
     * <p>The buffer automatically grows above the {@code #minBufferSize} when needed to 
     * accommodate with larger events. However, only the first {@code minBufferSize} bytes 
     * will be reused by subsequent invocations. It is therefore strongly advised to set
     * the minimum size at least equal to the average size of the encoded events to reduce
     * unnecessary memory allocations and reduce pressure on the garbage collector.
     * 
     * <p>Note: changes to the buffer size will not be taken into account after the encoder
     *          is started.
     */
    public void setMinBufferSize(int minBufferSize) {
        this.minBufferSize = minBufferSize;
    }
}
