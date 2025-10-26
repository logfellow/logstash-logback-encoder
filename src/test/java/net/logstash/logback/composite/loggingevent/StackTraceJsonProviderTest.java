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
package net.logstash.logback.composite.loggingevent;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import net.logstash.logback.fieldnames.LogstashFieldNames;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import org.assertj.core.util.Throwables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JsonGenerator;

@ExtendWith(MockitoExtension.class)
public class StackTraceJsonProviderTest {
    
    private final StackTraceJsonProvider provider = new StackTraceJsonProvider();
    
    @Mock
    private JsonGenerator generator;
    
    @Mock
    private ILoggingEvent event;
    
    @Mock
    private ThrowableHandlingConverter converter;
    
    @Mock
    private IThrowableProxy ThrowableProxy;
    
    @BeforeEach
    public void setup() {
        when(converter.convert(event)).thenReturn("stack");
        provider.setThrowableConverter(converter);
    }
    
    @Test
    public void testDefaultName() throws IOException {
        
        when(event.getThrowableProxy()).thenReturn(ThrowableProxy);
        
        provider.writeTo(generator, event);
        
        verify(generator).writeStringProperty(StackTraceJsonProvider.FIELD_STACK_TRACE, "stack");
    }

    @Test
    public void testFieldName() throws IOException {
        provider.setFieldName("newFieldName");
        
        when(event.getThrowableProxy()).thenReturn(ThrowableProxy);
        
        provider.writeTo(generator, event);
        
        verify(generator).writeStringProperty("newFieldName", "stack");
    }

    @Test
    public void testFieldNames() {
        LogstashFieldNames fieldNames = new LogstashFieldNames();
        fieldNames.setStackTrace("newFieldName");
        
        provider.setFieldNames(fieldNames);
        
        when(event.getThrowableProxy()).thenReturn(ThrowableProxy);
        
        provider.writeTo(generator, event);
        
        verify(generator).writeStringProperty("newFieldName", "stack");
    }

    @Test
    public void testWriteAsArray() {
        String stacktrace = Throwables.getStackTrace(new RuntimeException("testing exception handling"));
        when(converter.convert(event)).thenReturn(stacktrace);

        provider.setWriteAsArray(true);

        when(event.getThrowableProxy()).thenReturn(ThrowableProxy);

        provider.writeTo(generator, event);

        verify(generator).writeName("stack_trace");
        verify(generator).writeStartArray();
        verify(generator).writeString("java.lang.RuntimeException: testing exception handling");
        verify(generator, atLeastOnce()).writeString(anyString());
        verify(generator).writeEndArray();
    }

}
