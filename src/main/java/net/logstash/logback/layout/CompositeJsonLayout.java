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

import net.logstash.logback.composite.CompositeJsonFormatter;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.decorate.JsonFactoryDecorator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.DeferredProcessingAware;

public abstract class CompositeJsonLayout<Event extends DeferredProcessingAware> extends LayoutBase<Event> {
    
    private boolean immediateFlush = true;
    
    private Layout<Event> prefix;
    private Layout<Event> suffix;
    
    private final CompositeJsonFormatter<Event> formatter;
    
    public CompositeJsonLayout() {
        super();
        this.formatter = createFormatter();
    }
    
    protected abstract CompositeJsonFormatter<Event> createFormatter();

    public String doLayout(Event event) {
        final String result;
        try {
            result = formatter.writeEventAsString(event);
        } catch (IOException e) {
            addWarn("Error formatting logging event", e);
            return null;
        }
        
        if (prefix == null && suffix == null) {
            return result;
        }
        
        String prefixResult = doLayoutWrapped(prefix, event);
        String suffixResult = doLayoutWrapped(suffix, event);
        
        int size = result.length()
                + prefixResult == null ? 0 : prefixResult.length()
                + suffixResult == null ? 0 : suffixResult.length();
        
        StringBuilder stringBuilder = new StringBuilder(size);
        if (prefixResult != null) {
            stringBuilder.append(prefixResult);
        }
        stringBuilder.append(result);
        if (suffixResult != null) {
            stringBuilder.append(suffixResult);
        }
        return stringBuilder.toString();
    }

    private String doLayoutWrapped(Layout<Event> wrapped, Event event) {
        return wrapped == null ? null : wrapped.doLayout(event);
    }
    
    @Override
    public void start() {
        super.start();
        formatter.setContext(getContext());
        formatter.start();
        startWrapped(prefix);
        startWrapped(suffix);
    }

    private void startWrapped(Layout<Event> wrapped) {
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

}
