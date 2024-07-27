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
import java.util.Objects;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * A {@link JsonProvider} that nests other providers within a subobject.
 * 
 * @param <Event> type of event ({@link ILoggingEvent} or {@link IAccessEvent}).
 */
public abstract class AbstractNestedJsonProvider<Event extends DeferredProcessingAware> extends AbstractFieldJsonProvider<Event> implements JsonFactoryAware {
    
    public static final String FIELD_NESTED = "nested";
    
    /**
     * The providers that are used to populate the output nested JSON object.
     */
    private JsonProviders<Event> jsonProviders = new JsonProviders<>();
    
    public AbstractNestedJsonProvider() {
        setFieldName(FIELD_NESTED);
    }
    
    @Override
    public void start() {
        getProviders().start();
        super.start();
    }
    
    @Override
    public void stop() {
        // stop components in reverse order they were started
        super.stop();
        getProviders().stop();
    }
    
    @Override
    public void writeTo(JsonGenerator generator, Event event) throws IOException {
        generator.writeFieldName(getFieldName());
        generator.writeStartObject();
        jsonProviders.writeTo(generator, event);
        generator.writeEndObject();
    }
    
    public JsonProviders<Event> getProviders() {
        return jsonProviders;
    }
    
    public void setProviders(JsonProviders<Event> jsonProviders) {
        this.jsonProviders = Objects.requireNonNull(jsonProviders);
    }

    @Override
    public void setJsonFactory(final JsonFactory jsonFactory) {
        getProviders().setJsonFactory(Objects.requireNonNull(jsonFactory));
    }
    
    @Override
    public void prepareForDeferredProcessing(Event event) {
        super.prepareForDeferredProcessing(event);
        getProviders().prepareForDeferredProcessing(event);
    }
}
