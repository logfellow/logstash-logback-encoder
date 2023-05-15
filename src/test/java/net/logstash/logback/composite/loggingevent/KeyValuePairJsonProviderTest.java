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

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.fieldnames.LogstashFieldNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.KeyValuePair;

import java.io.IOException;
import java.util.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KeyValuePairJsonProviderTest {
    
    private KeyValuePairJsonProvider provider = new KeyValuePairJsonProvider();
    
    @Mock
    private JsonGenerator generator;
    
    @Mock
    private ILoggingEvent event;

    private List<KeyValuePair> kvp;
    
    @BeforeEach
    public void setup() {
        kvp = new ArrayList<>();
        kvp.add(new KeyValuePair("name1", "value1"));
        kvp.add(new KeyValuePair("name2", "value2"));
        kvp.add(new KeyValuePair("name3", "value3"));
        when(event.getKeyValuePairs()).thenReturn(kvp);
    }
    
    @Test
    public void testUnwrapped() throws IOException {
        
        provider.writeTo(generator, event);
        
        verify(generator).writeFieldName("name1");
        verify(generator).writeObject("value1");
        verify(generator).writeFieldName("name2");
        verify(generator).writeObject("value2");
        verify(generator).writeFieldName("name3");
        verify(generator).writeObject("value3");
    }

    @Test
    public void testWrapped() throws IOException {
        provider.setFieldName("kvp");
        
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        inOrder.verify(generator).writeObjectFieldStart("kvp");
        inOrder.verify(generator).writeFieldName("name1");
        inOrder.verify(generator).writeObject("value1");
        inOrder.verify(generator).writeFieldName("name2");
        inOrder.verify(generator).writeObject("value2");
        inOrder.verify(generator).writeFieldName("name3");
        inOrder.verify(generator).writeObject("value3");
        inOrder.verify(generator).writeEndObject();
    }

    @Test
    public void testWrappedUsingFieldNames() throws IOException {
        LogstashFieldNames fieldNames = new LogstashFieldNames();
        fieldNames.setKeyValuePair("kvp");

        provider.setFieldNames(fieldNames);
        
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        inOrder.verify(generator).writeObjectFieldStart("kvp");
        inOrder.verify(generator).writeFieldName("name1");
        inOrder.verify(generator).writeObject("value1");
        inOrder.verify(generator).writeFieldName("name2");
        inOrder.verify(generator).writeObject("value2");
        inOrder.verify(generator).writeFieldName("name3");
        inOrder.verify(generator).writeObject("value3");
        inOrder.verify(generator).writeEndObject();
    }

    @Test
    public void testInclude() throws IOException {
        
        provider.setIncludeKvpKeyNames(Collections.singletonList("name1"));
        provider.writeTo(generator, event);
        
        verify(generator).writeFieldName("name1");
        verify(generator).writeObject("value1");
        verify(generator, never()).writeFieldName("name2");
        verify(generator, never()).writeObject("value2");
        verify(generator, never()).writeFieldName("name3");
        verify(generator, never()).writeObject("value3");
    }

    @Test
    public void testExclude() throws IOException {
        
        provider.setExcludeKvpKeyNames(Collections.singletonList("name1"));
        provider.writeTo(generator, event);
        
        verify(generator, never()).writeFieldName("name1");
        verify(generator, never()).writeObject("value1");
        verify(generator).writeFieldName("name2");
        verify(generator).writeObject("value2");
        verify(generator).writeFieldName("name3");
        verify(generator).writeObject("value3");
    }

    @Test
    public void testAlternateFieldName() throws IOException {
        provider.addKvpKeyFieldName("name1=alternateName1");

        provider.writeTo(generator, event);

        verify(generator).writeFieldName("alternateName1");
        verify(generator).writeObject("value1");
        verify(generator).writeFieldName("name2");
        verify(generator).writeObject("value2");
        verify(generator).writeFieldName("name3");
        verify(generator).writeObject("value3");
    }

}
