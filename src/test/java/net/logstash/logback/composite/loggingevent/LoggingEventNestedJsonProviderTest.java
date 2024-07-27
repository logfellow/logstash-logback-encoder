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
import static org.mockito.Mockito.verify;

import java.io.IOException;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LoggingEventNestedJsonProviderTest {

    @InjectMocks
    private LoggingEventNestedJsonProvider provider;
    
    @Mock
    private JsonGenerator generator;
    
    @Mock
    private ILoggingEvent event;
    
    @Mock
    private LoggingEventJsonProviders providers;

    @BeforeEach
    void beforeEach() {
        provider.setProviders(providers);
    }
    
    @Test
    public void testWrite() throws IOException {
        
        provider.setFieldName("newFieldName");
        
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator, providers);
        
        inOrder.verify(generator).writeFieldName("newFieldName");
        inOrder.verify(generator).writeStartObject();
        inOrder.verify(providers).writeTo(generator, event);
        inOrder.verify(generator).writeEndObject();
    }

    
    /*
     * start/stop cascaded to the enclosed JsonProviders
     */
    @Test
    public void startStopProviders() {
        provider.start();
        verify(providers).start();
        
        provider.stop();
        verify(providers).stop();
    }
    
    /*
     * JsonFactory set on the enclosed JsonProviders as well
     */
    @Test
    public void jsonFactoryOnProviders() {
        JsonFactory jsonFactory = new JsonFactoryBuilder().build();
        provider.setJsonFactory(jsonFactory);
        verify(providers).setJsonFactory(jsonFactory);
    }
}
