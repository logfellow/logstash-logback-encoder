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

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.fieldnames.LogstashFieldNames;
import net.logstash.logback.marker.LogstashMarker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Marker;

import com.fasterxml.jackson.core.JsonGenerator;

@ExtendWith(MockitoExtension.class)
public class TagsJsonProviderTest {
    
    private TagsJsonProvider provider = new TagsJsonProvider();
    
    @Mock
    private JsonGenerator generator;
    
    @Mock
    private ILoggingEvent event;
    
    @Mock
    private Marker marker1;
    
    @Mock
    private LogstashMarker marker2;
    
    @Mock
    private Marker marker3;
    
    @BeforeEach
    public void setup() {
        when(marker1.hasReferences()).thenReturn(true);
        when(marker1.iterator()).thenReturn(Collections.<Marker>singleton(marker2).iterator());
        
        when(marker2.hasReferences()).thenReturn(true);
        when(marker2.iterator()).thenReturn(Collections.singleton(marker3).iterator());

        when(marker1.getName()).thenReturn("marker1");
        when(marker3.getName()).thenReturn("marker3");
        
        when(event.getMarker()).thenReturn(marker1);
        
    }
    
    @Test
    public void testDefaultName() throws IOException {
        
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        
        inOrder.verify(generator).writeArrayFieldStart(TagsJsonProvider.FIELD_TAGS);
        inOrder.verify(generator).writeString("marker1");
        inOrder.verify(generator).writeString("marker3");
        inOrder.verify(generator).writeEndArray();
    }

    @Test
    public void testFieldName() throws IOException {
        provider.setFieldName("newFieldName");
        
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        
        inOrder.verify(generator).writeArrayFieldStart("newFieldName");
        inOrder.verify(generator).writeString("marker1");
        inOrder.verify(generator).writeString("marker3");
        inOrder.verify(generator).writeEndArray();
    }

    @Test
    public void testFieldNames() throws IOException {
        LogstashFieldNames fieldNames = new LogstashFieldNames();
        fieldNames.setTags("newFieldName");
        provider.setFieldNames(fieldNames);
        
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        
        inOrder.verify(generator).writeArrayFieldStart("newFieldName");
        inOrder.verify(generator).writeString("marker1");
        inOrder.verify(generator).writeString("marker3");
        inOrder.verify(generator).writeEndArray();
    }

}
