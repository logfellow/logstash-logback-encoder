/*
 * Copyright 2013-2022 the original author or authors.
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

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ThrowableMessageJsonProviderTest {

    private ThrowableMessageJsonProvider provider = new ThrowableMessageJsonProvider();

    @Mock
    private JsonGenerator generator;

    @Mock
    private ILoggingEvent event;

    @Test
    public void testFieldName() throws IOException {
        provider.setFieldName("newFieldName");

        IOException throwable = new IOException("kaput");
        when(event.getThrowableProxy()).thenReturn(new ThrowableProxy(throwable));

        provider.writeTo(generator, event);

        verify(generator).writeStringField("newFieldName", "kaput");
    }

    @Test
    public void testNoThrowable() throws IOException {
        when(event.getThrowableProxy()).thenReturn(null);

        provider.writeTo(generator, event);

        verify(event, atLeastOnce()).getThrowableProxy();
        verifyNoInteractions(generator);
    }
}
