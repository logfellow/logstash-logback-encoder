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
package net.logstash.logback.encoder;

import static org.apache.commons.io.IOUtils.LINE_SEPARATOR;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import net.logstash.logback.Logback11Support;

import org.apache.commons.lang.time.FastDateFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.Context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Logback11Support.class)
public class LogstashAccessEncoderTest {
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private LogstashAccessEncoder encoder = new LogstashAccessEncoder();
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    
    @Test
    public void basicsAreIncluded_logback11() throws Exception {
        
        PowerMockito.mockStatic(Logback11Support.class);
        when(Logback11Support.isLogback11OrBefore()).thenReturn(true);
        
        encoder.init(outputStream);
        
        final long timestamp = System.currentTimeMillis();
        
        IAccessEvent event = mockBasicILoggingEvent();
        when(event.getTimeStamp()).thenReturn(timestamp);
        
        encoder.getFieldNames().setTimestamp("timestamp");
        
        encoder.start();
        encoder.doEncode(event);
        closeQuietly(outputStream);
        
        JsonNode node = MAPPER.readTree(outputStream.toByteArray());
        
        verifyBasics(timestamp, event, node);
        
    }

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
        assertThat(node.get("timestamp").textValue()).isEqualTo(FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ").format
                (timestamp));
        assertThat(node.get("@version").intValue()).isEqualTo(1);
        assertThat(node.get("@message").textValue()).isEqualTo(String.format("%s - %s [%s] \"%s\" %s %s", event.getRemoteHost(), event.getRemoteUser(),
                FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ").format
                        (event.getTimeStamp()), event.getRequestURL(), event.getStatusCode(),
                event.getContentLength()));
        
        assertThat(node.get("@fields.method").textValue()).isEqualTo(event.getMethod());
        assertThat(node.get("@fields.protocol").textValue()).isEqualTo(event.getProtocol());
        assertThat(node.get("@fields.status_code").asInt()).isEqualTo(event.getStatusCode());
        assertThat(node.get("@fields.requested_url").textValue()).isEqualTo(event.getRequestURL());
        assertThat(node.get("@fields.requested_uri").textValue()).isEqualTo(event.getRequestURI());
        assertThat(node.get("@fields.remote_host").textValue()).isEqualTo(event.getRemoteHost());
        assertThat(node.get("@fields.HOSTNAME").textValue()).isEqualTo(event.getRemoteHost());
        assertThat(node.get("@fields.remote_user").textValue()).isEqualTo(event.getRemoteUser());
        assertThat(node.get("@fields.content_length").asLong()).isEqualTo(event.getContentLength());
        assertThat(node.get("@fields.elapsed_time").asLong()).isEqualTo(event.getElapsedTime());
        assertThat(node.get("@fields.request_headers")).isNull();
        assertThat(node.get("@fields.response_headers")).isNull();
    }
    
    @Test
    public void closePutsSeparatorAtTheEnd() throws Exception {
        PowerMockito.mockStatic(Logback11Support.class);
        when(Logback11Support.isLogback11OrBefore()).thenReturn(true);
        
        encoder.init(outputStream);

        IAccessEvent event = mockBasicILoggingEvent();
        
        encoder.start();
        encoder.doEncode(event);
        encoder.close();
        closeQuietly(outputStream);
        
        assertThat(outputStream.toString()).endsWith(LINE_SEPARATOR);
    }
    
    @Test
    public void propertiesInContextAreIncluded() throws Exception {
        Map<String, String> propertyMap = new HashMap<String, String>();
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
        
        encoder.getFieldNames().setFieldsRequestHeaders("@fields.request_headers");
        encoder.getFieldNames().setFieldsResponseHeaders("@fields.response_headers");
        encoder.start();
        byte[] encoded = encoder.encode(event);
        
        JsonNode node = MAPPER.readTree(encoded);

        assertThat(node.get("@fields.request_headers").size()).isEqualTo(2);
        assertThat(node.get("@fields.request_headers").get("User-Agent").textValue()).isEqualTo("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36");
        assertThat(node.get("@fields.request_headers").get("Accept").textValue()).isEqualTo("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        assertThat(node.get("@fields.request_headers").get("Unknown")).isNull();
        
        assertThat(node.get("@fields.response_headers").size()).isEqualTo(2);
        assertThat(node.get("@fields.response_headers").get("Content-Type").textValue()).isEqualTo("text/html; charset=UTF-8");
        assertThat(node.get("@fields.response_headers").get("Content-Length").textValue()).isEqualTo("42");
        assertThat(node.get("@fields.response_headers").get("Unknown")).isNull();
    }
    
    @Test
    public void immediateFlushIsSane() {
        encoder.setImmediateFlush(true);
        assertThat(encoder.isImmediateFlush()).isEqualTo(true);
        
        encoder.setImmediateFlush(false);
        assertThat(encoder.isImmediateFlush()).isEqualTo(false);
    }
    
    @Test
    public void testCustomFields() throws Exception {
        String customFields = "{\"foo\":\"bar\"}";
        encoder.setCustomFields(customFields);
        assertThat(encoder.getCustomFields()).isEqualTo(customFields);
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
