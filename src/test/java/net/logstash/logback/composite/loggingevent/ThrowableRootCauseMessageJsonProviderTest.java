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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JsonGenerator;

@ExtendWith(MockitoExtension.class)
public class ThrowableRootCauseMessageJsonProviderTest {

    private final AbstractThrowableMessageJsonProvider provider = new ThrowableRootCauseMessageJsonProvider();

    @Mock
    private JsonGenerator generator;

    @Mock
    private ILoggingEvent event;

    @Test
    public void testNoThrowable() {
        when(event.getThrowableProxy()).thenReturn(null);

        provider.writeTo(generator, event);

        verify(event, atLeastOnce()).getThrowableProxy();
        verifyNoInteractions(generator);
    }

    @Test
    public void testDefaultFieldName() {
        when(event.getThrowableProxy()).thenReturn(new ThrowableProxy(new Exception("kaput")));

        provider.writeTo(generator, event);

        verify(generator).writeName("throwable_root_cause_message");
        verify(generator).writeString("kaput");
    }

    @Test
    public void testCustomFieldName() {
        when(event.getThrowableProxy()).thenReturn(new ThrowableProxy(new Exception("kaput")));

        provider.setFieldName("some_custom_field");
        provider.writeTo(generator, event);

        verify(generator).writeName("some_custom_field");
        verify(generator).writeString("kaput");
    }

    @Test
    public void testNestedException() {
        Exception foo = new Exception("foo", new Exception("bar", new Exception("baz")));
        when(event.getThrowableProxy()).thenReturn(new ThrowableProxy(foo));

        provider.writeTo(generator, event);

        verify(generator).writeName(anyString());
        verify(generator).writeString("baz");
    }

    @Test
    public void testCircularReference() {
        IThrowableProxy foo = mock(IThrowableProxy.class, "foo");
        IThrowableProxy bar = mock(IThrowableProxy.class, "bar");
        IThrowableProxy baz = mock(IThrowableProxy.class, "baz");
        when(foo.getCause()).thenReturn(bar);
        when(bar.getCause()).thenReturn(baz);
        when(baz.getCause()).thenReturn(foo);

        when(event.getThrowableProxy()).thenReturn(foo);

        provider.writeTo(generator, event);

        verify(event, atLeastOnce()).getThrowableProxy();
        verifyNoInteractions(generator);
    }
}
