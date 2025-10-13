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
package net.logstash.logback.composite;

import static org.mockito.Mockito.verify;

import net.logstash.logback.fieldnames.LogstashFieldNames;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JsonGenerator;

@ExtendWith(MockitoExtension.class)
public class LogstashVersionJsonProviderTest {
    
    private final LogstashVersionJsonProvider<ILoggingEvent> provider = new LogstashVersionJsonProvider<>();
    
    @Mock
    private JsonGenerator generator;
    
    @Mock
    private ILoggingEvent event;
    
    @Test
    public void testVersionAsNumeric() {
        provider.setWriteAsInteger(true);

        provider.writeTo(generator, event);
        
        verify(generator).writeNumberProperty(LogstashVersionJsonProvider.FIELD_VERSION, Long.parseLong(LogstashVersionJsonProvider.DEFAULT_VERSION));
    }

    @Test
    public void testVersionAsString() {
        provider.writeTo(generator, event);
        
        verify(generator).writeStringProperty(LogstashVersionJsonProvider.FIELD_VERSION, LogstashVersionJsonProvider.DEFAULT_VERSION);
    }

    @Test
    public void testNonDefaultVersionAsNumeric() {
        provider.setVersion("800");
        provider.setWriteAsInteger(true);
        
        provider.writeTo(generator, event);
        
        verify(generator).writeNumberProperty(LogstashVersionJsonProvider.FIELD_VERSION, 800L);
    }

    @Test
    public void testNonDefaultVersionAsString() {
        provider.setVersion("800");
        
        provider.writeTo(generator, event);
        
        verify(generator).writeStringProperty(LogstashVersionJsonProvider.FIELD_VERSION, "800");
    }

    @Test
    public void testFieldName() {
        provider.setFieldName("newFieldName");
        
        provider.writeTo(generator, event);
        
        verify(generator).writeStringProperty("newFieldName", LogstashVersionJsonProvider.DEFAULT_VERSION);
    }

    @Test
    public void testFieldNames() {
        LogstashFieldNames fieldNames = new LogstashFieldNames();
        fieldNames.setVersion("newFieldName");
        
        provider.setFieldNames(fieldNames);
        
        provider.writeTo(generator, event);
        
        verify(generator).writeStringProperty("newFieldName", LogstashVersionJsonProvider.DEFAULT_VERSION);
    }

}
