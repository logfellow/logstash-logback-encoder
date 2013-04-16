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
import ch.qos.logback.core.CoreConstants;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import static org.hamcrest.MatcherAssert.*;
import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class LogstashEncoderTest {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
    private static JsonReaderFactory parser = Json.createReaderFactory(null);
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

        JsonObject node = createObject(outputStream.toString());

        assertThat(
                node.getString("@timestamp"),
                is(DATE_FORMAT.format(timestamp)));
        assertThat(node.getString("@message"), is("My message"));
        assertThat(node.getJsonObject("@fields").getString("logger_name"), is("LoggerName"));
        assertThat(node.getJsonObject("@fields").getString("thread_name"), is("ThreadName"));
        assertThat(node.getJsonObject("@fields").getString("level"), is("ERROR"));
        assertThat(node.getJsonObject("@fields").getInt("level_value"), is(LEVEL_VALUE));
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
        when(event.getFormattedMessage()).thenReturn("My message");
        when(event.getLevel()).thenReturn(Level.ERROR);

        encoder.doEncode(event);
        encoder.close();
        closeQuietly(outputStream);

        assertThat(outputStream.toString(), Matchers.endsWith(CoreConstants.LINE_SEPARATOR));
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

        JsonObject node = createObject(outputStream.toString());

        assertThat(
                node.getJsonObject("@fields").getString("stack_trace"),
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

        JsonObject node = createObject(outputStream.toString());

        assertThat(node.getJsonObject("@fields").getString("thing_one"), is("One"));
        assertThat(node.getJsonObject("@fields").getString("thing_two"), is("Three"));
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

    /**
     * Closes a stream an swallows the exception.
     *
     * @param closeable stream to close
     */
    private void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }

    private JsonObject createObject(String input) {
        JsonReader reader = parser.createReader(new StringReader(input));
        return reader.readObject();
    }
}
