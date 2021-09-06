/*
 * Copyright 2013-2021 the original author or authors.
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import net.logstash.logback.TestJsonProvider;
import net.logstash.logback.composite.CompositeJsonFormatter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.EncoderBase;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.WarnStatus;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
public class CompositeJsonEncoderTest {
    
    @InjectMocks
    private final TestCompositeJsonEncoder encoder = new TestCompositeJsonEncoder();
    
    private CompositeJsonFormatter<ILoggingEvent> formatter;
    
    @Mock(lenient = true)
    private Context context;
    
    @Mock
    private StatusManager statusManager;
    
    @Mock
    private ILoggingEvent event;
    
    @BeforeEach
    public void setup() {
        // suppress line separator to make test platform independent
        this.encoder.setLineSeparator("");
        this.formatter = encoder.getFormatter();
        
        when(context.getStatusManager()).thenReturn(statusManager);
    }
    
    
    @Test
    public void startStop() {
        Encoder<ILoggingEvent> prefix = spy(new TestEncoder("prefix"));
        encoder.setPrefix(prefix);
        
        // stopped by default
        assertThat(encoder.isStarted()).isFalse();
        assertThat(formatter.isStarted()).isFalse();
        assertThat(prefix.isStarted()).isFalse();
        
        // start encoder
        encoder.start();
        assertThat(encoder.isStarted()).isTrue();
        assertThat(formatter.isStarted()).isTrue();
        assertThat(prefix.isStarted()).isTrue();
        verify(formatter).setContext(context);
        
        // providers are not started a second time
        encoder.start();
        verify(formatter, times(1)).start();
        verify(prefix, times(1)).start();
        
        // stop encoder
        encoder.stop();
        assertThat(encoder.isStarted()).isFalse();
        assertThat(formatter.isStarted()).isFalse();
        assertThat(prefix.isStarted()).isFalse();
        
        // providers are not stopped a second time
        encoder.stop();
        verify(formatter, times(1)).stop();
        verify(prefix, times(1)).stop();
    }

    
    @Test
    public void notStarted() {
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> encoder.encode(event))
                .withMessage("Encoder is not started");
    }
    
    
    @Test
    public void encode_noPrefixSuffix() {
        encoder.start();
        assertThat(new String(encoder.encode(event))).isEqualTo("{}");
    }

    
    /*
     * Encode log event with prefix and suffix encoders.
     * 
     * NOTE: Encoder#headerBytes and Encoder#footerBytes are ignored
     */
    @Test
    public void encode_withPrefixSuffix() {
        encoder.setPrefix(new TestEncoder("prefix"));
        encoder.setSuffix(new TestEncoder("suffix"));
        encoder.start();
        
        assertThat(new String(encoder.encode(event))).isEqualTo("prefix/event{}suffix/event");
    }
    
    
    /*
     * Use a custom line separator
     */
    @Test
    public void encode_customLineSeparator() {
        encoder.setLineSeparator("-");
        encoder.start();
        assertThat(new String(encoder.encode(event))).isEqualTo("{}-");
    }
    
    
    /*
     * Prefix/Suffix of type LayoutWrappingEncoder have their charset set to the same value
     * as the Formatter used by the CompositeJsonEncoder
     */
    @Test
    public void charsetOnLayoutWrappingEncoder() {
        formatter.setEncoding(JsonEncoding.UTF16_BE.getJavaName()); // use an encoding that is not likely to be
                                                                    // the default to avoid false positives
        
        LayoutWrappingEncoder<ILoggingEvent> prefix = mock(LayoutWrappingEncoder.class);
        encoder.setPrefix(prefix);
        
        encoder.start();
        
        verify(prefix).setCharset(StandardCharsets.UTF_16BE);
    }
    
    
    /*
     * Encode using the StreamingEncoder API
     */
    @Test
    public void streamingEncode() {
        encoder.start();
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        assertThatNoException().isThrownBy(() -> encoder.encode(event, bos));
        
        assertThat(new String(bos.toByteArray())).isEqualTo("{}");
    }
    
    
    /*
     * Test decoding of special line separators
     */
    @Test
    public void testLineEndings() {
        // Use a brand new default instance to get rid of configuration done by the #setup() method
        TestCompositeJsonEncoder encoder = new TestCompositeJsonEncoder();
        
        assertThat(encoder.getLineSeparator()).isEqualTo(System.getProperty("line.separator"));
        
        encoder.setLineSeparator("UNIX");
        assertThat(encoder.getLineSeparator()).isEqualTo("\n");
        
        encoder.setLineSeparator(null);
        assertThat(encoder.getLineSeparator()).isNull();
        
        encoder.setLineSeparator("WINDOWS");
        assertThat(encoder.getLineSeparator()).isEqualTo("\r\n");
        
        encoder.setLineSeparator("foo");
        assertThat(encoder.getLineSeparator()).isEqualTo("foo");
        
        encoder.setLineSeparator("SYSTEM");
        assertThat(encoder.getLineSeparator()).isEqualTo(System.getProperty("line.separator"));
        
        encoder.setLineSeparator("");
        assertThat(encoder.getLineSeparator()).isNull();
    }

    
    /*
     * Failure to encode event should log an warning status
     */
    @Test
    public void testIOException() throws IOException {
    encoder.exceptionToThrow = new IOException();
        encoder.start();
        
        encoder.encode(event);
        
        verify(statusManager).add(new WarnStatus("Error encountered while encoding log event. "
                + "Event: " + event, context, encoder.exceptionToThrow));
    }

    
    /*
     * StreamingEncoder re-throws the IOException to the caller and does not log any warning
     */
    @Test
    public void testIOException_streaming() throws IOException {
        encoder.start();
        
        IOException exception = new IOException();
        
        OutputStream stream = mock(OutputStream.class);
        doThrow(exception).when(stream).write(any(byte[].class), any(int.class), any(int.class));
        
        assertThatCode(() -> encoder.encode(event, stream)).isInstanceOf(IOException.class);
        
        verify(statusManager, never()).add(new WarnStatus("Error encountered while encoding log event. "
                + "Event: " + event, context, exception));
    }
    
    
    // ----------------------------------------------------------------------------------------------------------------
    
    
    private static class TestCompositeJsonEncoder extends CompositeJsonEncoder<ILoggingEvent> {
    private IOException exceptionToThrow;
    
        @Override
        protected CompositeJsonFormatter<ILoggingEvent> createFormatter() {
            CompositeJsonFormatter<ILoggingEvent> formatter = spy(new CompositeJsonFormatter<ILoggingEvent>(this) {
                @Override
                protected void writeEventToGenerator(JsonGenerator generator, ILoggingEvent event) throws IOException {
                    if (exceptionToThrow != null) {
                        throw exceptionToThrow;
                    }
                    super.writeEventToGenerator(generator, event);
                }
            });
            formatter.getProviders().addProvider(new TestJsonProvider());
            return formatter;
        }
    }
    
    private static class TestEncoder extends EncoderBase<ILoggingEvent> {
        private final String name;
        
        private TestEncoder(String name) {
            this.name = name;
        }
        
        public byte[] encode(ILoggingEvent event) {
            return getBytes(name + "/event");
        }

        public byte[] footerBytes()  {
           return getBytes(name + "/footer");
        }

        public byte[] headerBytes()  {
            return getBytes(name + "/header");
        }
        
        private byte[] getBytes(String s) {
            return s.getBytes();
        }
    }
}
