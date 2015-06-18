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
package net.logstash.logback.composite.loggingevent;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;

import net.logstash.logback.fieldnames.LogstashFieldNames;
import net.logstash.logback.marker.LogstashMarker;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.slf4j.Marker;

import ch.qos.logback.classic.spi.ILoggingEvent;

import com.fasterxml.jackson.core.JsonGenerator;

public class TagsJsonProviderTest {
    
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    
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
    
    @Mock
    private Marker marker4;
    
    @Before
    public void setup() {
        when(marker1.hasReferences()).thenReturn(true);
        when(marker1.iterator()).thenReturn(Collections.<Marker>singleton(marker2).iterator());
        
        when(marker2.hasReferences()).thenReturn(true);
        when(marker2.iterator()).thenReturn(Collections.singleton(marker3).iterator());
        
        when(marker3.hasReferences()).thenReturn(true);
        when(marker3.iterator()).thenReturn(Collections.singleton(marker4).iterator());
        
        
        when(marker1.getName()).thenReturn("marker1");
        when(marker2.getName()).thenReturn("marker2");
        when(marker3.getName()).thenReturn(JsonMessageJsonProvider.JSON_MARKER_NAME);
        when(marker4.getName()).thenReturn("marker4");
        
        when(event.getMarker()).thenReturn(marker1);
        
    }
    
    @Test
    public void testDefaultName() throws IOException {
        
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        
        inOrder.verify(generator).writeArrayFieldStart(TagsJsonProvider.FIELD_TAGS);
        inOrder.verify(generator).writeString("marker1");
        inOrder.verify(generator).writeString("marker4");
        inOrder.verify(generator).writeEndArray();
    }

    @Test
    public void testFieldName() throws IOException {
        provider.setFieldName("newFieldName");
        
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        
        inOrder.verify(generator).writeArrayFieldStart("newFieldName");
        inOrder.verify(generator).writeString("marker1");
        inOrder.verify(generator).writeString("marker4");
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
        inOrder.verify(generator).writeString("marker4");
        inOrder.verify(generator).writeEndArray();
    }

}
