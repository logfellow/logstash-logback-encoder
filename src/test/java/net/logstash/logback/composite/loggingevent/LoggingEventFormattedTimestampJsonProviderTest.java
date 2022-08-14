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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import net.logstash.logback.composite.AbstractFormattedTimestampJsonProvider;
import net.logstash.logback.util.LogbackUtils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LoggingEventFormattedTimestampJsonProviderTest {

    @Mock
    private JsonGenerator generator;

    @Mock(lenient = true)
    private ILoggingEvent event;

    private Instant now;
    
    @BeforeEach
    public void setup() {
        // Logback has nano precision since version 1.3
        if (LogbackUtils.isVersion13()) {
            now = Instant.now();
            when(event.getTimeStamp()).thenReturn(now.toEpochMilli());
            when(event.getInstant()).thenReturn(now);
        }
        else {
            now = Instant.ofEpochMilli(System.currentTimeMillis());
            when(event.getTimeStamp()).thenReturn(now.toEpochMilli());
        }
    }
    
    
    @Test
    public void withDefaults() throws IOException {
        LoggingEventFormattedTimestampJsonProvider provider = new LoggingEventFormattedTimestampJsonProvider();

        provider.writeTo(generator, event);

        String expectedValue = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(TimeZone.getDefault().toZoneId()).format(now);
        verify(generator).writeStringField(AbstractFormattedTimestampJsonProvider.FIELD_TIMESTAMP, expectedValue);
    }

    @Test
    public void customTimeZone() throws IOException {
        LoggingEventFormattedTimestampJsonProvider provider = new LoggingEventFormattedTimestampJsonProvider();
        provider.setTimeZone("UTC");

        provider.writeTo(generator, event);

        String expectedValue = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("UTC")).format(now);
        verify(generator).writeStringField(AbstractFormattedTimestampJsonProvider.FIELD_TIMESTAMP, expectedValue);
    }

    @Test
    public void constant() throws IOException {
        LoggingEventFormattedTimestampJsonProvider provider = new LoggingEventFormattedTimestampJsonProvider();
        provider.setPattern("[RFC_1123_DATE_TIME]"); // use a DateTimeFormatter pattern not supported by the FastISOTimestampFormatter

        provider.writeTo(generator, event);

        String expectedValue = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(TimeZone.getDefault().toZoneId()).format(now);
        verify(generator).writeStringField(AbstractFormattedTimestampJsonProvider.FIELD_TIMESTAMP, expectedValue);
    }

    @Test
    public void unknownConstant() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            LoggingEventFormattedTimestampJsonProvider provider = new LoggingEventFormattedTimestampJsonProvider();
            provider.setPattern("[foo]");
        });
    }

    @Test
    public void customPattern() throws IOException {
        LoggingEventFormattedTimestampJsonProvider provider = new LoggingEventFormattedTimestampJsonProvider();
        String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS";
        provider.setPattern(pattern);

        provider.writeTo(generator, event);

        String expectedValue = DateTimeFormatter.ofPattern(pattern).withZone(TimeZone.getDefault().toZoneId()).format(now);
        verify(generator).writeStringField(AbstractFormattedTimestampJsonProvider.FIELD_TIMESTAMP, expectedValue);
    }

    @Test
    public void unixEpochAsNumber() throws IOException {
        LoggingEventFormattedTimestampJsonProvider provider = new LoggingEventFormattedTimestampJsonProvider();
        provider.setPattern(AbstractFormattedTimestampJsonProvider.UNIX_TIMESTAMP_AS_NUMBER);

        provider.writeTo(generator, event);

        verify(generator).writeNumberField(AbstractFormattedTimestampJsonProvider.FIELD_TIMESTAMP, event.getTimeStamp());
    }

    @Test
    public void unixEpochAsString() throws IOException {
        LoggingEventFormattedTimestampJsonProvider provider = new LoggingEventFormattedTimestampJsonProvider();
        provider.setPattern(AbstractFormattedTimestampJsonProvider.UNIX_TIMESTAMP_AS_STRING);

        provider.writeTo(generator, event);

        verify(generator).writeStringField(AbstractFormattedTimestampJsonProvider.FIELD_TIMESTAMP, Long.toString(event.getTimeStamp()));
    }
}
