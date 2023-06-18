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

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.logstash.logback.composite.loggingevent.mdc.BooleanMdcEntryWriter;
import net.logstash.logback.composite.loggingevent.mdc.DoubleMdcEntryWriter;
import net.logstash.logback.composite.loggingevent.mdc.LongMdcEntryWriter;
import net.logstash.logback.fieldnames.LogstashFieldNames;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MdcJsonProviderTest {
    
    private MdcJsonProvider provider = new MdcJsonProvider();
    
    @Mock
    private JsonGenerator generator;
    
    @Mock
    private ILoggingEvent event;

    private Map<String, String> mdc;
    
    @BeforeEach
    public void setup() {
        mdc = new LinkedHashMap<String, String>();
        mdc.put("name1", "value1");
        mdc.put("name2", "value2");
        mdc.put("name3", "value3");
        when(event.getMDCPropertyMap()).thenReturn(mdc);
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
        provider.setFieldName("mdc");
        
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        inOrder.verify(generator).writeObjectFieldStart("mdc");
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
        fieldNames.setMdc("mdc");

        provider.setFieldNames(fieldNames);
        
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        inOrder.verify(generator).writeObjectFieldStart("mdc");
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
        
        provider.setIncludeMdcKeyNames(Collections.singletonList("name1"));
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
        
        provider.setExcludeMdcKeyNames(Collections.singletonList("name1"));
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
        provider.addMdcKeyFieldName("name1=alternateName1");

        provider.writeTo(generator, event);

        verify(generator).writeFieldName("alternateName1");
        verify(generator).writeObject("value1");
        verify(generator).writeFieldName("name2");
        verify(generator).writeObject("value2");
        verify(generator).writeFieldName("name3");
        verify(generator).writeObject("value3");
    }

    @Test
    public void testMdcEntryWriters() throws IOException {
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

        verify(generator).writeFieldName("long");
        verify(generator).writeNumber(4711L);
        verify(generator).writeFieldName("double");
        verify(generator).writeNumber(2.71828);
        verify(generator).writeFieldName("bool");
        verify(generator).writeBoolean(true);
        verify(generator).writeFieldName("string_bool");
        verify(generator).writeObject("trueblue");
        verify(generator).writeFieldName("string_hex");
        verify(generator).writeObject("0xBAD");
        verify(generator).writeFieldName("empty");
        verify(generator).writeObject("");
        verifyNoMoreInteractions(generator);
    }

    @Test
    public void testMdcEntryWritersExcludeKeyPattern() throws IOException {
        mdc = new LinkedHashMap<>();
        mdc.put("long", "1");
        mdc.put("skip_exact", "2");
        mdc.put("skip_prefix.key", "3");
        mdc.put("axe", "4");
        when(event.getMDCPropertyMap()).thenReturn(mdc);

        provider.addMdcEntryWriter(new LongMdcEntryWriter());
        provider.setMdcEntryWriterExcludeKeyPattern("(skip_prefix\\..*|skip_exact|.x.)");

        provider.writeTo(generator, event);

        verify(generator).writeFieldName("long");
        verify(generator).writeNumber(1L);
        verify(generator).writeFieldName("skip_exact");
        verify(generator).writeObject("2");
        verify(generator).writeFieldName("skip_prefix.key");
        verify(generator).writeObject("3");
        verify(generator).writeFieldName("axe");
        verify(generator).writeObject("4");
        verifyNoMoreInteractions(generator);
    }

}
