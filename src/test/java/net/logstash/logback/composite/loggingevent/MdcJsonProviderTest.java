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

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.logstash.logback.composite.loggingevent.mdc.BooleanMdcEntryWriter;
import net.logstash.logback.composite.loggingevent.mdc.DoubleMdcEntryWriter;
import net.logstash.logback.composite.loggingevent.mdc.LongMdcEntryWriter;
import net.logstash.logback.fieldnames.LogstashFieldNames;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JsonGenerator;

@ExtendWith(MockitoExtension.class)
public class MdcJsonProviderTest {
    
    private final MdcJsonProvider provider = new MdcJsonProvider();
    
    @Mock
    private JsonGenerator generator;
    
    @Mock
    private ILoggingEvent event;

    private Map<String, String> mdc;
    
    @BeforeEach
    public void setup() {
        mdc = new LinkedHashMap<>();
        mdc.put("name1", "value1");
        mdc.put("name2", "value2");
        mdc.put("name3", "value3");
        when(event.getMDCPropertyMap()).thenReturn(mdc);
    }
    
    @Test
    public void testUnwrapped() {
        
        provider.writeTo(generator, event);
        
        verify(generator).writePOJOProperty("name1", "value1");
        verify(generator).writePOJOProperty("name2", "value2");
        verify(generator).writePOJOProperty("name3", "value3");
    }

    @Test
    public void testWrapped() {
        provider.setFieldName("mdc");
        
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        inOrder.verify(generator).writeObjectPropertyStart("mdc");
        inOrder.verify(generator).writePOJOProperty("name1", "value1");
        inOrder.verify(generator).writePOJOProperty("name2", "value2");
        inOrder.verify(generator).writePOJOProperty("name3", "value3");
        inOrder.verify(generator).writeEndObject();
    }

    @Test
    public void testWrappedUsingFieldNames() {
        LogstashFieldNames fieldNames = new LogstashFieldNames();
        fieldNames.setMdc("mdc");

        provider.setFieldNames(fieldNames);
        
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        inOrder.verify(generator).writeObjectPropertyStart("mdc");
        inOrder.verify(generator).writePOJOProperty("name1", "value1");
        inOrder.verify(generator).writePOJOProperty("name2", "value2");
        inOrder.verify(generator).writePOJOProperty("name3", "value3");
        inOrder.verify(generator).writeEndObject();
    }

    @Test
    public void testInclude() {
        
        provider.setIncludeMdcKeyNames(Collections.singletonList("name1"));
        provider.writeTo(generator, event);
        
        verify(generator).writePOJOProperty("name1", "value1");
        verify(generator, never()).writePOJOProperty("name2", "value2");
        verify(generator, never()).writePOJOProperty("name3", "value3");
    }

    @Test
    public void testExclude() {
        
        provider.setExcludeMdcKeyNames(Collections.singletonList("name1"));
        provider.writeTo(generator, event);
        
        verify(generator, never()).writePOJOProperty("name1", "value1");
        verify(generator).writePOJOProperty("name2", "value2");
        verify(generator).writePOJOProperty("name3", "value3");
    }

    @Test
    public void testAlternateFieldName() {
        provider.addMdcKeyFieldName("name1=alternateName1");

        provider.writeTo(generator, event);

        verify(generator).writePOJOProperty("alternateName1", "value1");
        verify(generator).writePOJOProperty("name2", "value2");
        verify(generator).writePOJOProperty("name3", "value3");
    }

    @Test
    public void testMdcEntryWriters() {
        mdc = new LinkedHashMap<>();
        mdc.put("long", "4711");
        mdc.put("double", "2.71828");
        mdc.put("bool", "true");
        mdc.put("string_bool", "trueblue");
        mdc.put("string_hex", "0xBAD");
        mdc.put("empty", "");
        mdc.put("key_null", null); // not logged
        when(event.getMDCPropertyMap()).thenReturn(mdc);

        provider.addMdcEntryWriter(new LongMdcEntryWriter());
        provider.addMdcEntryWriter(new DoubleMdcEntryWriter());
        provider.addMdcEntryWriter(new BooleanMdcEntryWriter());

        provider.writeTo(generator, event);

        verify(generator).writeName("long");
        verify(generator).writeNumber(4711L);
        verify(generator).writeName("double");
        verify(generator).writeNumber(2.71828);
        verify(generator).writeName("bool");
        verify(generator).writeBoolean(true);
        verify(generator).writeName("string_bool");
        verify(generator).writePOJO("trueblue");
        verify(generator).writeName("string_hex");
        verify(generator).writePOJO("0xBAD");
        verify(generator).writeName("empty");
        verify(generator).writePOJO("");
        verifyNoMoreInteractions(generator);
    }

}
