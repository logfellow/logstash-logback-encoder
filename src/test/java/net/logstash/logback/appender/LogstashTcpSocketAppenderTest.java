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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.concurrent.Future;

import javax.net.SocketFactory;

import net.logstash.logback.Logback11Support;
import net.logstash.logback.encoder.SeparatorParser;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.util.Duration;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Logback11Support.class)
public class LogstashTcpSocketAppenderTest {
    
    private static final int VERIFICATION_TIMEOUT = 1000 * 10;

    @InjectMocks
    private LogstashTcpSocketAppender appender = new TestableLogstashTcpSocketAppender();
    
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

    @Mock
    private Future<?> readableRunnableFuture;
    
    private class TestableLogstashTcpSocketAppender extends LogstashTcpSocketAppender {
        @Override
        protected Future<?> scheduleReaderRunnable(Runnable readerRunnable) {
            return readableRunnableFuture;
        }
    }
    
    @Before
    public void setup() throws IOException {
        when(context.getStatusManager()).thenReturn(statusManager);
        when(socketFactory.createSocket()).thenReturn(socket);
        when(socket.getOutputStream()).thenReturn(outputStream);
        when(encoder.encode(event1)).thenReturn("event1".getBytes("UTF-8"));
    }
    
    @After
    public void tearDown() {
        appender.stop();
    }
    
    @Test
    public void testEncoderCalled_logback11() throws Exception {
        PowerMockito.mockStatic(Logback11Support.class);
        when(Logback11Support.isLogback11OrBefore()).thenReturn(true);
        
        appender.addDestination("localhost:10000");
        appender.setIncludeCallerData(true);
        
        appender.start();
        
        verify(encoder).start();
        
        appender.append(event1);
        
        verify(event1).getCallerData();
        
        PowerMockito.verifyStatic(timeout(VERIFICATION_TIMEOUT));
        Logback11Support.init(eq(encoder), any(OutputStream.class));
        
        PowerMockito.verifyStatic(timeout(VERIFICATION_TIMEOUT));
        Logback11Support.doEncode(encoder, event1);

    }

    @Test
    public void testEncoderCalled_logback12OrLater() throws Exception {
        appender.addDestination("localhost:10000");
        appender.setIncludeCallerData(true);
        
        appender.start();
        
        verify(encoder).start();
        
        appender.append(event1);
        
        verify(event1).getCallerData();
        
        verify(encoder, timeout(VERIFICATION_TIMEOUT)).encode(event1);
    }

    @Test
    public void testReconnectOnOpen() throws Exception {
        appender.addDestination("localhost:10000");
        appender.setReconnectionDelay(new Duration(100));
        
        reset(socketFactory);
        when(socketFactory.createSocket())
            .thenThrow(new SocketTimeoutException())
            .thenReturn(socket);
        
        appender.start();
        
        verify(encoder).start();
        
        appender.append(event1);
        
        verify(encoder, timeout(VERIFICATION_TIMEOUT)).encode(event1);
    }

    @Test
    public void testReconnectOnWrite() throws Exception {
        appender.addDestination("localhost:10000");
        appender.setReconnectionDelay(new Duration(100));
        
        appender.start();
        
        verify(encoder).start();
        
        doThrow(new RuntimeException()).doReturn("event1".getBytes("UTF-8")).when(encoder).encode(event1);
        
        appender.append(event1);
        
        verify(encoder, timeout(VERIFICATION_TIMEOUT).times(2)).encode(event1);
    }

    @Test
    public void testReconnectOnReadFailure() throws Exception {
        
        appender.addDestination("localhost:10000");
        appender.setReconnectionDelay(new Duration(100));
        
        when(readableRunnableFuture.isDone())
            /*
             * First return true, so that the reconnect logic is executed 
             */
            .thenReturn(true)
            /*
             * Then return false so that the event can be written
             */
            .thenReturn(false);
        
        appender.start();
        
        verify(encoder).start();
        
        appender.append(event1);
        
        verify(encoder, timeout(VERIFICATION_TIMEOUT)).encode(event1);
    }


    /**
     * Scenario:
     *   Two servers: localhost:10000 (primary), localhost:10001 (secondary)
     *   Primary is available at startup
     *   Appender should connect to PRIMARY and not any secondaries
     *   
     * @throws Exception
     */
    @Test
    public void testConnectOnPrimary() throws Exception {
        appender.addDestination("localhost:10000");
        appender.addDestination("localhost:10001");

        appender.start();
        verify(encoder).start();

        // Only one socket should have been created
        verify(socket, timeout(VERIFICATION_TIMEOUT).times(1)).connect(any(SocketAddress.class), anyInt());

        // The only socket should be connected to primary
        verify(socket).connect(host("localhost", 10000), anyInt());
    }
    
    
    /**
     * Scenario:
     *   Two servers: localhost:10000 (primary), localhost:10001 (secondary)
     *   Primary is not available at startup
     *   Appender should first try primary then immediately connect to secondary
     *   
     * @throws Exception
     */
    @Test
    public void testReconnectToSecondaryOnOpen() throws Exception {
        appender.addDestination("localhost:10000");
        appender.addDestination("localhost:10001");

        // Make it failed to connect to primary
        doThrow(SocketTimeoutException.class)
            .when(socket).connect(host("localhost", 10000), anyInt());

        // Start the appender and verify it is actually started.
        // It should try to connect to primary, fail then retry on secondary.
        appender.start();
        verify(encoder).start();

        // TWO connection attempts must have been made (without delay)
        verify(socket, timeout(VERIFICATION_TIMEOUT).times(2)).connect(any(SocketAddress.class), anyInt());
        InOrder inOrder = inOrder(socket);

        // 1) First attempt on PRIMARY: failure
        inOrder.verify(socket).connect(host("localhost", 10000), anyInt());

        // 2) Second attempt on SECONDARY
        inOrder.verify(socket).connect(host("localhost", 10001), anyInt());
    }
    
    
    /**
     * Scenario:
     *   Two servers: localhost:10000 (primary), localhost:10001 (secondary)
     *   Primary is available at startup then fails after the first event.
     *   Appender should then connect on secondary for the next event.
     *   
     * @throws Exception
     */
    @Test
    public void testReconnectToSecondaryOnWrite() throws Exception {
        appender.addDestination("localhost:10000");
        appender.addDestination("localhost:10001");

        // Primary accepts first connection attempt then refuses
        doNothing()
            .doThrow(SocketTimeoutException.class)
            .when(socket).connect(host("localhost", 10000), anyInt());
        
        // First attempt of sending the event throws an exception while subsequent 
        // attempts will succeed. This should force the appender to close the connection
        // and attempt to reconnect starting from the first host of the list.
        doThrow(new RuntimeException())
            .doReturn("event1".getBytes("UTF-8"))
            .when(encoder).encode(event1);
        
        
        // Start the appender and verify it is actually started
        // At this point, it should be connected to primary.
        appender.start();
        verify(encoder).start();

        appender.append(event1);


        // THREE connection attempts must have been made in total
        verify(socket, timeout(VERIFICATION_TIMEOUT).times(3)).connect(any(SocketAddress.class), anyInt());
        InOrder inOrder = inOrder(socket);

        // 1) connected to primary at startup
        inOrder.verify(socket).connect(host("localhost", 10000), anyInt());

        // 2) retry on primary after failed attempt to send event
        inOrder.verify(socket).connect(host("localhost", 10000), anyInt());

        // 3) connect to secondary
        inOrder.verify(socket).connect(host("localhost", 10001), anyInt());
    }
    
    
    /**
     * Make sure the appender tries to reconnect to primary after a while.
     */
    @Test
    public void testReconnectToPrimaryWhileOnSecondary() throws Exception {
        appender.addDestination("localhost:10000");
        appender.addDestination("localhost:10001");
        appender.setSecondaryConnectionTTL(Duration.buildByMilliseconds(100));
        
        // Primary refuses first connection to force the appender to go on the secondary.
        doThrow(SocketTimeoutException.class)
            .doNothing()
            .when(socket).connect(host("localhost", 10000), anyInt());
        
        
        // Start the appender and verify it is actually started
        // At this point, it should be connected to primary.
        appender.start();
        verify(encoder).start();

        
        // The appender is supposed to be on the secondary.
        // Wait until after the appender is supposed to reattempt to connect to primary, then
        // send an event (requires some activity to trigger the reconnection process).
        Thread.sleep(appender.getSecondaryConnectionTTL().getMilliseconds() + 50);
        appender.append(event1);


        // THREE connection attempts must have been made in total
        verify(socket, timeout(VERIFICATION_TIMEOUT).times(3)).connect(any(SocketAddress.class), anyInt());
        InOrder inOrder = inOrder(socket, encoder);

        // 1) fail to connect on primary at startup
        inOrder.verify(socket).connect(host("localhost", 10000), anyInt());

        // 2) connect to secondary
        inOrder.verify(socket).connect(host("localhost", 10001), anyInt());

        // 3) send the event
        inOrder.verify(encoder).encode(event1);

        // 4) connect to primary
        inOrder.verify(socket).connect(host("localhost", 10000), anyInt());
    }
    
    
    /**
     * When a connection failure occurs, the appender retries immediately with the next 
     * available host. When all hosts are exhausted, the appender should wait {reconnectionDelay}
     * before retrying with the first server.
     */
    @Test
    public void testReconnectWaitWhenExhausted() throws Exception {
        
        appender.addDestination("localhost:10000");
        appender.addDestination("localhost:10001");
        appender.setReconnectionDelay(Duration.buildByMilliseconds(100));
        
        // Both hosts refuse the first connection attempt
        doThrow(SocketTimeoutException.class)
            .doNothing()
            .when(socket).connect(host("localhost", 10000), anyInt());
        doThrow(SocketTimeoutException.class)
            .doNothing()
            .when(socket).connect(host("localhost", 10001), anyInt());
        
        // Start the appender and verify it is actually started
        // At this point, it should be connected to primary.
        appender.start();
        verify(encoder).start();
        
        
        // THREE connection attempts must have been made in total
        verify(socket, timeout(appender.getReconnectionDelay().getMilliseconds()+50).times(3)).connect(any(SocketAddress.class), anyInt());
        InOrder inOrder = inOrder(socket, encoder);

        // 1) fail to connect on primary at startup
        inOrder.verify(socket).connect(host("localhost", 10000), anyInt());

        // 2) connect to secondary
        inOrder.verify(socket).connect(host("localhost", 10001), anyInt());

        // 3) connect to primary
        inOrder.verify(socket).connect(host("localhost", 10000), anyInt());
    }
    
    
    /**
     * Schedule keep alive and make sure we got the expected amount of messages
     * in the given time.
     */
    @Test
    public void testKeepAlive() throws Exception {

        appender.addDestination("localhost");

        // Schedule keepalive message every 100ms
        appender.setKeepAliveMessage("UNIX");
        appender.setKeepAliveCharset(Charset.forName("UTF-8"));
        appender.setKeepAliveDuration(Duration.buildByMilliseconds(100));

        String expectedKeepAlives = SeparatorParser.parseSeparator("UNIX") + SeparatorParser.parseSeparator("UNIX");
        byte[] expectedKeepAlivesBytes = expectedKeepAlives.getBytes("UTF-8");

        // Use a ByteArrayOutputStream to capture the actual keep alive message bytes
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        when(socket.getOutputStream())
            .thenReturn(bos);


        // Start the appender and verify it is actually started
        // At this point, it should be connected to primary.
        appender.start();
        verify(encoder).start();

        // Wait for a bit more than 2 keep alive messages then make sure we got the expected content
        Thread.sleep(250);
        Assert.assertArrayEquals(expectedKeepAlivesBytes, bos.toByteArray());
    }
    
    
    /**
     * Make sure keep alive messages trigger reconnect to another host upon failure.
     * 
     */
    @Test
    public void testReconnectToSecondaryOnKeepAlive() throws Exception {

        appender.addDestination("localhost:10000");
        appender.addDestination("localhost:10001");

        // Schedule keep alive message every 100ms
        appender.setKeepAliveMessage("UNIX");
        appender.setKeepAliveDuration(Duration.buildByMilliseconds(100));
        
        // Primary accepts first connection then refuse subsequent attemps
        doNothing()
            .doThrow(SocketTimeoutException.class)
            .when(socket).connect(host("localhost", 10000), anyInt());
        
        // Throw an exception the first time the the appender attempts to write in the output stream.
        // This should cause the appender to initiate the reconnect sequence.
        doThrow(SocketException.class)
            .doNothing()
            .when(outputStream).write(any(byte[].class), anyInt(), anyInt());
        
        // Start the appender and verify it is actually started
        // At this point, it should be connected to primary.
        appender.start();
        verify(encoder).start();

        // Wait for a bit more than a single keep alive message.
        // THREE connection attempts must have been made in total:
        verify(socket, timeout(VERIFICATION_TIMEOUT).times(3)).connect(any(SocketAddress.class), anyInt());
        InOrder inOrder = inOrder(socket);

        // 1) connected to primary at startup
        inOrder.verify(socket).connect(host("localhost", 10000), anyInt());

        // 2) retry on primary after failed attempt to send event
        inOrder.verify(socket).connect(host("localhost", 10000), anyInt());

        // 3) connect to secondary
        inOrder.verify(socket).connect(host("localhost", 10001), anyInt());
    }
    
    
    /**
     * At least one valid destination must be configured. 
     * The appender refuses to start in case of error.
     */
    @Test
    public void testDestination_None() throws Exception {
        appender.start();
        Assert.assertFalse(appender.isStarted());
    }
    
    
    /**
     * Specify destinations using both <remoteHost>/<port> and <destination>.
     * Only one scheme can be used - make sure the appender refuses to start.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testDestination_MixedType() throws Exception {
        appender.setRemoteHost("localhost");
        appender.setPort(10000);
        appender.addDestination("localhost:10001");
        
        appender.start();
        Assert.assertFalse(appender.isStarted());
    }
    
    private SocketAddress host(final String host, final int port) {
    	return argThat(hasHostAndPort(host, port));
    }
    
    private ArgumentMatcher<SocketAddress> hasHostAndPort(final String host, final int port) {
    	return new ArgumentMatcher<SocketAddress>() {

			@Override
			public boolean matches(Object argument) {
				InetSocketAddress sockAddr = (InetSocketAddress) argument;
				return host.equals(sockAddr.getHostName()) && port==sockAddr.getPort();
			}

    	};
    }
}
