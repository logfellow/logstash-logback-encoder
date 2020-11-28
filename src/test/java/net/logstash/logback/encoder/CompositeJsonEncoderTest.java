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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import net.logstash.logback.Logback11Support;
import net.logstash.logback.composite.CompositeJsonFormatter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.WarnStatus;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
public class CompositeJsonEncoderTest {
    
    private CompositeJsonFormatter<ILoggingEvent> formatter = mock(CompositeJsonFormatter.class);
    
    @InjectMocks
    private CompositeJsonEncoder<ILoggingEvent> encoder = new CompositeJsonEncoder<ILoggingEvent>() {

        @Override
        protected CompositeJsonFormatter<ILoggingEvent> createFormatter() {
            return formatter;
        }
    };
    
    @Mock(lenient = true)
    private Context context;
    
    @Mock
    private StatusManager statusManager;
    
    @Mock
    private ILoggingEvent event;
    
    @Mock
    private Logback11Support logback11Support;

    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
    
    @BeforeEach
    public void setup() {
        when(formatter.getEncoding()).thenReturn("UTF-8");
        when(context.getStatusManager()).thenReturn(statusManager);
        encoder.setLogback11Support(logback11Support);
    }
    
    @Test
    public void testNoPrefixNoSuffix_logback11() throws IOException {
        
        when(logback11Support.isLogback11OrBefore()).thenReturn(true);

        encoder.start();
        
        Assertions.assertTrue(encoder.isStarted());
        
        verify(formatter).setContext(context);
        verify(formatter).start();
        
        encoder.init(outputStream);
        
        encoder.doEncode(event);
        
        verify(formatter).writeEventToOutputStream(event, outputStream);
        
        Assertions.assertEquals(System.getProperty("line.separator"), outputStream.toString("UTF-8"));
        
        encoder.stop();
        Assertions.assertFalse(encoder.isStarted());
        verify(formatter).stop();
    }
    
    @Test
    public void testNoPrefixNoSuffix_logback12OrLater() throws IOException {
        
        encoder.start();
        
        Assertions.assertTrue(encoder.isStarted());
        
        verify(formatter).setContext(context);
        verify(formatter).start();
        
        byte[] encoded = encoder.encode(event);
        
        verify(formatter).writeEventToOutputStream(eq(event), any(OutputStream.class));
        
        assertThat(encoded).containsExactly(System.getProperty("line.separator").getBytes("UTF-8"));
        
        encoder.stop();
        Assertions.assertFalse(encoder.isStarted());
        verify(formatter).stop();
    }
    
    @Test
    public void testPrefixAndSuffix_logback11() throws IOException {
        
        when(logback11Support.isLogback11OrBefore()).thenReturn(true);
        
        LayoutWrappingEncoder<ILoggingEvent> prefix = mock(LayoutWrappingEncoder.class);
        Encoder<ILoggingEvent> suffix = mock(Encoder.class);
        
        encoder.setPrefix(prefix);
        encoder.setSuffix(suffix);
        
        encoder.start();
        
        Assertions.assertTrue(encoder.isStarted());
        
        verify(formatter).setContext(context);
        verify(formatter).start();
        
        verify(prefix).setCharset(StandardCharsets.UTF_8);
        verify(prefix).start();
        verify(suffix).start();
        
        encoder.init(outputStream);

        verify(logback11Support).init(prefix, outputStream);
        verify(logback11Support).init(suffix, outputStream);
        
        encoder.doEncode(event);
        
        verify(logback11Support).doEncode(prefix, event);
        verify(logback11Support).doEncode(suffix, event);
        
        verify(formatter).writeEventToOutputStream(event, outputStream);
        
        Assertions.assertEquals(System.getProperty("line.separator"), outputStream.toString("UTF-8"));
        
        encoder.close();
        verify(logback11Support).close(prefix);
        verify(logback11Support).close(suffix);
        
        encoder.stop();
        Assertions.assertFalse(encoder.isStarted());
        verify(formatter).stop();
        verify(prefix).stop();
        verify(suffix).stop();
    }
    
    @Test
    public void testPrefixAndSuffix_logback12OrLater() throws IOException {
        
        LayoutWrappingEncoder<ILoggingEvent> prefix = mock(LayoutWrappingEncoder.class);
        Encoder<ILoggingEvent> suffix = mock(Encoder.class);
        
        when(prefix.encode(event)).thenReturn("prefix".getBytes(StandardCharsets.UTF_8));
        when(suffix.encode(event)).thenReturn("suffix".getBytes(StandardCharsets.UTF_8));
        
        encoder.setPrefix(prefix);
        encoder.setSuffix(suffix);
        
        encoder.start();
        
        Assertions.assertTrue(encoder.isStarted());
        
        verify(formatter).setContext(context);
        verify(formatter).start();
        
        verify(prefix).setCharset(StandardCharsets.UTF_8);
        verify(prefix).start();
        verify(suffix).start();
        
        byte[] encoded = encoder.encode(event);
        
        verify(prefix).encode(event);
        verify(suffix).encode(event);
        
        verify(formatter).writeEventToOutputStream(eq(event), any(OutputStream.class));
        
        assertThat(encoded).containsExactly(("prefixsuffix" + System.getProperty("line.separator")).getBytes(StandardCharsets.UTF_8));
        
        encoder.stop();
        Assertions.assertFalse(encoder.isStarted());
        verify(formatter).stop();
        verify(prefix).stop();
        verify(suffix).stop();
    }
    
    @Test
    public void testNoImmediateFlush_logback11() throws IOException {
        
        encoder.setLogback11Support(logback11Support);
        when(logback11Support.isLogback11OrBefore()).thenReturn(true);

        encoder.setImmediateFlush(false);
        
        encoder.start();
        
        Assertions.assertTrue(encoder.isStarted());
        
        verify(formatter).setContext(context);
        verify(formatter).start();
        
        encoder.init(bufferedOutputStream);
        
        encoder.doEncode(event);
        
        verify(formatter).writeEventToOutputStream(event, bufferedOutputStream);
        
        Assertions.assertEquals("", outputStream.toString("UTF-8"));
        
        bufferedOutputStream.flush();
        
        Assertions.assertEquals(System.getProperty("line.separator"), outputStream.toString("UTF-8"));
        
        encoder.stop();
        Assertions.assertFalse(encoder.isStarted());
        verify(formatter).stop();
    }
    
    @Test
    public void testLineEndings() throws IOException {
        
        Assertions.assertEquals(System.getProperty("line.separator"), encoder.getLineSeparator());
        
        encoder.setLineSeparator("UNIX");
        Assertions.assertEquals("\n", encoder.getLineSeparator());
        
        encoder.setLineSeparator(null);
        Assertions.assertEquals(null, encoder.getLineSeparator());
        
        encoder.setLineSeparator("WINDOWS");
        Assertions.assertEquals("\r\n", encoder.getLineSeparator());
        
        encoder.setLineSeparator("foo");
        Assertions.assertEquals("foo", encoder.getLineSeparator());
        
        encoder.setLineSeparator("SYSTEM");
        Assertions.assertEquals(System.getProperty("line.separator"), encoder.getLineSeparator());
        
        encoder.setLineSeparator("");
        Assertions.assertEquals(null, encoder.getLineSeparator());
    }
    
    @Test
    public void testIOException_logback11() throws IOException {
        
        encoder.setLogback11Support(logback11Support);
        when(logback11Support.isLogback11OrBefore()).thenReturn(true);
        
        encoder.start();
        
        Assertions.assertTrue(encoder.isStarted());
        
        verify(formatter).setContext(context);
        verify(formatter).start();
        
        encoder.init(outputStream);
        
        IOException exception = new IOException();
        doThrow(exception).when(formatter).writeEventToOutputStream(event, outputStream);
        
        encoder.doEncode(event);
        
        Assertions.assertTrue(encoder.isStarted());
        
        verify(statusManager).add(new WarnStatus("Error encountered while encoding log event. "
                + "OutputStream is now in an unknown state, but will continue to be used for future log events."
                + "Event: " + event, context, exception));
    }

    @Test
    public void testIOException_logback12OrLater() throws IOException {
        
        encoder.start();
        
        Assertions.assertTrue(encoder.isStarted());
        
        verify(formatter).setContext(context);
        verify(formatter).start();
        
        IOException exception = new IOException();
        doThrow(exception).when(formatter).writeEventToOutputStream(eq(event), any(OutputStream.class));
        
        encoder.encode(event);
        
        Assertions.assertTrue(encoder.isStarted());
        
        verify(statusManager).add(new WarnStatus("Error encountered while encoding log event. "
                + "Event: " + event, context, exception));
    }

}
