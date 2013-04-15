/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package net.logstash.logback.encoder;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import static org.apache.commons.io.IOUtils.*;
import org.apache.commons.lang.time.FastDateFormat;
import static org.hamcrest.MatcherAssert.*;
import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

/**
 * Test the logstash logback encoder
 */
public class LogstashEncoderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final int LEVEL_VALUE = 40000;
    private LogstashEncoder encoder;
    private ByteArrayOutputStream outputStream;

    /**
     *
     * @throws Exception while closing the outputstream
     */
    @Before
    public void before() throws Exception {
        outputStream = new ByteArrayOutputStream();
        encoder = new LogstashEncoder();
        encoder.init(outputStream);
    }

    /**
     *
     * @throws Exception while closing the outputstream
     */
    @Test
    public void basicsAreIncluded() throws Exception {
        final long timestamp = System.currentTimeMillis();

        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLoggerName()).thenReturn("LoggerName");
        when(event.getThreadName()).thenReturn("ThreadName");
        when(event.getFormattedMessage()).thenReturn("My message");
        when(event.getLevel()).thenReturn(Level.ERROR);
        when(event.getTimeStamp()).thenReturn(timestamp);

        encoder.doEncode(event);
        closeQuietly(outputStream);

        JsonNode node = MAPPER.readTree(outputStream.toByteArray());

        assertThat(
                node.get("@timestamp").textValue(),
                is(FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ").format(timestamp)));
        assertThat(node.get("@fields").get("logger_name").textValue(), is("LoggerName"));
        assertThat(node.get("@fields").get("thread_name").textValue(), is("ThreadName"));
        assertThat(node.get("@message").textValue(), is("My message"));
        assertThat(node.get("@fields").get("level").textValue(), is("ERROR"));
        assertThat(node.get("@fields").get("level_value").intValue(), is(LEVEL_VALUE));
    }

    /**
     *
     * @throws Exception while closing the outputstream
     */
    @Test
    public void closePutsSeparatorAtTheEnd() throws Exception {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLoggerName()).thenReturn("LoggerName");
        when(event.getThreadName()).thenReturn("ThreadName");
        when(event.getMessage()).thenReturn("My message");
        when(event.getLevel()).thenReturn(Level.ERROR);

        encoder.doEncode(event);
        encoder.close();
        closeQuietly(outputStream);

        assertThat(outputStream.toString(), Matchers.endsWith(LINE_SEPARATOR));
    }

    /**
     *
     * @throws Exception while closing the outputstream
     */
    @Test
    public void includingThrowableProxyIncludesStackTrace() throws Exception {
        IThrowableProxy throwableProxy = new ThrowableProxy(new Exception("My goodness"));

        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLoggerName()).thenReturn("LoggerName");
        when(event.getThreadName()).thenReturn("ThreadName");
        when(event.getFormattedMessage()).thenReturn("My message");
        when(event.getLevel()).thenReturn(Level.ERROR);
        when(event.getThrowableProxy()).thenReturn(throwableProxy);

        encoder.doEncode(event);
        closeQuietly(outputStream);

        JsonNode node = MAPPER.readTree(outputStream.toByteArray());

        assertThat(
                node.get("@fields").get("stack_trace").textValue(),
                is(ThrowableProxyUtil.asString(throwableProxy)));
    }

    /**
     *
     * @throws Exception while closing the outputstream
     */
    @Test
    public void propertiesInMDCAreIncluded() throws Exception {
        Map<String, String> mdcMap = new HashMap<String, String>();
        mdcMap.put("thing_one", "One");
        mdcMap.put("thing_two", "Three");

        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLoggerName()).thenReturn("LoggerName");
        when(event.getThreadName()).thenReturn("ThreadName");
        when(event.getFormattedMessage()).thenReturn("My message");
        when(event.getLevel()).thenReturn(Level.ERROR);
        when(event.getMDCPropertyMap()).thenReturn(mdcMap);

        encoder.doEncode(event);
        closeQuietly(outputStream);

        JsonNode node = MAPPER.readTree(outputStream.toByteArray());

        assertThat(node.get("@fields").get("thing_one").textValue(), is("One"));
        assertThat(node.get("@fields").get("thing_two").textValue(), is("Three"));
    }

    /**
     *
     * @throws Exception while closing the outputstream
     */
    @Test
    public void nullMDCDoesNotCauseEverythingToBlowUp() throws Exception {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLoggerName()).thenReturn("LoggerName");
        when(event.getThreadName()).thenReturn("ThreadName");
        when(event.getFormattedMessage()).thenReturn("My message");
        when(event.getLevel()).thenReturn(Level.ERROR);
        when(event.getMDCPropertyMap()).thenReturn(null);

        encoder.doEncode(event);
        closeQuietly(outputStream);
    }
}
