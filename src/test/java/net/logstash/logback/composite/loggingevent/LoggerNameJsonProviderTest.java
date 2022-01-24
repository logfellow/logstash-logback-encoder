/*
 * Copyright 2013-2021 the original author or authors.
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

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LoggerNameJsonProviderTest {
    
    private LoggerNameJsonProvider provider = new LoggerNameJsonProvider();
    
    @Mock
    private JsonGenerator generator;
    
    @Mock
    private ILoggingEvent event;
    
    @Test
    public void testFullName() throws IOException {
        when(event.getLoggerName()).thenReturn(getClass().getName());
        
        writeEvent();
        
        verify(generator).writeStringField(LoggerNameJsonProvider.FIELD_LOGGER_NAME, getClass().getName());
    }

    @Test
    public void testFieldName() throws IOException {
        provider.setFieldName("newFieldName");
        
        when(event.getLoggerName()).thenReturn(getClass().getName());
        
        writeEvent();
        
        verify(generator).writeStringField("newFieldName", getClass().getName());
    }

    @Test
    public void testFieldNames() throws IOException {
        LogstashFieldNames fieldNames = new LogstashFieldNames();
        fieldNames.setLogger("newFieldName");
        
        provider.setFieldNames(fieldNames);
        
        when(event.getLoggerName()).thenReturn(getClass().getName());
        
        writeEvent();
        
        verify(generator).writeStringField("newFieldName", getClass().getName());
    }

    @Test
    public void testShortName() throws IOException {
        provider.setShortenedLoggerNameLength(5);
        
        when(event.getLoggerName()).thenReturn(getClass().getName());
        
        writeEvent();
        
        verify(generator).writeStringField(LoggerNameJsonProvider.FIELD_LOGGER_NAME, "n.l.l.c.l.LoggerNameJsonProviderTest");
    }

    @Test
    public void testShortName_zeroLength() throws IOException {
        provider.setShortenedLoggerNameLength(0);
        
        when(event.getLoggerName()).thenReturn(getClass().getName());
        
        writeEvent();
        
        verify(generator).writeStringField(LoggerNameJsonProvider.FIELD_LOGGER_NAME, "LoggerNameJsonProviderTest");
    }
    
    protected void writeEvent() throws IOException {
        if (!provider.isStarted()) {
            provider.start();
        }
        
        provider.writeTo(generator, event);
    }
}
