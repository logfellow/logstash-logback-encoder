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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import net.logstash.logback.composite.loggingevent.LoggerNameJsonProvider;
import net.logstash.logback.fieldnames.LogstashFieldNames;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ch.qos.logback.classic.spi.ILoggingEvent;

import com.fasterxml.jackson.core.JsonGenerator;

public class LogstashVersionJsonProviderTest {
    
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    
    private LogstashVersionJsonProvider<ILoggingEvent> provider = new LogstashVersionJsonProvider<ILoggingEvent>();
    
    @Mock
    private JsonGenerator generator;
    
    @Mock
    private ILoggingEvent event;
    
    @Test
    public void testVersionAsNumeric() throws IOException {
        
        provider.writeTo(generator, event);
        
        verify(generator).writeNumberField(LogstashVersionJsonProvider.FIELD_VERSION, LogstashVersionJsonProvider.DEFAULT_VERSION);
    }

    @Test
    public void testVersionAsString() throws IOException {
        provider.setWriteAsString(true);
        
        provider.writeTo(generator, event);
        
        verify(generator).writeStringField(LogstashVersionJsonProvider.FIELD_VERSION, Integer.toString(LogstashVersionJsonProvider.DEFAULT_VERSION));
    }

    @Test
    public void testNonDefaultVersionAsNumeric() throws IOException {
        provider.setVersion(800);
        
        provider.writeTo(generator, event);
        
        verify(generator).writeNumberField(LogstashVersionJsonProvider.FIELD_VERSION, 800);
    }

    @Test
    public void testNonDefaultVersionAsString() throws IOException {
        provider.setVersion(800);
        provider.setWriteAsString(true);
        
        provider.writeTo(generator, event);
        
        verify(generator).writeStringField(LogstashVersionJsonProvider.FIELD_VERSION, "800");
    }

    @Test
    public void testFieldName() throws IOException {
        provider.setFieldName("newFieldName");
        
        provider.writeTo(generator, event);
        
        verify(generator).writeNumberField("newFieldName", LogstashVersionJsonProvider.DEFAULT_VERSION);
    }

    @Test
    public void testFieldNames() throws IOException {
        LogstashFieldNames fieldNames = new LogstashFieldNames();
        fieldNames.setVersion("newFieldName");
        
        provider.setFieldNames(fieldNames);
        
        provider.writeTo(generator, event);
        
        verify(generator).writeNumberField("newFieldName", LogstashVersionJsonProvider.DEFAULT_VERSION);
    }

}
