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

import java.io.IOException;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.fieldnames.LogstashFieldNames;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonGenerator;

@ExtendWith(MockitoExtension.class)
public class LogstashVersionJsonProviderTest {
    
    private LogstashVersionJsonProvider<ILoggingEvent> provider = new LogstashVersionJsonProvider<ILoggingEvent>();
    
    @Mock
    private JsonGenerator generator;
    
    @Mock
    private ILoggingEvent event;
    
    @Test
    public void testVersionAsNumeric() throws IOException {
        provider.setWriteAsInteger(true);

        provider.writeTo(generator, event);
        
        verify(generator).writeNumberField(LogstashVersionJsonProvider.FIELD_VERSION, Long.parseLong(LogstashVersionJsonProvider.DEFAULT_VERSION));
    }

    @Test
    public void testVersionAsString() throws IOException {
        provider.writeTo(generator, event);
        
        verify(generator).writeStringField(LogstashVersionJsonProvider.FIELD_VERSION, LogstashVersionJsonProvider.DEFAULT_VERSION);
    }

    @Test
    public void testNonDefaultVersionAsNumeric() throws IOException {
        provider.setVersion("800");
        provider.setWriteAsInteger(true);
        
        provider.writeTo(generator, event);
        
        verify(generator).writeNumberField(LogstashVersionJsonProvider.FIELD_VERSION, 800L);
    }

    @Test
    public void testNonDefaultVersionAsString() throws IOException {
        provider.setVersion("800");
        
        provider.writeTo(generator, event);
        
        verify(generator).writeStringField(LogstashVersionJsonProvider.FIELD_VERSION, "800");
    }

    @Test
    public void testFieldName() throws IOException {
        provider.setFieldName("newFieldName");
        
        provider.writeTo(generator, event);
        
        verify(generator).writeStringField("newFieldName", LogstashVersionJsonProvider.DEFAULT_VERSION);
    }

    @Test
    public void testFieldNames() throws IOException {
        LogstashFieldNames fieldNames = new LogstashFieldNames();
        fieldNames.setVersion("newFieldName");
        
        provider.setFieldNames(fieldNames);
        
        provider.writeTo(generator, event);
        
        verify(generator).writeStringField("newFieldName", LogstashVersionJsonProvider.DEFAULT_VERSION);
    }

}
