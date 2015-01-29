package net.logstash.logback.encoder;

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
    private ILoggingEvent event;
    
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
    
    @Before
    public void setup() {
        when(formatter.getEncoding()).thenReturn("UTF-8");
    }
    
    @Test
    public void testNoPrefixNoSuffix() throws IOException {
        
        encoder.start();
        
        Assert.assertTrue(encoder.isStarted());
        
        verify(formatter).setContext(context);
        verify(formatter).start();
        
        encoder.init(outputStream);
        
        encoder.doEncode(event);
        
        verify(formatter).writeEventToOutputStream(event, outputStream);
        
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
        
        encoder.init(outputStream);
        
        verify(prefix).init(outputStream);
        verify(suffix).init(outputStream);
        
        encoder.doEncode(event);
        
        verify(prefix).doEncode(event);
        verify(suffix).doEncode(event);
        
        verify(formatter).writeEventToOutputStream(event, outputStream);
        
        Assert.assertEquals(System.getProperty("line.separator"), outputStream.toString("UTF-8"));
        
        encoder.close();
        verify(prefix).close();
        verify(suffix).close();
        
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
        
        encoder.init(bufferedOutputStream);
        
        encoder.doEncode(event);
        
        verify(formatter).writeEventToOutputStream(event, bufferedOutputStream);
        
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
    

}
