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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.regex.Pattern;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonGenerator;

@ExtendWith(MockitoExtension.class)
public class StackHashJsonProviderTest {
    
    private StackHashJsonProvider provider = new StackHashJsonProvider();
    
    @Mock
    private JsonGenerator generator;
    
    @Mock
    private ILoggingEvent event;

    @Mock
    private ThrowableProxy throwableProxy;

    private static Pattern HEX_PATTERN = Pattern.compile("[0-9a-fA-F]{1,8}");

    @Test
    public void testDefaultName() throws IOException {
        // GIVEN
        when(event.getThrowableProxy()).thenReturn(throwableProxy);
        when(throwableProxy.getThrowable()).thenReturn(new Exception("test error"));
        // WHEN
        provider.writeTo(generator, event);
        // THEN
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(generator).writeStringField(eq(StackHashJsonProvider.FIELD_NAME), hashCaptor.capture());
        Assertions.assertTrue(HEX_PATTERN.matcher(hashCaptor.getValue()).matches(), "Did not produce an hexadecimal integer: "+hashCaptor.getValue());
    }

    @Test
    public void testFieldName() throws IOException {
        // GIVEN
        when(event.getThrowableProxy()).thenReturn(throwableProxy);
        when(throwableProxy.getThrowable()).thenReturn(new Exception("test error"));
        provider.setFieldName("newFieldName");
        // WHEN
        provider.writeTo(generator, event);
        // THEN
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(generator).writeStringField(eq("newFieldName"), hashCaptor.capture());
        Assertions.assertTrue(HEX_PATTERN.matcher(hashCaptor.getValue()).matches(), "Did not produce an hexadecimal integer: "+hashCaptor.getValue());
    }
}
