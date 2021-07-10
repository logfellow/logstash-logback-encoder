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

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.io.IOException;

import net.logstash.logback.fieldnames.ShortenedFieldNames;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RootStackTraceElementJsonProviderTest {

    private RootStackTraceElementJsonProvider provider = new RootStackTraceElementJsonProvider();

    @Mock
    private JsonGenerator generator;

    @Mock
    private ILoggingEvent event;

    @Mock
    private ThrowableProxy throwableProxy;

    @Mock
    private StackTraceElementProxy steProxy;

    private StackTraceElement ste = new StackTraceElement("TestDeclaringClass", "testMethodName", "testFileName", 0);

    @Test
    public void testStackTraceElementIsWritten() throws IOException {
        // GIVEN
        when(event.getThrowableProxy()).thenReturn(throwableProxy);
        StackTraceElementProxy[] steArray = new StackTraceElementProxy[]{steProxy};
        when(throwableProxy.getStackTraceElementProxyArray()).thenReturn(steArray);
        when(steProxy.getStackTraceElement()).thenReturn(ste);
        provider.setFieldName(RootStackTraceElementJsonProvider.FIELD_STACKTRACE_ELEMENT);
        // WHEN
        provider.writeTo(generator, event);
        // THEN
        InOrder inOrder = inOrder(generator);

        inOrder.verify(generator).writeObjectFieldStart(RootStackTraceElementJsonProvider.FIELD_STACKTRACE_ELEMENT);
        inOrder.verify(generator).writeStringField(RootStackTraceElementJsonProvider.FIELD_CLASS_NAME, "TestDeclaringClass");
        inOrder.verify(generator).writeStringField(RootStackTraceElementJsonProvider.FIELD_METHOD_NAME, "testMethodName");
        inOrder.verify(generator).writeEndObject();
    }

    @Test
    public void testOverrideFieldNameWithShortNames() throws IOException {
        // GIVEN
        when(event.getThrowableProxy()).thenReturn(throwableProxy);
        StackTraceElementProxy[] steArray = new StackTraceElementProxy[]{steProxy};
        when(throwableProxy.getStackTraceElementProxyArray()).thenReturn(steArray);
        when(steProxy.getStackTraceElement()).thenReturn(ste);
        provider.setFieldNames(new ShortenedFieldNames());
        // WHEN
        provider.writeTo(generator, event);
        // THEN
        InOrder inOrder = inOrder(generator);

        inOrder.verify(generator).writeObjectFieldStart(RootStackTraceElementJsonProvider.FIELD_STACKTRACE_ELEMENT);
        inOrder.verify(generator).writeStringField(ShortenedFieldNames.FIELD_CLASS, "TestDeclaringClass");
        inOrder.verify(generator).writeStringField(ShortenedFieldNames.FIELD_METHOD, "testMethodName");
        inOrder.verify(generator).writeEndObject();
    }
}
