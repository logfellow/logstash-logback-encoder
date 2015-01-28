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
package net.logstash.logback.encoder;

import java.io.IOException;
import java.io.OutputStream;

import net.logstash.logback.composite.CompositeJsonFormatter;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.decorate.JsonFactoryDecorator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;

import org.apache.commons.io.IOUtils;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.EncoderBase;
import ch.qos.logback.core.spi.DeferredProcessingAware;

public abstract class CompositeJsonEncoder<Event extends DeferredProcessingAware>
        extends EncoderBase<Event> {
    
    private boolean immediateFlush = true;
    
    private Encoder<Event> prefix;
    private Encoder<Event> suffix;
    
    private final CompositeJsonFormatter<Event> formatter;
    
    public CompositeJsonEncoder() {
        super();
        this.formatter = createFormatter();
    }
    
    protected abstract CompositeJsonFormatter<Event> createFormatter();
    
    @Override
    public void init(OutputStream os) throws IOException {
        
        initWrapped(prefix, os);
        super.init(os);
        initWrapped(suffix, os);
        
    }

    private void initWrapped(Encoder<Event> wrapped, OutputStream os) throws IOException {
        if (wrapped != null) {
            wrapped.init(os);
        }
    }

    @Override
    public void doEncode(Event event) throws IOException {
        
        doEncodeWrapped(prefix, event);
        
        formatter.writeEventToOutputStream(event, outputStream);
        
        doEncodeWrapped(suffix, event);
        
        IOUtils.write(CoreConstants.LINE_SEPARATOR, outputStream);
        
        if (immediateFlush) {
            outputStream.flush();
        }
        
    }

    private void doEncodeWrapped(Encoder<Event> wrapped, Event event) throws IOException {
        if (wrapped != null) {
            wrapped.doEncode(event);
        }
    }
    
    @Override
    public void start() {
        super.start();
        formatter.setContext(getContext());
        formatter.start();
        startWrapped(prefix);
        startWrapped(suffix);
    }

    private void startWrapped(Encoder<Event> wrapped) {
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
    
    private void stopWrapped(Encoder<Event> wrapped) {
        if (wrapped != null && !wrapped.isStarted()) {
            wrapped.stop();
        }
    }
    
    @Override
    public void close() throws IOException {
        closeWrapped(prefix);
        closeWrapped(suffix);
        IOUtils.write(CoreConstants.LINE_SEPARATOR, outputStream);
    }
    
    private void closeWrapped(Encoder<Event> wrapped) throws IOException {
        if (wrapped != null && !wrapped.isStarted()) {
            wrapped.close();
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
