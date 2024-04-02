/*
 * Copyright 2013-2023 the original author or authors.
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
package net.logstash.logback.composite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Contains a collection of {@link JsonProvider}s to be used to write
 * JSON output for an Event.
 * 
 * Subclasses will provide convenience methods for specific provider
 * implementations, so that they can easily be added via XML configuration.
 * 
 * Most methods on this class just delegate to the method of the same
 * name on each {@link JsonProvider}.
 *
 * @param <Event> type of event ({@link ILoggingEvent} or {@link IAccessEvent}).
 */
public class JsonProviders<Event extends DeferredProcessingAware> implements JsonFactoryAware {
    
    private final List<JsonProvider<Event>> jsonProviders = new ArrayList<>();
    
    public void start() {
        for (JsonProvider<Event> jsonProvider : jsonProviders) {
            jsonProvider.start();
        }
    }

    public void stop() {
        for (JsonProvider<Event> jsonProvider : jsonProviders) {
            jsonProvider.stop();
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
    
    @Override
    public void setJsonFactory(JsonFactory jsonFactory) {
        for (JsonProvider<Event> jsonProvider : jsonProviders) {
            if (jsonProvider instanceof JsonFactoryAware) {
                ((JsonFactoryAware) jsonProvider).setJsonFactory(jsonFactory);
            }
        }
    }
    
    public List<JsonProvider<Event>> getProviders() {
        return Collections.unmodifiableList(jsonProviders);
    }
    
    public void addContext(ContextJsonProvider<Event> provider) {
        addProvider(provider);
    }
    public void addGlobalCustomFields(GlobalCustomFieldsJsonProvider<Event> provider) {
        addProvider(provider);
    }
    public void addUuid(UuidJsonProvider<Event> provider) {
        addProvider(provider);
    }
    public void addVersion(LogstashVersionJsonProvider<Event> provider) {
        addProvider(provider);
    }
}
