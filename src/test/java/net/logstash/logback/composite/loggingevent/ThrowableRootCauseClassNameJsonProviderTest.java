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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
public class ThrowableRootCauseClassNameJsonProviderTest {

    private AbstractThrowableClassNameJsonProvider provider = new ThrowableRootCauseClassNameJsonProvider();

    @Mock
    private JsonGenerator generator;

    @Mock
    private ILoggingEvent event;

    @Test
    public void testFieldName() throws IOException {
        check(ThrowableRootCauseClassNameJsonProvider.FIELD_NAME);
    }

    @Test
    public void testCustomFieldName() throws IOException {
        provider.setFieldName("newFieldName");
        check("newFieldName");
    }

    @Test
    public void testFieldNameWithoutNestedException() throws IOException {
        IOException throwable = new IOException();
        check(ThrowableRootCauseClassNameJsonProvider.FIELD_NAME, throwable,
                throwable.getClass().getSimpleName());
    }

    private void check(String fieldName) throws IOException {
        check(fieldName, new IOException(new IllegalArgumentException(new IllegalStateException())),
                IllegalStateException.class.getSimpleName());
    }

    private void check(String fieldName, Throwable throwable, String expectedClassName) throws IOException {
        when(event.getThrowableProxy()).thenReturn(new ThrowableProxy(throwable));

        provider.writeTo(generator, event);

        verify(generator).writeStringField(fieldName, expectedClassName);
    }

    @Test
    public void testNoThrowable() throws IOException {
        provider.writeTo(generator, event);

        verify(generator, times(0)).writeStringField(anyString(), anyString());
    }
}