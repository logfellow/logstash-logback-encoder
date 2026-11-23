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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.json.JsonMapper;

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
    
    @ParameterizedTest
    @ValueSource(strings = {"nested", "newFieldName", "", "   "})
    public void testWrite(String fieldName) {
        
        provider.setFieldName(fieldName);
        provider.start();
        assertThat(provider.isStarted()).isTrue();
        
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator, providers);
        
        inOrder.verify(generator).writeObjectPropertyStart(fieldName);
        inOrder.verify(providers).writeTo(generator, event);
        inOrder.verify(generator).writeEndObject();
    }

    
    @Test
    public void shouldRejectNullFieldNameBeforeStartingProviders() {
        provider.setFieldName(null);

        assertThatIllegalStateException()
                .isThrownBy(provider::start)
                .withMessage("fieldName must not be null");

        assertThat(provider.isStarted()).isFalse();
        verify(providers, never()).start();
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
     * ObjectMapper set on the enclosed JsonProviders as well
     */
    @Test
    public void objectMapperOnProviders() {
        JsonMapper jsonMapper = JsonMapper.builder().build();
        provider.setObjectMapper(jsonMapper);
        verify(providers).setObjectMapper(jsonMapper);
    }
}
