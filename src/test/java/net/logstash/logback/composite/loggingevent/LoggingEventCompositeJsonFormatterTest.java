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
package net.logstash.logback.composite.loggingevent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import net.logstash.logback.argument.StructuredArguments;
import net.logstash.logback.composite.AbstractJsonProvider;
import net.logstash.logback.decorate.JsonGeneratorDecorator;
import net.logstash.logback.decorate.NullJsonGeneratorDecorator;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.ContextAware;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LoggingEventCompositeJsonFormatterTest {
    
    private LoggingEventCompositeJsonFormatter formatter = new LoggingEventCompositeJsonFormatter(mock(ContextAware.class));
    
    @Mock
    private ILoggingEvent event;

    public static class EmptyBean {
        
    }

    @Test
    public void testDoesNotFailOnEmptyBeans() throws IOException {
        when(event.getArgumentArray()).thenReturn(new Object[] {StructuredArguments.keyValue("empty", new EmptyBean())});
        ((LoggingEventJsonProviders) formatter.getProviders()).addArguments(new ArgumentsJsonProvider());
        formatter.start();
        
        /*
         * This should not throw an exception, since SerializationFeature.FAIL_ON_EMPTY_BEANS is disabled
         */
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            assertThatCode(() -> formatter.writeEvent(event, bos)).doesNotThrowAnyException();
        }
    }
    
    
    /*
     * JsonFormatter reused by subsequent invocations
     */
    @Test
    public void testReused() throws IOException {
        JsonGeneratorDecorator decorator = spy(new NullJsonGeneratorDecorator());
        formatter.setJsonGeneratorDecorator(decorator);
        formatter.start();
        
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            formatter.writeEvent(event, bos);
            formatter.writeEvent(event, bos);
            
            // Only once instance should have been created
            verify(decorator, times(1)).decorate(any(JsonGenerator.class));
        }
    }
    
    
    /*
     * JsonFormatter not reused after exception thrown by underlying OutputStream
     */
    @Test
    public void testNotReusedAfterIOException() throws IOException {
        JsonGeneratorDecorator decorator = spy(new NullJsonGeneratorDecorator());
        formatter.setJsonGeneratorDecorator(decorator);
        formatter.start();
        
        OutputStream bos = mock(OutputStream.class);
        doThrow(new IOException())
            .doCallRealMethod()
            .when(bos).write(any(byte[].class), any(int.class), any(int.class));
        
        assertThatThrownBy(() -> formatter.writeEvent(event, bos)).isInstanceOf(IOException.class);
        formatter.writeEvent(event, bos);
        
        // Two instances created because the first was discarded because of the exception
        verify(decorator, times(2)).decorate(any(JsonGenerator.class));
    }
    
    
    /*
     * JsonFormatter not reused after exception thrown while calling registered JsonProviders
     */
    @Test
    public void testNotReusedAfterEncoderException() throws IOException {
        JsonGeneratorDecorator decorator = spy(new NullJsonGeneratorDecorator());
        formatter.setJsonGeneratorDecorator(decorator);
        formatter.getProviders().addProvider(new AbstractJsonProvider<ILoggingEvent>() {
            @Override
            public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
                throw new IOException("Exception thrown by JsonProvider");
            }
        });
        formatter.start();
        
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            assertThatThrownBy(() -> formatter.writeEvent(event, bos)).isInstanceOf(IOException.class);
            assertThatThrownBy(() -> formatter.writeEvent(event, bos)).isInstanceOf(IOException.class);
        
            // Two instances created because the first was discarded because of the exception
            verify(decorator, times(2)).decorate(any(JsonGenerator.class));
        }
    }

    /*
     * When overriden, the writeObject method of the JsonGenerator passed to setJsonGeneratorDecorator is honored
     */
    @Test
    public void testOverriddenJsonGeneratorWriteObjectIsHonored() throws IOException {
        when(event.getArgumentArray()).thenReturn(new Object[] {StructuredArguments.keyValue("answer", 42)});
        formatter.setJsonGeneratorDecorator(generator -> new JsonGeneratorDelegate(generator) {
            @Override
            public void writeObject(final Object pojo) throws IOException {
                super.writeObject(String.format("is <%s>", pojo));
            }
        });
        ((LoggingEventJsonProviders) formatter.getProviders()).addArguments(new ArgumentsJsonProvider());
        formatter.start();

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            formatter.writeEvent(event, bos);
            assertThat(bos).hasToString("{\"answer\":\"is <42>\"}");
        }
    }
}
