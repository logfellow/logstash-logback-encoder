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
package net.logstash.logback.composite.loggingevent;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import net.logstash.logback.fieldnames.LogstashFieldNames;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StackTraceJsonProviderTest {
    
    private StackTraceJsonProvider provider = new StackTraceJsonProvider();
    
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
        
        verify(generator).writeStringField(StackTraceJsonProvider.FIELD_STACK_TRACE, "stack");
    }

    @Test
    public void testFieldName() throws IOException {
        provider.setFieldName("newFieldName");
        
        when(event.getThrowableProxy()).thenReturn(ThrowableProxy);
        
        provider.writeTo(generator, event);
        
        verify(generator).writeStringField("newFieldName", "stack");
    }

    @Test
    public void testFieldNames() throws IOException {
        LogstashFieldNames fieldNames = new LogstashFieldNames();
        fieldNames.setStackTrace("newFieldName");
        
        provider.setFieldNames(fieldNames);
        
        when(event.getThrowableProxy()).thenReturn(ThrowableProxy);
        
        provider.writeTo(generator, event);
        
        verify(generator).writeStringField("newFieldName", "stack");
    }

}
