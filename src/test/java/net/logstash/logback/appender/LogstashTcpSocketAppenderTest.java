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
package net.logstash.logback.appender;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import javax.net.SocketFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.util.Duration;

@RunWith(MockitoJUnitRunner.class)
public class LogstashTcpSocketAppenderTest {
    
    private static final int VERIFICATION_TIMEOUT = 1000 * 10;

    @InjectMocks
    private LogstashTcpSocketAppender appender;
    
    @Mock
    private Context context;
    
    @Mock
    private StatusManager statusManager;
    
    @Mock
    private ILoggingEvent event1;
    
    @Mock
    private ILoggingEvent event2;
    
    @Mock
    private SocketFactory socketFactory;
    
    @Mock
    private Socket socket;
    
    @Mock
    private OutputStream outputStream;
    
    @Mock
    private Encoder<ILoggingEvent> encoder;
    
    @Before
    public void setup() throws IOException {
        appender.setRemoteHost("localhost");
        appender.setPort(10000);
        when(context.getStatusManager()).thenReturn(statusManager);
        
        when(socket.getOutputStream()).thenReturn(outputStream);
        
    }
    
    @After
    public void tearDown() {
        appender.stop();
    }
    
    @Test
    public void testEncoderCalled() throws Exception {
        
        appender.setIncludeCallerData(true);
        
        when(socketFactory.createSocket()).thenReturn(socket);
        
        appender.start();
        
        verify(encoder).start();
        
        appender.append(event1);
        
        verify(event1).getCallerData();
        
        verify(encoder, timeout(VERIFICATION_TIMEOUT)).init(any(OutputStream.class));
        
        verify(encoder, timeout(VERIFICATION_TIMEOUT)).doEncode(event1);
        
    }

    @Test
    public void testReconnectOnOpen() throws Exception {
        
        appender.setReconnectionDelay(new Duration(100));
        
        when(socketFactory.createSocket())
            .thenThrow(new SocketTimeoutException())
            .thenReturn(socket);
        
        appender.start();
        
        verify(encoder).start();
        
        appender.append(event1);
        
        verify(encoder, timeout(VERIFICATION_TIMEOUT)).init(any(OutputStream.class));
        
        verify(encoder, timeout(VERIFICATION_TIMEOUT)).doEncode(event1);
    }

    @Test
    public void testReconnectOnWrite() throws Exception {
        
        appender.setReconnectionDelay(new Duration(100));
        
        when(socketFactory.createSocket()).thenReturn(socket);
        
        appender.start();
        
        verify(encoder).start();
        
        doThrow(new SocketException()).doNothing().when(encoder).doEncode(event1);
        
        appender.append(event1);
        
        verify(encoder, timeout(VERIFICATION_TIMEOUT).times(2)).init(any(OutputStream.class));
        
        verify(encoder, timeout(VERIFICATION_TIMEOUT).times(2)).doEncode(event1);
    }

}
