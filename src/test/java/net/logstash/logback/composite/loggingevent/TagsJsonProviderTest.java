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
import static org.mockito.Mockito.spy;

import net.logstash.logback.fieldnames.LogstashFieldNames;
import net.logstash.logback.marker.LogstashMarker;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import tools.jackson.core.JsonGenerator;

@ExtendWith(MockitoExtension.class)
public class TagsJsonProviderTest {
    
    private final TagsJsonProvider provider = new TagsJsonProvider();
    
    @Mock
    private JsonGenerator generator;
    
    private ILoggingEvent event;

    @BeforeEach
    public void setup() {
        Marker marker1 = createBasicMarker("marker1");
        LogstashMarker marker2 = createLogstashMarker("marker2");
        Marker marker3 = createBasicMarker("marker3");
        
        marker1.add(marker2);
        marker2.add(marker3);

        Marker marker4 = createBasicMarker("marker4");

        event = createEvent(marker1, marker4);
    }
    
    @Test
    public void testDefaultName() {
        
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        
        inOrder.verify(generator).writeName(TagsJsonProvider.FIELD_TAGS);
        inOrder.verify(generator).writeStartArray();
        inOrder.verify(generator).writeString("marker1");
        inOrder.verify(generator).writeString("marker3");
        inOrder.verify(generator).writeString("marker4");
        inOrder.verify(generator).writeEndArray();
        
        Mockito.verifyNoMoreInteractions(generator);
    }

    @Test
    public void testFieldName() {
        provider.setFieldName("newFieldName");
        
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        
        inOrder.verify(generator).writeName("newFieldName");
        inOrder.verify(generator).writeStartArray();
        inOrder.verify(generator).writeString("marker1");
        inOrder.verify(generator).writeString("marker3");
        inOrder.verify(generator).writeString("marker4");
        inOrder.verify(generator).writeEndArray();
        
        Mockito.verifyNoMoreInteractions(generator);
    }

    @Test
    public void testFieldNames() {
        LogstashFieldNames fieldNames = new LogstashFieldNames();
        fieldNames.setTags("newFieldName");
        provider.setFieldNames(fieldNames);
        
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        
        inOrder.verify(generator).writeName("newFieldName");
        inOrder.verify(generator).writeStartArray();
        inOrder.verify(generator).writeString("marker1");
        inOrder.verify(generator).writeString("marker3");
        inOrder.verify(generator).writeString("marker4");
        inOrder.verify(generator).writeEndArray();
        
        Mockito.verifyNoMoreInteractions(generator);
    }

    
    // -- Utility methods -------------------------------------------------------------------------
    
    private Marker createBasicMarker(String name) {
        return MarkerFactory.getDetachedMarker(name);
    }
    
    private LogstashMarker createLogstashMarker(String name) {
        return spy(new TestLogstashMarker(name));
    }
    
    private LoggingEvent createEvent(Marker... markers) {
        LoggingEvent event = spy(new LoggingEvent());
        
        if (markers != null && markers.length > 0) {
            for (Marker marker: markers) {
                event.addMarker(marker);
            }
        }
        
        return event;
    }
    
    private static class TestLogstashMarker extends LogstashMarker {
        TestLogstashMarker(String name) {
            super(name);
        }

        @Override
        public void writeTo(JsonGenerator generator) {
            // noop
        }
    }
}
