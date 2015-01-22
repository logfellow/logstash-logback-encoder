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
import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.DeferredProcessingAware;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public class JsonProviders<Event extends DeferredProcessingAware> {
    
    private final List<JsonProvider<Event>> jsonProviders = new ArrayList<JsonProvider<Event>>();

    public void start() {
        for (JsonProvider<Event> jsonProvider : jsonProviders) {
            jsonProvider.start();
        }
    }

    public void stop() {
        for (JsonProvider<Event> jsonProvider : jsonProviders) {
            jsonProvider.start();
        }
    }

    public void setContext(Context context) {
        for (JsonProvider<Event> jsonProvider : jsonProviders) {
            jsonProvider.setContext(context);
        }
    }
    
    public void addProvider(JsonProvider<Event> provider) {
        if (provider != null) {
            this.jsonProviders.add(provider);
        }
    }

    public void removeProvider(JsonProvider<Event> provider) {
        if (provider != null) {
            this.jsonProviders.remove(provider);
        }
    }

    public void writeTo(JsonGenerator generator, Event event) throws IOException {
        for (JsonProvider<Event> jsonProvider : jsonProviders) {
            jsonProvider.writeTo(generator, event);
        }
    }

    protected void prepareForDeferredProcessing(Event event) {
        for (JsonProvider<Event> jsonProvider : jsonProviders) {
            jsonProvider.prepareForDeferredProcessing(event);
        }
    }
    
    public void setJsonFactory(MappingJsonFactory jsonFactory) {
        for (JsonProvider<Event> jsonProvider : jsonProviders) {
            if (jsonProvider instanceof JsonFactoryAware) {
                ((JsonFactoryAware) jsonProvider).setJsonFactory(jsonFactory);
            }
        }
    }
    
    public List<JsonProvider<Event>> getProviders() {
        return new ArrayList<JsonProvider<Event>>(jsonProviders);
    }

}
