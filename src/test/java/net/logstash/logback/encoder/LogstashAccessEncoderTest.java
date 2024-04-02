/*
 * Copyright 2013-2023 the original author or authors.
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
package net.logstash.logback.encoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import net.logstash.logback.composite.AbstractFormattedTimestampJsonProvider;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class LogstashAccessEncoderTest {
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final LogstashAccessEncoder encoder = new LogstashAccessEncoder();

    @Test
    public void basicsAreIncluded_logback12OrLater() throws Exception {
        
        final long timestamp = System.currentTimeMillis();
        
        IAccessEvent event = mockBasicILoggingEvent();
        when(event.getTimeStamp()).thenReturn(timestamp);
        
        encoder.getFieldNames().setTimestamp("timestamp");
        
        encoder.start();
        byte[] encoded = encoder.encode(event);
        
        JsonNode node = MAPPER.readTree(encoded);
        
        verifyBasics(timestamp, event, node);
    }
    
    protected void verifyBasics(final long timestamp, IAccessEvent event, JsonNode node) {
        assertThat(node.get("timestamp").textValue()).isEqualTo(DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(TimeZone.getDefault().toZoneId()).format(Instant.ofEpochMilli(timestamp)));
        assertThat(node.get("@version").textValue()).isEqualTo("1");
        assertThat(node.get("message").textValue()).isEqualTo(String.format("%s - %s [%s] \"%s\" %s %s", event.getRemoteHost(), event.getRemoteUser(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(TimeZone.getDefault().toZoneId())
                        .format(Instant.ofEpochMilli(event.getTimeStamp())), event.getRequestURL(), event.getStatusCode(),
                event.getContentLength()));
        
        assertThat(node.get("method").textValue()).isEqualTo(event.getMethod());
        assertThat(node.get("protocol").textValue()).isEqualTo(event.getProtocol());
        assertThat(node.get("status_code").asInt()).isEqualTo(event.getStatusCode());
        assertThat(node.get("requested_url").textValue()).isEqualTo(event.getRequestURL());
        assertThat(node.get("requested_uri").textValue()).isEqualTo(event.getRequestURI());
        assertThat(node.get("remote_host").textValue()).isEqualTo(event.getRemoteHost());
        assertThat(node.get("remote_user").textValue()).isEqualTo(event.getRemoteUser());
        assertThat(node.get("content_length").asLong()).isEqualTo(event.getContentLength());
        assertThat(node.get("elapsed_time").asLong()).isEqualTo(event.getElapsedTime());
        assertThat(node.get("request_headers")).isNull();
        assertThat(node.get("response_headers")).isNull();
    }
    
    @Test
    public void propertiesInContextAreIncluded() throws Exception {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put("thing_one", "One");
        propertyMap.put("thing_two", "Three");
        
        final Context context = mock(Context.class);
        when(context.getCopyOfPropertyMap()).thenReturn(propertyMap);
        
        IAccessEvent event = mockBasicILoggingEvent();
        
        encoder.setContext(context);
        encoder.start();
        byte[] encoded = encoder.encode(event);
        
        JsonNode node = MAPPER.readTree(encoded);
        
        assertThat(node.get("thing_one").textValue()).isEqualTo("One");
        assertThat(node.get("thing_two").textValue()).isEqualTo("Three");
    }
    
    @Test
    public void requestAndResponseHeadersAreIncluded() throws Exception {

        IAccessEvent event = mockBasicILoggingEvent();
        
        encoder.getFieldNames().setRequestHeaders("request_headers");
        encoder.getFieldNames().setResponseHeaders("response_headers");
        encoder.start();
        byte[] encoded = encoder.encode(event);
        
        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("request_headers").size()).isEqualTo(2);
        assertThat(node.get("request_headers").get("user-agent").textValue()).isEqualTo("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36");
        assertThat(node.get("request_headers").get("accept").textValue()).isEqualTo("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        assertThat(node.get("request_headers").get("unknown")).isNull();
        
        assertThat(node.get("response_headers").size()).isEqualTo(2);
        assertThat(node.get("response_headers").get("content-type").textValue()).isEqualTo("text/html; charset=UTF-8");
        assertThat(node.get("response_headers").get("content-length").textValue()).isEqualTo("42");
        assertThat(node.get("response_headers").get("unknown")).isNull();
    }
    
    @Test
    public void testCustomFields() {
        String customFields = "{\"foo\":\"bar\"}";
        encoder.setCustomFields(customFields);
        assertThat(encoder.getCustomFields()).isEqualTo(customFields);
    }
    
    @Test
    public void unixTimestampAsNumber() throws Exception {
        final long timestamp = System.currentTimeMillis();
        
        IAccessEvent event = mockBasicILoggingEvent();
        when(event.getTimeStamp()).thenReturn(timestamp);
        
        encoder.setTimestampPattern(AbstractFormattedTimestampJsonProvider.UNIX_TIMESTAMP_AS_NUMBER);
        encoder.start();
        byte[] encoded = encoder.encode(event);
        
        JsonNode node = MAPPER.readTree(encoded);
        
        assertThat(node.get("@timestamp").numberValue()).isEqualTo(timestamp);
    }
    
    @Test
    public void unixTimestampAsString() throws Exception {
        final long timestamp = System.currentTimeMillis();
        
        IAccessEvent event = mockBasicILoggingEvent();
        when(event.getTimeStamp()).thenReturn(timestamp);
        
        encoder.setTimestampPattern(AbstractFormattedTimestampJsonProvider.UNIX_TIMESTAMP_AS_STRING);
        encoder.start();
        byte[] encoded = encoder.encode(event);
        
        JsonNode node = MAPPER.readTree(encoded);
        
        assertThat(node.get("@timestamp").textValue()).isEqualTo(Long.toString(timestamp));
    }
    
    @Test
    public void customMessagePattern() throws Exception {

        IAccessEvent event = mockBasicILoggingEvent();

        Context context = mock(Context.class);

        encoder.setMessagePattern("%requestURL %statusCode %bytesSent");
        encoder.getFieldNames().setMessage("msg");
        encoder.setContext(context);
        encoder.start();
        byte[] encoded = encoder.encode(event);

        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("msg").textValue()).isEqualTo("https://123.123.123.123/my/uri 200 123");

        encoder.stop();

        encoder.setMessagePattern(null);
        encoder.start();

        assertThat(MAPPER.readTree(encoder.encode(event)).get("msg").textValue())
                .startsWith("123.123.123.123 - remote-user ")
                .endsWith("\"https://123.123.123.123/my/uri\" 200 123");
    }

    private IAccessEvent mockBasicILoggingEvent() {
        IAccessEvent event = mock(IAccessEvent.class);
        when(event.getContentLength()).thenReturn(123L);
        when(event.getMethod()).thenReturn("GET");
        when(event.getProtocol()).thenReturn("HTTPS");
        when(event.getRemoteHost()).thenReturn("123.123.123.123");
        when(event.getRemoteUser()).thenReturn("remote-user");
        when(event.getRequestURI()).thenReturn("/my/uri");
        when(event.getRequestURL()).thenReturn("https://123.123.123.123/my/uri");
        when(event.getStatusCode()).thenReturn(200);
        when(event.getTimeStamp()).thenReturn(System.currentTimeMillis());
        when(event.getElapsedTime()).thenReturn(246L);
        when(event.getRequestHeaderMap()).thenReturn(new HashMap<String, String>() {{
            put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36");
            put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        }});
        when(event.getResponseHeaderMap()).thenReturn(new HashMap<String, String>() {{
            put("Content-Type", "text/html; charset=UTF-8");
            put("Content-Length", "42");
        }});
        return event;
    }
    
}
