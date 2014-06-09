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

import static org.apache.commons.io.IOUtils.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.logstash.logback.LogstashFormatter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.Context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LogstashEncoderTest {
    
    private static Logger LOG = LoggerFactory.getLogger(LogstashEncoderTest.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private LogstashEncoder encoder;
    private ByteArrayOutputStream outputStream;
    
    @Before
    public void before() throws Exception {
        outputStream = new ByteArrayOutputStream();
        encoder = new LogstashEncoder();
        encoder.init(outputStream);
    }
    
    @Test
    public void basicsAreIncluded() throws Exception {
        final long timestamp = System.currentTimeMillis();
        
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getTimeStamp()).thenReturn(timestamp);
        
        encoder.doEncode(event);
        closeQuietly(outputStream);
        
        JsonNode node = MAPPER.readTree(outputStream.toByteArray());
        
        assertThat(node.get("@timestamp").textValue(), is(FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ").format
                (timestamp)));
        assertThat(node.get("@version").intValue(), is(1));
        assertThat(node.get("logger_name").textValue(), is("LoggerName"));
        assertThat(node.get("thread_name").textValue(), is("ThreadName"));
        assertThat(node.get("message").textValue(), is("My message"));
        assertThat(node.get("level").textValue(), is("ERROR"));
        assertThat(node.get("level_value").intValue(), is(40000));
    }
    
    @Test
    public void closePutsSeparatorAtTheEnd() throws Exception {
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        
        encoder.doEncode(event);
        encoder.close();
        closeQuietly(outputStream);
        
        assertThat(outputStream.toString(), Matchers.endsWith(LINE_SEPARATOR));
    }
    
    @Test
    public void includingThrowableProxyIncludesStackTrace() throws Exception {
        IThrowableProxy throwableProxy = new ThrowableProxy(new Exception("My goodness"));
        
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getThrowableProxy()).thenReturn(throwableProxy);
        
        encoder.doEncode(event);
        closeQuietly(outputStream);
        
        JsonNode node = MAPPER.readTree(outputStream.toByteArray());
        
        assertThat(node.get("stack_trace").textValue(), is(ThrowableProxyUtil.asString(throwableProxy)));
    }
    
    @Test
    public void propertiesInMDCAreIncluded() throws Exception {
        Map<String, String> mdcMap = new HashMap<String, String>();
        mdcMap.put("thing_one", "One");
        mdcMap.put("thing_two", "Three");
        
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getMDCPropertyMap()).thenReturn(mdcMap);
        
        encoder.doEncode(event);
        closeQuietly(outputStream);
        
        JsonNode node = MAPPER.readTree(outputStream.toByteArray());
        
        assertThat(node.get("thing_one").textValue(), is("One"));
        assertThat(node.get("thing_two").textValue(), is("Three"));
    }
    
    @Test
    public void nullMDCDoesNotCauseEverythingToBlowUp() throws Exception {
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getMDCPropertyMap()).thenReturn(null);
        
        encoder.doEncode(event);
        closeQuietly(outputStream);
    }
    
    @Test
    public void callerDataIsIncluded() throws Exception {
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        when(event.getMDCPropertyMap()).thenReturn(Collections.<String, String> emptyMap());
        final StackTraceElement[] stackTraceElements = { new StackTraceElement("caller_class", "method_name", "file_name", 12345) };
        when(event.getCallerData()).thenReturn(stackTraceElements);
        
        encoder.setIncludeCallerInfo(true);
        
        encoder.doEncode(event);
        closeQuietly(outputStream);
        
        JsonNode node = MAPPER.readTree(outputStream.toByteArray());
        
        assertThat(node.get("caller_class_name").textValue(), is(stackTraceElements[0].getClassName()));
        assertThat(node.get("caller_method_name").textValue(), is(stackTraceElements[0].getMethodName()));
        assertThat(node.get("caller_file_name").textValue(), is(stackTraceElements[0].getFileName()));
        assertThat(node.get("caller_line_number").intValue(), is(stackTraceElements[0].getLineNumber()));
    }
    
    @Test
    public void callerDataIsNotIncludedIfSwitchedOff() throws Exception {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLoggerName()).thenReturn("LoggerName");
        when(event.getThreadName()).thenReturn("ThreadName");
        when(event.getFormattedMessage()).thenReturn("My message");
        when(event.getLevel()).thenReturn(Level.ERROR);
        when(event.getMDCPropertyMap()).thenReturn(Collections.<String, String> emptyMap());
        final StackTraceElement[] stackTraceElements = { new StackTraceElement("caller_class", "method_name", "file_name", 12345) };
        when(event.getCallerData()).thenReturn(stackTraceElements);
        
        encoder.setIncludeCallerInfo(false);
        
        encoder.doEncode(event);
        closeQuietly(outputStream);
        
        JsonNode node = MAPPER.readTree(outputStream.toByteArray());
        assertThat(node.get("caller_class_name"), is(nullValue()));
        assertThat(node.get("caller_method_name"), is(nullValue()));
        assertThat(node.get("caller_file_name"), is(nullValue()));
        assertThat(node.get("caller_line_number"), is(nullValue()));
    }
    
    @Test
    public void propertiesInContextAreIncluded() throws Exception {
        Map<String, String> propertyMap = new HashMap<String, String>();
        propertyMap.put("thing_one", "One");
        propertyMap.put("thing_two", "Three");
        
        final Context context = mock(Context.class);
        when(context.getCopyOfPropertyMap()).thenReturn(propertyMap);
        
        ILoggingEvent event = mockBasicILoggingEvent(Level.ERROR);
        
        encoder.setContext(context);
        encoder.doEncode(event);
        closeQuietly(outputStream);
        
        JsonNode node = MAPPER.readTree(outputStream.toByteArray());
        
        assertThat(node.get("thing_one").textValue(), is("One"));
        assertThat(node.get("thing_two").textValue(), is("Three"));
    }
    
    @Test
    public void markerIncludesItselfAsTag() throws Exception {
        Marker marker = MarkerFactory.getMarker("hoosh");
        ILoggingEvent event = mockBasicILoggingEvent(Level.INFO);
        when(event.getMarker()).thenReturn(marker);
        
        encoder.doEncode(event);
        closeQuietly(outputStream);
        
        JsonNode node = MAPPER.readTree(outputStream.toByteArray());
        
        assertJsonArray(node.findValue("tags"), "hoosh");
    }
    
    @Test
    public void markerReferencesAreIncludedAsTags() throws Exception {
        Marker marker = MarkerFactory.getMarker("bees");
        marker.add(MarkerFactory.getMarker("knees"));
        ILoggingEvent event = mockBasicILoggingEvent(Level.INFO);
        when(event.getMarker()).thenReturn(marker);
        
        encoder.doEncode(event);
        closeQuietly(outputStream);
        
        JsonNode node = MAPPER.readTree(outputStream.toByteArray());
        
        assertJsonArray(node.findValue("tags"), "bees", "knees");
    }
    
    @Test
    public void nullMarkerIsIgnored() throws Exception {
        ILoggingEvent event = mockBasicILoggingEvent(Level.INFO);
        when(event.getMarker()).thenReturn(null);
        
        encoder.doEncode(event);
        closeQuietly(outputStream);
        
        JsonNode node = MAPPER.readTree(outputStream.toByteArray());
        
        assertJsonArray(node.findValue("tags"));
    }
   
    @Test
    public void markerIsJSON() throws Exception {
        Object[] argArray = new Object[] { 1, "hello", new HashMap<String, Object>() };
        Marker marker = MarkerFactory.getMarker("JSON");
        ILoggingEvent event = mockBasicILoggingEvent(Level.INFO);
        when(event.getMarker()).thenReturn(marker);
        when(event.getArgumentArray()).thenReturn(argArray);

        encoder.doEncode(event);
        closeQuietly(outputStream);
        
        JsonNode node = MAPPER.readTree(outputStream.toByteArray());
        
        assertThat(MAPPER.convertValue(argArray, JsonNode.class).equals(node.get("json_message")), is(true));
    } 

    @Test
    public void immediateFlushIsSane() {
        encoder.setImmediateFlush(true);
        assertThat(encoder.isImmediateFlush(), is(true));
        
        encoder.setImmediateFlush(false);
        assertThat(encoder.isImmediateFlush(), is(false));
    }
    
    @Test
    public void includeJsonChunk() throws Exception {
        String customFields = "{\"appname\":\"damnGodWebservice\",\"roles\":[\"customerorder\", \"auth\"], \"buildinfo\": { \"version\" : \"Version 0.1.0-SNAPSHOT\", \"lastcommit\" : \"75473700d5befa953c45f630c6d9105413c16fe1\"} }";
        ILoggingEvent event = mockBasicILoggingEvent(Level.INFO);
        
        encoder.setCustomFields(customFields);
        encoder.doEncode(event);
        closeQuietly(outputStream);
        
        JsonNode node = MAPPER.readTree(outputStream.toByteArray());
        
        assertThat(node.get("appname").textValue(), is("damnGodWebservice"));
        Assert.assertTrue(node.get("roles").equals(LogstashFormatter.parseCustomFields("[\"customerorder\", \"auth\"]")));
        Assert.assertTrue(node.get("buildinfo").equals(LogstashFormatter.parseCustomFields("{ \"version\" : \"Version 0.1.0-SNAPSHOT\", \"lastcommit\" : \"75473700d5befa953c45f630c6d9105413c16fe1\"}")));
    }

    @Test
    public void testContextMapWithNoArguments() throws Exception {
        ILoggingEvent event = mockBasicILoggingEvent(Level.INFO);
        when(event.getArgumentArray()).thenReturn(null);

        encoder.setEnableContextMap(true);
        encoder.doEncode(event);
        closeQuietly(outputStream);

        JsonNode node = MAPPER.readTree(outputStream.toByteArray());
        assertThat(node.get("message").textValue(), is("My message"));
    }

    @Test
    public void testContextMap() throws Exception {
        ILoggingEvent event = mockBasicILoggingEvent(Level.INFO);

        Map<String, Object> contextMap = new HashMap<String, Object>();
        contextMap.put("duration", 1200);
        contextMap.put("remoteResponse", "OK");
        contextMap.put("extra", Collections.singletonMap("extraEntry", "extraValue"));

        Object[] argList = new Object[] {
                "firstParamThatShouldBeIgnored",
                Collections.singletonMap("ignoredMapEntry", "whatever"),
                contextMap
        };

        when(event.getArgumentArray()).thenReturn(argList);

        encoder.setEnableContextMap(true);
        encoder.doEncode(event);
        closeQuietly(outputStream);

        JsonNode node = MAPPER.readTree(outputStream.toByteArray());
        assertThat(node.get("duration"), notNullValue());
        assertThat(node.get("duration").intValue(), is(1200));
        assertThat(node.get("remoteResponse"), notNullValue());
        assertThat(node.get("remoteResponse").textValue(), is("OK"));
        assertThat(node.get("extra"), notNullValue());
        assertThat(node.get("extra").get("extraEntry"), notNullValue());
        assertThat(node.get("extra").get("extraEntry").textValue(), is("extraValue"));

        assertThat("The second map from the end should be ignored", node.get("ignoredMapEntry"), nullValue());
    }

    
    @Test
    public void testEncoderConfiguration() throws Exception {
        // Empty the log file
        PrintWriter writer = new PrintWriter(System.getProperty("java.io.tmpdir") + "/test.log");
        writer.print("");
        writer.close();
        LOG.info("Testing info logging.");
        InputStream is = new FileInputStream(System.getProperty("java.io.tmpdir") + "/test.log");
        
        List<String> lines = IOUtils.readLines(is);
        JsonNode node = MAPPER.readTree(lines.get(0).getBytes("UTF-8"));
        
        assertThat(node.get("appname").textValue(), is("damnGodWebservice"));
        Assert.assertTrue(node.get("roles").equals(LogstashFormatter.parseCustomFields("[\"customerorder\", \"auth\"]")));
        Assert.assertTrue(node.get("buildinfo").equals(LogstashFormatter.parseCustomFields("{ \"version\" : \"Version 0.1.0-SNAPSHOT\", \"lastcommit\" : \"75473700d5befa953c45f630c6d9105413c16fe1\"}")));
    }
    
    private void assertJsonArray(JsonNode jsonNode, String... expected) {
        String[] values = new String[jsonNode.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = jsonNode.get(i).asText();
        }
        
        Assert.assertArrayEquals(expected, values);
    }
    
    private ILoggingEvent mockBasicILoggingEvent(Level level) {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLoggerName()).thenReturn("LoggerName");
        when(event.getThreadName()).thenReturn("ThreadName");
        when(event.getFormattedMessage()).thenReturn("My message");
        when(event.getLevel()).thenReturn(level);
        return event;
    }

}
