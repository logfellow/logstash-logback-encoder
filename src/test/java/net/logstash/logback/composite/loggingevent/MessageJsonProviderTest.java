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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import net.logstash.logback.fieldnames.LogstashFieldNames;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        assertThat(provider.getMessageSplitRegex()).isNull();

        mockEventMessage("###");
        provider.writeTo(generator, event);

        verifySingleLineMessageGenerated("###");
    }

    @Test
    public void testMessageSplitDisabledForNullRegex() throws Exception {
        provider.setMessageSplitRegex(null);
        assertThat(provider.getMessageSplitRegex()).isNull();

        mockEventMessage("###");
        provider.writeTo(generator, event);

        verifySingleLineMessageGenerated("###");
    }

    @Test
    public void testMessageSplitDisabledForEmptyRegex() throws Exception {
        provider.setMessageSplitRegex("");
        assertThat(provider.getMessageSplitRegex()).isNull();

        mockEventMessage("###");
        provider.writeTo(generator, event);

        verifySingleLineMessageGenerated("###");
    }

    @Test
    public void testMessageSplitWithSystemSeparator() throws IOException {
        provider.setMessageSplitRegex("SYSTEM");
        assertThat(provider.getMessageSplitRegex()).isEqualTo(System.lineSeparator());

        mockEventMessage(System.lineSeparator());
        provider.writeTo(generator, event);

        verifyMultiLineMessageGenerated();
    }

    @Test
    public void testMessageSplitWithUnixSeparator() throws IOException {
        provider.setMessageSplitRegex("UNIX");
        assertThat(provider.getMessageSplitRegex()).isEqualTo("\n");

        mockEventMessage("\n");
        provider.writeTo(generator, event);

        verifyMultiLineMessageGenerated();
    }

    @Test
    public void testMessageSplitWithWindowsSeparator() throws IOException {
        provider.setMessageSplitRegex("WINDOWS");
        assertThat(provider.getMessageSplitRegex()).isEqualTo("\r\n");

        mockEventMessage("\r\n");
        provider.writeTo(generator, event);

        verifyMultiLineMessageGenerated();
    }

    @Test
    public void testMessageSplitWithCustomRegex() throws IOException {
        provider.setMessageSplitRegex("#+");
        assertThat(provider.getMessageSplitRegex()).isEqualTo("#+");

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
