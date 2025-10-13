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

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JsonGenerator;

@ExtendWith(MockitoExtension.class)
public class ThrowableRootCauseClassNameJsonProviderTest {

    private final AbstractThrowableClassNameJsonProvider provider = new ThrowableRootCauseClassNameJsonProvider();

    @Mock
    private JsonGenerator generator;

    @Mock
    private ILoggingEvent event;

    @Test
    public void testFieldName() {
        check(ThrowableRootCauseClassNameJsonProvider.FIELD_NAME);
    }

    @Test
    public void testCustomFieldName() {
        provider.setFieldName("newFieldName");
        check("newFieldName");
    }

    @Test
    public void testFieldNameWithoutNestedException() {
        IOException throwable = new IOException();
        check(ThrowableRootCauseClassNameJsonProvider.FIELD_NAME, throwable,
                throwable.getClass().getSimpleName());
    }

    private void check(String fieldName) {
        check(fieldName, new IOException(new IllegalArgumentException(new IllegalStateException())),
                IllegalStateException.class.getSimpleName());
    }

    private void check(String fieldName, Throwable throwable, String expectedClassName) {
        when(event.getThrowableProxy()).thenReturn(new ThrowableProxy(throwable));

        provider.writeTo(generator, event);

        verify(generator).writeStringProperty(fieldName, expectedClassName);
    }

    @Test
    public void testNoThrowable() {
        provider.writeTo(generator, event);

        verifyNoInteractions(generator);
    }

    @Test
    public void testCircularReference() {
         IThrowableProxy baz = mock(IThrowableProxy.class, "baz");
         IThrowableProxy bar = mock(IThrowableProxy.class, "bar");
         IThrowableProxy foo = mock(IThrowableProxy.class, "foo");
         when(foo.getCause()).thenReturn(bar);
         when(bar.getCause()).thenReturn(baz);
         when(baz.getCause()).thenReturn(foo);

        when(event.getThrowableProxy()).thenReturn(foo);

        provider.writeTo(generator, event);

        verify(event, atLeastOnce()).getThrowableProxy();
        verifyNoInteractions(generator);
    }
}
