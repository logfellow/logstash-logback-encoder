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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import net.logstash.logback.composite.CompositeJsonFormatter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.WarnStatus;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class CompositeJsonEncoderTest {
    
    private CompositeJsonFormatter<ILoggingEvent> formatter = mock(CompositeJsonFormatter.class);
    
    @InjectMocks
    private CompositeJsonEncoder<ILoggingEvent> encoder = new CompositeJsonEncoder<ILoggingEvent>() {

        @Override
        protected CompositeJsonFormatter<ILoggingEvent> createFormatter() {
            return formatter;
        }
    };
    
    @Mock
    private Context context;
    
    @Mock
    private StatusManager statusManager;
    
    @Mock
    private ILoggingEvent event;
    
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
    
    @Before
    public void setup() {
        when(formatter.getEncoding()).thenReturn("UTF-8");
        when(context.getStatusManager()).thenReturn(statusManager);
    }
    
    @Test
    public void testNoPrefixNoSuffix() throws IOException {
        
        encoder.start();
        
        Assert.assertTrue(encoder.isStarted());
        
        verify(formatter).setContext(context);
        verify(formatter).start();

        outputStream.write(encoder.encode(event));
        
        verify(formatter).writeEventAsString(event);
        
        Assert.assertEquals(System.getProperty("line.separator"), outputStream.toString("UTF-8"));
        
        encoder.stop();
        Assert.assertFalse(encoder.isStarted());
        verify(formatter).stop();
    }
    
    @Test
    public void testPrefixAndSuffix() throws IOException {
        
        LayoutWrappingEncoder<ILoggingEvent> prefix = mock(LayoutWrappingEncoder.class);
        Encoder<ILoggingEvent> suffix = mock(Encoder.class);
        
        encoder.setPrefix(prefix);
        encoder.setSuffix(suffix);
        
        encoder.start();
        
        Assert.assertTrue(encoder.isStarted());
        
        verify(formatter).setContext(context);
        verify(formatter).start();
        
        verify(prefix).setCharset(Charset.forName("UTF-8"));
        verify(prefix).start();
        verify(suffix).start();
        
        outputStream.write(encoder.encode(event));
        
        verify(prefix).encode(event);
        verify(suffix).encode(event);
        
        verify(formatter).writeEventAsString(event);
        
        Assert.assertEquals(System.getProperty("line.separator"), outputStream.toString("UTF-8"));
        
        encoder.stop();
        Assert.assertFalse(encoder.isStarted());
        verify(formatter).stop();
        verify(prefix).stop();
        verify(suffix).stop();
    }
    
    @Test
    public void testNoImmediateFlush() throws IOException {
        
        encoder.setImmediateFlush(false);
        
        encoder.start();
        
        Assert.assertTrue(encoder.isStarted());
        
        verify(formatter).setContext(context);
        verify(formatter).start();
        
        outputStream.write(encoder.encode(event));
        
        verify(formatter).writeEventAsString(event);
        
        Assert.assertEquals("", outputStream.toString("UTF-8"));
        
        bufferedOutputStream.flush();
        
        Assert.assertEquals(System.getProperty("line.separator"), outputStream.toString("UTF-8"));
        
        encoder.stop();
        Assert.assertFalse(encoder.isStarted());
        verify(formatter).stop();
    }
    
    @Test
    public void testLineEndings() throws IOException {
        
        Assert.assertEquals(System.getProperty("line.separator"), encoder.getLineSeparator());
        
        encoder.setLineSeparator("UNIX");
        Assert.assertEquals("\n", encoder.getLineSeparator());
        
        encoder.setLineSeparator(null);
        Assert.assertEquals(null, encoder.getLineSeparator());
        
        encoder.setLineSeparator("WINDOWS");
        Assert.assertEquals("\r\n", encoder.getLineSeparator());
        
        encoder.setLineSeparator("foo");
        Assert.assertEquals("foo", encoder.getLineSeparator());
        
        encoder.setLineSeparator("SYSTEM");
        Assert.assertEquals(System.getProperty("line.separator"), encoder.getLineSeparator());
        
        encoder.setLineSeparator("");
        Assert.assertEquals(null, encoder.getLineSeparator());
    }
    
    @Test
    public void testIOException() throws IOException {
        
        encoder.start();
        
        Assert.assertTrue(encoder.isStarted());
        
        verify(formatter).setContext(context);
        verify(formatter).start();
        
        IOException exception = new IOException();
        doThrow(exception).when(formatter).writeEventAsString(event);
        
        outputStream.write(encoder.encode(event));
        
        Assert.assertTrue(encoder.isStarted());
        
        verify(statusManager).add(new WarnStatus("Error encountered while encoding log event. "
                + "OutputStream is now in an unknown state, but will continue to be used for future log events."
                + "Event: " + event, context, exception));
    }

}
