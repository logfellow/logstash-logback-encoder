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
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.spi.DeferredProcessingAware;

public abstract class CompositeJsonLayout<Event extends DeferredProcessingAware> extends LayoutBase<Event> {
    
    private boolean immediateFlush = true;
    
    private final CompositeJsonFormatter<Event> formatter;
    
    public CompositeJsonLayout() {
        super();
        this.formatter = createFormatter();
    }
    
    protected abstract CompositeJsonFormatter<Event> createFormatter();

    public String doLayout(Event event) {
        try {
            return formatter.writeEventAsString(event);
        } catch (IOException e) {
            addWarn("Error formatting logging event", e);
            return null;
        }
    }
    
    @Override
    public void start() {
        super.start();
        formatter.setContext(getContext());
        formatter.start();
    }
    
    @Override
    public void stop() {
        super.stop();
        formatter.stop();
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

}
