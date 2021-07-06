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
import java.util.Collections;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.marker.LogstashMarker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Marker;

import com.fasterxml.jackson.core.JsonGenerator;

@ExtendWith(MockitoExtension.class)
public class LogstashMarkersJsonProviderTest {
    
    private LogstashMarkersJsonProvider provider = new LogstashMarkersJsonProvider();
    
    @Mock
    private JsonGenerator generator;
    
    @Mock
    private ILoggingEvent event;
    
    @Mock
    private LogstashMarker outerMarker;
    
    @Mock
    private LogstashMarker innerMarker;
    
    @Test
    public void test() throws IOException {
        
        when(outerMarker.hasReferences()).thenReturn(true);
        when(outerMarker.iterator()).thenReturn(Collections.<Marker>singleton(innerMarker).iterator());
        
        when(event.getMarker()).thenReturn(outerMarker);
        
        provider.writeTo(generator, event);
        
        verify(outerMarker).writeTo(generator);
        verify(innerMarker).writeTo(generator);
    }

}
