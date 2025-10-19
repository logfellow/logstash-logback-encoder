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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.function.Function;

import net.logstash.logback.test.AbstractLogbackTest;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.SequenceNumberGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JsonGenerator;

@ExtendWith(MockitoExtension.class)
public class SequenceJsonProviderTest extends AbstractLogbackTest {

    private final SequenceJsonProvider provider = new SequenceJsonProvider();

    @Mock
    private JsonGenerator generator;

    @Mock
    private ILoggingEvent event;


    @Test
    public void testNoContext() {
        assertThatThrownBy(provider::start).isInstanceOf(IllegalStateException.class);
    }

    
    @Test
    public void testProviderNotStarted() {
        provider.setContext(context);
        assertThatThrownBy(() -> provider.writeTo(generator, event)).isInstanceOf(IllegalStateException.class);
    }
    
    
    @Test
    public void testDefaultName() throws IOException {
        provider.setContext(context);
        provider.start();
        
        provider.writeTo(generator, event);
        
        verify(generator).writeNumberProperty(SequenceJsonProvider.FIELD_SEQUENCE, 1L);
    }
    
    
    @Test
    public void testCustomFieldName() {
        provider.setContext(context);
        provider.setFieldName("newFieldName");
        provider.start();
        
        provider.writeTo(generator, event);
        
        verify(generator).writeNumberProperty("newFieldName", 1L);
    }
    
    
    @Test
    public void testNoSequenceGeneratorInContext() {
        context.setSequenceNumberGenerator(null);
        provider.setContext(context);
        provider.start();
        
        // a WARN status is logged about missing SequenceGenerator
        assertThat(statusManager.getCopyOfStatusList())
            .hasSize(1)
            .anyMatch(s -> s.getMessage().startsWith("No <sequenceNumberGenerator>"));
        
        // assert the local generator produces the expected output
        provider.writeTo(generator, event);
        verify(generator).writeName(SequenceJsonProvider.FIELD_SEQUENCE);
        verify(generator).writeNumber(1L);

        provider.writeTo(generator, event);
        verify(generator, times(2)).writeName(SequenceJsonProvider.FIELD_SEQUENCE);
        verify(generator).writeNumber(2L);

        verify(event, never()).getSequenceNumber();
    }
    
    
    @Test
    public void testSequenceGeneratorInContext() {
        // Set a SequenceNumberGenerator to the context
        SequenceNumberGenerator seqGenerator = mock(SequenceNumberGenerator.class);
        context.setSequenceNumberGenerator(seqGenerator);
        provider.setContext(context);
        provider.start();
        
        // LoggingEvent is a mock -> it won't have its sequence number out of the generator -> mock the returned values
        when(event.getSequenceNumber()).thenReturn(123L);
        
        // no WARN status logged
        assertThat(statusManager.getCopyOfStatusList())
            .noneMatch(s -> s.getMessage().startsWith("No <sequenceNumberGenerator>"));
        
        // assert expected output
        provider.writeTo(generator, event);
        verify(generator).writeNumberProperty(SequenceJsonProvider.FIELD_SEQUENCE, 123L);
        
        verify(event, times(1)).getSequenceNumber();
    }
    
    
    @Test
    public void testCustomSequenceProvider() throws IOException {
        Function<ILoggingEvent, Long> sequenceProvider = spy(new Function<ILoggingEvent, Long>() {
            @Override
            public Long apply(ILoggingEvent t) {
                return 456L;
            }
        });
        provider.setSequenceProvider(sequenceProvider);
        provider.setContext(context);
        provider.start();
        
        
        // no WARN status logged
        assertThat(statusManager.getCopyOfStatusList())
            .noneMatch(s -> s.getMessage().startsWith("No <sequenceNumberGenerator>"));
        
        // assert expected output
        provider.writeTo(generator, event);
        verify(generator).writeNumberProperty(SequenceJsonProvider.FIELD_SEQUENCE, 456L);
        
        verify(sequenceProvider).apply(event);
        verify(event, never()).getSequenceNumber();
    }
}
