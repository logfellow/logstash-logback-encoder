/**
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.composite.FormattedTimestampJsonProvider;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.fasterxml.jackson.core.JsonGenerator;

public class LoggingEventFormattedTimestampJsonProviderTest {

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private JsonGenerator generator;

    @Mock
    private ILoggingEvent event;

    @Test
    public void withDefaults() throws IOException {
        LoggingEventFormattedTimestampJsonProvider provider = new LoggingEventFormattedTimestampJsonProvider();
        when(event.getTimeStamp()).thenReturn(0L);

        provider.writeTo(generator, event);

        String expectedValue = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(TimeZone.getDefault().toZoneId()).format(Instant.ofEpochMilli(0));
        verify(generator).writeStringField(FormattedTimestampJsonProvider.FIELD_TIMESTAMP, expectedValue);
    }

    @Test
    public void customTimeZone() throws IOException {
        LoggingEventFormattedTimestampJsonProvider provider = new LoggingEventFormattedTimestampJsonProvider();
        provider.setTimeZone("UTC");
        when(event.getTimeStamp()).thenReturn(0L);

        provider.writeTo(generator, event);

        String expectedValue = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("UTC")).format(Instant.ofEpochMilli(0));
        verify(generator).writeStringField(FormattedTimestampJsonProvider.FIELD_TIMESTAMP, expectedValue);
    }

    @Test
    public void constant() throws IOException {
        LoggingEventFormattedTimestampJsonProvider provider = new LoggingEventFormattedTimestampJsonProvider();
        provider.setPattern("[ISO_DATE_TIME]");
        when(event.getTimeStamp()).thenReturn(0L);

        provider.writeTo(generator, event);

        String expectedValue = DateTimeFormatter.ISO_DATE_TIME.withZone(TimeZone.getDefault().toZoneId()).format(Instant.ofEpochMilli(0));
        verify(generator).writeStringField(FormattedTimestampJsonProvider.FIELD_TIMESTAMP, expectedValue);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownConstant() throws IOException {
        LoggingEventFormattedTimestampJsonProvider provider = new LoggingEventFormattedTimestampJsonProvider();
        provider.setPattern("[foo]");
    }

    @Test
    public void customPattern() throws IOException {
        LoggingEventFormattedTimestampJsonProvider provider = new LoggingEventFormattedTimestampJsonProvider();
        String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS";
        provider.setPattern(pattern);
        when(event.getTimeStamp()).thenReturn(0L);

        provider.writeTo(generator, event);

        String expectedValue = DateTimeFormatter.ofPattern(pattern).withZone(TimeZone.getDefault().toZoneId()).format(Instant.ofEpochMilli(0));
        verify(generator).writeStringField(FormattedTimestampJsonProvider.FIELD_TIMESTAMP, expectedValue);
    }

    @Test
    public void unixEpochAsNumber() throws IOException {
        LoggingEventFormattedTimestampJsonProvider provider = new LoggingEventFormattedTimestampJsonProvider();
        provider.setPattern(FormattedTimestampJsonProvider.UNIX_TIMESTAMP_AS_NUMBER);
        when(event.getTimeStamp()).thenReturn(0L);

        provider.writeTo(generator, event);

        verify(generator).writeNumberField(FormattedTimestampJsonProvider.FIELD_TIMESTAMP, 0L);
    }

    @Test
    public void unixEpochAsString() throws IOException {
        LoggingEventFormattedTimestampJsonProvider provider = new LoggingEventFormattedTimestampJsonProvider();
        provider.setPattern(FormattedTimestampJsonProvider.UNIX_TIMESTAMP_AS_STRING);
        when(event.getTimeStamp()).thenReturn(0L);

        provider.writeTo(generator, event);

        verify(generator).writeStringField(FormattedTimestampJsonProvider.FIELD_TIMESTAMP, "0");
    }


}