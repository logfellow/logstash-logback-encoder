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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.fieldnames.LogstashFieldNames;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonGenerator;

@ExtendWith(MockitoExtension.class)
public class MessageJsonProviderTest {
    
    private MessageJsonProvider provider = new MessageJsonProvider();
    
    @Mock
    private JsonGenerator generator;
    
    @Mock
    private ILoggingEvent event;
    
    @Test
    public void testDefaultName() throws IOException {
        
        when(event.getFormattedMessage()).thenReturn("message");
        
        provider.writeTo(generator, event);
        
        verify(generator).writeStringField(MessageJsonProvider.FIELD_MESSAGE, "message");
    }

    @Test
    public void testFieldName() throws IOException {
        provider.setFieldName("newFieldName");
        
        when(event.getFormattedMessage()).thenReturn("message");
        
        provider.writeTo(generator, event);
        
        verify(generator).writeStringField("newFieldName", "message");
    }

    @Test
    public void testFieldNames() throws IOException {
        LogstashFieldNames fieldNames = new LogstashFieldNames();
        fieldNames.setMessage("newFieldName");
        
        provider.setFieldNames(fieldNames);
        
        when(event.getFormattedMessage()).thenReturn("message");
        
        provider.writeTo(generator, event);
        
        verify(generator).writeStringField("newFieldName", "message");
    }

    @Test
    public void testMessageSplitDisabledByDefault() throws Exception {
        assertNull(provider.getMessageSplitRegex());

        mockEventMessage("###");
        provider.writeTo(generator, event);

        verifySingleLineMessageGenerated("###");
    }

    @Test
    public void testMessageSplitDisabledForNullRegex() throws Exception {
        provider.setMessageSplitRegex(null);
        assertNull(provider.getMessageSplitRegex());

        mockEventMessage("###");
        provider.writeTo(generator, event);

        verifySingleLineMessageGenerated("###");
    }

    @Test
    public void testMessageSplitDisabledForEmptyRegex() throws Exception {
        provider.setMessageSplitRegex("");
        assertNull(provider.getMessageSplitRegex());

        mockEventMessage("###");
        provider.writeTo(generator, event);

        verifySingleLineMessageGenerated("###");
    }

    @Test
    public void testMessageSplitWithSystemSeparator() throws IOException {
        provider.setMessageSplitRegex("SYSTEM");
        assertEquals(System.lineSeparator(), provider.getMessageSplitRegex());

        mockEventMessage(System.lineSeparator());
        provider.writeTo(generator, event);

        verifyMultiLineMessageGenerated();
    }

    @Test
    public void testMessageSplitWithUnixSeparator() throws IOException {
        provider.setMessageSplitRegex("UNIX");
        assertEquals("\n", provider.getMessageSplitRegex());

        mockEventMessage("\n");
        provider.writeTo(generator, event);

        verifyMultiLineMessageGenerated();
    }

    @Test
    public void testMessageSplitWithWindowsSeparator() throws IOException {
        provider.setMessageSplitRegex("WINDOWS");
        assertEquals("\r\n", provider.getMessageSplitRegex());

        mockEventMessage("\r\n");
        provider.writeTo(generator, event);

        verifyMultiLineMessageGenerated();
    }

    @Test
    public void testMessageSplitWithCustomRegex() throws IOException {
        provider.setMessageSplitRegex("#+");
        assertEquals("#+", provider.getMessageSplitRegex());

        mockEventMessage("###");
        provider.writeTo(generator, event);

        verifyMultiLineMessageGenerated();
    }

    @Test
    public void testMessageSplitWithInvalidRegex() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            provider.setMessageSplitRegex("++");
        });
    }

    private void mockEventMessage(String lineSeparator) {
        String message = buildMultiLineMessage(lineSeparator);
        when(event.getFormattedMessage()).thenReturn(message);
    }

    private void verifySingleLineMessageGenerated(String lineSeparator) throws IOException {
        String message = buildMultiLineMessage(lineSeparator);
        verify(generator).writeStringField(MessageJsonProvider.FIELD_MESSAGE, message);
    }

    private void verifyMultiLineMessageGenerated() throws IOException {
        verify(generator).writeArrayFieldStart(MessageJsonProvider.FIELD_MESSAGE);
        verify(generator).writeString("line1");
        verify(generator).writeString("line2");
        verify(generator).writeString("line3");
        verify(generator).writeEndArray();
    }

    private static String buildMultiLineMessage(String lineSeparator) {
        return String.join(lineSeparator, "line1", "line2", "line3");
    }
}
