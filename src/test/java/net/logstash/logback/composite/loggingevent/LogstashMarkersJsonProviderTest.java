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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import net.logstash.logback.marker.LogstashMarker;

import ch.qos.logback.classic.spi.LoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

@ExtendWith(MockitoExtension.class)
public class LogstashMarkersJsonProviderTest {
    
    @Mock
    private JsonGenerator generator;
    
    private LogstashMarkersJsonProvider provider = new LogstashMarkersJsonProvider();
    private int markerCount;
    

    
    @Test
    public void noMarkers() {
        LoggingEvent event = createEvent();
        assertThatCode(() -> provider.writeTo(generator, event)).doesNotThrowAnyException();
    }

    /*
     * 
     */
    @Test
    public void singleMarker() throws IOException {
        
        // event:
        //  * basic1 -> marker1 -> marker11
        //                      -> basic12 -> marker121
        //           -> marker2
        
        
        Marker         basic1    = createBasicMarker();
        LogstashMarker marker1   = createLogstashMarker(); basic1.add(marker1);
        LogstashMarker marker11  = createLogstashMarker(); marker1.add(marker11);
        Marker         basic12   = createBasicMarker();    marker11.add(basic12);
        LogstashMarker marker121 = createLogstashMarker(); basic12.add(marker121);
                
        LoggingEvent event = createEvent(basic1);
        
        provider.writeTo(generator, event);
        
        verify(marker1).writeTo(generator);
        verify(marker11).writeTo(generator);
        verify(marker121).writeTo(generator);
    }
    
    
    @Test
    public void multipleMarkers() throws IOException {
        // event:
        //  * basic1 -> marker1
        //  * marker2
        
        
        Marker         basic1  = createBasicMarker();
        LogstashMarker marker1 = createLogstashMarker(); basic1.add(marker1);
        LogstashMarker marker2 = createLogstashMarker();
                
        LoggingEvent event = createEvent(basic1, marker2);
        
        provider.writeTo(generator, event);
        
        verify(marker1).writeTo(generator);
        verify(marker2).writeTo(generator);
    }
    
    
    
    // -- Utility methods -------------------------------------------------------------------------
    
    private Marker createBasicMarker() {
        return MarkerFactory.getDetachedMarker(Integer.toString(this.markerCount++));
    }
    
    private LogstashMarker createLogstashMarker() {
        return spy(new TestLogstashMarker(Integer.toString(this.markerCount++)));
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
        public void writeTo(JsonGenerator generator) throws IOException {
            // noop
        }
    }
}
