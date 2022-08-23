/*
 * Copyright 2013-2022 the original author or authors.
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import net.logstash.logback.util.LogbackUtils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Outputs an incrementing sequence number.
 * Useful for determining if log events get lost along the transport chain.
 * 
 * <p>With Logback 1.3 the sequence number is obtained from {@link ILoggingEvent#getSequenceNumber()} provided the
 * LoggerContext is configured with a {@link SequenceNumberGenerator} (which is not by default).
 * If no SequenceNumberGenerator is configured, the provider issues a warning and reverts to a locally generated
 * incrementing number.
 *
 * <p>With Logback versions prior to 1.3 the sequence number is generated locally by the provider itself.
 * 
 * <p>If needed, a different strategy can be used by setting a custom provider with {@link #setSequenceProvider(Function)}.
 */
public abstract class AbstractSequenceJsonProvider<Event extends DeferredProcessingAware> extends AbstractFieldJsonProvider<Event> {

    /**
     * Default JSON field name
     */
    public static final String FIELD_SEQUENCE = "sequence";

    /**
     * The function used to get the sequence number associated with the supplied event.
     * May be {@code null} until the provider is started.
     * If none is explicitly assigned through {@link #setSequenceProvider(Function)} one is automatically deduced from the Logback version
     * found on the classpath. 
     */
    private Function<Event, Long> sequenceProvider;
    
    
    public AbstractSequenceJsonProvider() {
        setFieldName(FIELD_SEQUENCE);
    }
    
    
    @Override
    public void start() {
        if (getContext() == null) {
            throw new IllegalStateException("No context given to " + this.getClass().getName());
        }
        if (this.sequenceProvider == null) {
            this.sequenceProvider = createSequenceProvider();
        }
        
        super.start();
    }
    

    @Override
    public void writeTo(JsonGenerator generator, Event event) throws IOException {
        if (!isStarted()) {
            throw new IllegalStateException("Provider " + this.getClass().getName() + " is  not started");
        }
        JsonWritingUtils.writeNumberField(generator, getFieldName(), sequenceProvider.apply(event));
    }

    /**
     * Assign a custom sequence provider instead of relying on the default.
     * 
     * @param sequenceProvider the sequence provider to use to retrieve the sequence number for the event
     */
    public void setSequenceProvider(Function<Event, Long> sequenceProvider) {
        this.sequenceProvider = Objects.requireNonNull(sequenceProvider);
    }
    
    /**
     * Get the sequence provider used to get the sequence number associated with the supplied event.
     *  
     * @return a sequence provider
     */
    public Function<Event, Long> getSequenceProvider() {
        return sequenceProvider;
    }

    
    /**
     * Create a default sequence provider depending on the current Logback version.
     * 
     * <p>With Logback 1.3 the sequence number is obtained for {@link ILoggingEvent#getSequenceNumber()} provided the
     * LoggerContext is configured with a {@link SequenceNumberGenerator} (which is not by default).
     * If no SequenceNumberGenerator is configured, the provider issues a warning and reverts to a locally generated
     * incrementing number.
     *
     * <p>With Logback versions prior to 1.3 the sequence number is generated locally by the provider itself.
     * 
     * @return a sequence provider
     */
    protected Function<Event, Long> createSequenceProvider() {
        if (LogbackUtils.isVersion13()) {
            if (getContext() == null || getContext().getSequenceNumberGenerator() == null) {
                this.addWarn("No <sequenceNumberGenerator> defined in Logback configuration - revert to using a local incrementing sequence number.");
            }
            else {
                return createNativeSequenceNumberFieldAccessor();
            }
        }
        return new Function<Event, Long>() {
            private final AtomicLong sequence = new AtomicLong(0L);
            
            @Override
            public Long apply(Event t) {
                return sequence.incrementAndGet();
            }
        };
    }
    

    /**
     * Get a function used to access the sequenceNumber field of the supplied event.
     * 
     * @return a function used to access the sequenceNumber field of the supplied event.
     */
    protected abstract Function<Event, Long> createNativeSequenceNumberFieldAccessor();
}
