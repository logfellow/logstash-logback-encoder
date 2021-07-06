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
package net.logstash.logback.appender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
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
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import javax.net.SocketFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.util.Duration;
import net.logstash.logback.appender.destination.RandomDestinationConnectionStrategy;
import net.logstash.logback.appender.destination.RoundRobinDestinationConnectionStrategy;
import net.logstash.logback.appender.listener.TcpAppenderListener;
import net.logstash.logback.encoder.SeparatorParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LogstashTcpSocketAppenderTest {
    
    private static final int VERIFICATION_TIMEOUT = 1000 * 10;

    @InjectMocks
    private final LogstashTcpSocketAppender appender = new TestableLogstashTcpSocketAppender();
    
    @Mock
    private Context context;
    
    @Mock
    private StatusManager statusManager;
    
    @Mock
    private ILoggingEvent event1;
    
    @Mock(lenient = true)
    private SocketFactory socketFactory;

    @Mock(lenient = true)
    private Socket socket;
    
    @Mock
    private OutputStream outputStream;
    
    @Mock(lenient = true)
    private Encoder<ILoggingEvent> encoder;

    @Mock
    private Future<?> readableCallableFuture;
    
    @Mock
    private TcpAppenderListener<ILoggingEvent> listener;

    @Mock
    private Random random;
    
    private class TestableLogstashTcpSocketAppender extends LogstashTcpSocketAppender {
        @Override
        protected Future<?> scheduleReaderCallable(Callable<Void> readerCallable) {
            return readableCallableFuture;
        }
    }
    
    @BeforeEach
    public void setup() throws IOException {
        when(context.getStatusManager()).thenReturn(statusManager);
        when(socketFactory.createSocket()).thenReturn(socket);
        when(socket.getOutputStream()).thenReturn(outputStream);
        when(encoder.encode(event1)).thenReturn("event1".getBytes(StandardCharsets.UTF_8));
        appender.addListener(listener);
        
    }
    
    @AfterEach
    public void tearDown() {
        appender.stop();
    }
    
    @Test
    public void testEncoderCalled_logback12OrLater() {
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
        SocketTimeoutException exception = new SocketTimeoutException();
        when(socketFactory.createSocket())
            .thenThrow(exception)
            .thenReturn(socket);
        
        appender.start();
        
        verify(encoder).start();
        
        appender.append(event1);
        
        verify(encoder, timeout(VERIFICATION_TIMEOUT)).encode(event1);

        assertThat(appender.getConnectedDestination()).isPresent();
        
        verify(listener).appenderStarted(appender);
        verify(listener).eventAppended(eq(appender), eq(event1), anyLong());
        verify(listener).connectionFailed(eq(appender), any(InetSocketAddress.class), eq(exception));
        verify(listener).connectionOpened(appender, socket);
        verify(listener).eventSent(eq(appender), eq(socket), eq(event1), anyLong());
        
        appender.stop();
        
        verify(listener).connectionClosed(appender, socket);

        assertThat(appender.getConnectedDestination()).isNotPresent();
    }

    @Test
    public void testReconnectOnWrite() {
        appender.addDestination("localhost:10000");
        appender.setReconnectionDelay(new Duration(100));
        
        appender.start();
        
        verify(encoder).start();
        
        doThrow(new RuntimeException()).doReturn("event1".getBytes(StandardCharsets.UTF_8)).when(encoder).encode(event1);
        
        appender.append(event1);
        
        verify(encoder, timeout(VERIFICATION_TIMEOUT).times(2)).encode(event1);
    }

    @Test
    public void testReconnectOnReadFailure() {
        
        appender.addDestination("localhost:10000");
        appender.setReconnectionDelay(new Duration(100));
        
        when(readableCallableFuture.isDone())
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

        assertThat(appender.getConnectedDestination()).isPresent();

    }
    
    
    /**
     * Scenario:
     *   Two servers: localhost:10000 (primary), localhost:10001 (secondary)
     *   Primary is not available at startup
     *   Appender should first try primary then immediately connect to secondary
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

    @Test
    public void testRandomDestinationAndReconnectToSecondaryOnOpen() throws Exception {
        appender.addDestination("localhost:10000");
        appender.addDestination("localhost:10001");
        RandomDestinationConnectionStrategy strategy = spy(new RandomDestinationConnectionStrategy());
        doReturn(random).when(strategy).getRandom();
        appender.setConnectionStrategy(strategy);

        // Make it failed to connect to second destination
        doThrow(SocketTimeoutException.class)
            .when(socket).connect(host("localhost", 10001), anyInt());

        // The first index is second destination.
        when(random.nextInt(appender.getDestinations().size())).thenReturn(1).thenReturn(0);

        // Start the appender and verify it is actually started.
        // It should try to connect to second destination by random destination, fail then retry on first destination.
        appender.start();
        verify(encoder).start();

        // TWO connection attempts must have been made (without delay)
        verify(socket, timeout(VERIFICATION_TIMEOUT).times(2)).connect(any(SocketAddress.class), anyInt());
        InOrder inOrder = inOrder(socket);

        // 1) First attempt on second destination: failure
        inOrder.verify(socket).connect(host("localhost", 10001), anyInt());

        // 2) Second attempt on first destination
        inOrder.verify(socket).connect(host("localhost", 10000), anyInt());
    }
    
    
    /**
     * Scenario:
     *   Two servers: localhost:10000 (primary), localhost:10001 (secondary)
     *   Primary is available at startup then fails after the first event.
     *   Appender should then connect on secondary for the next event.
     */
    @Test
    public void testReconnectToSecondaryOnWrite() throws Exception {
        appender.addDestination("localhost:10000");
        appender.addDestination("localhost:10001");

        // Primary accepts first connection attempt then refuses
        doNothing()
            .doThrow(SocketTimeoutException.class)
            .when(socket).connect(host("localhost", 10000), anyInt());

        // Secondary refuses all attempts
        doThrow(SocketTimeoutException.class)
                .when(socket).connect(host("localhost", 10001), anyInt());

        // First attempt of sending the event throws an exception while subsequent
        // attempts will succeed. This should force the appender to close the connection
        // and attempt to reconnect
        doThrow(new RuntimeException())
            .doReturn("event1".getBytes(StandardCharsets.UTF_8))
            .when(encoder).encode(event1);
        
        
        // Start the appender and verify it is actually started
        // At this point, it should be connected to primary.
        appender.start();
        verify(encoder).start();

        appender.append(event1);


        // TWO connection attempts must have been made in total
        verify(socket, timeout(VERIFICATION_TIMEOUT).times(2)).connect(any(SocketAddress.class), anyInt());
        InOrder inOrder = inOrder(socket);

        // 1) connected to primary at startup
        inOrder.verify(socket).connect(host("localhost", 10000), anyInt());

        // 2) retry on secondary after failed attempt to send event
        inOrder.verify(socket).connect(host("localhost", 10001), anyInt());
    }
    
    
    /**
     * Make sure the appender tries to reconnect to primary after a while.
     */
    @Test
    public void testReconnectToPrimaryWhileOnSecondary() throws Exception {
        appender.addDestination("localhost:10000");
        appender.addDestination("localhost:10001");
        appender.setReconnectionDelay(new Duration(1));
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
        appender.setKeepAliveCharset(StandardCharsets.UTF_8);
        appender.setKeepAliveDuration(Duration.buildByMilliseconds(100));

        String expectedKeepAlives = SeparatorParser.parseSeparator("UNIX") + SeparatorParser.parseSeparator("UNIX");
        byte[] expectedKeepAlivesBytes = expectedKeepAlives.getBytes(StandardCharsets.UTF_8);

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
        Assertions.assertArrayEquals(expectedKeepAlivesBytes, bos.toByteArray());
    }

    @Test
    public void testWriteTimeout() throws Exception {

        appender.addDestination("localhost");
        appender.setWriteTimeout(new Duration(100));
        appender.setReconnectionDelay(new Duration(1));
        appender.setWriteBufferSize(0);

        Socket badSocket = mock(Socket.class, "badSocket");
        Socket goodSocket = mock(Socket.class, "goodSocket");

        when(socketFactory.createSocket())
                .thenReturn(badSocket)
                .thenReturn(goodSocket);

        OutputStream badOutputStream = mock(OutputStream.class, "badOutputStream");
        when(badSocket.getOutputStream()).thenReturn(badOutputStream);

        CountDownLatch closeLatch = new CountDownLatch(1);
        doAnswer(invocation -> {
            closeLatch.countDown();
            return null;
        }).when(badSocket).close();

        doAnswer(invocation -> {
            closeLatch.await();
            throw new SocketException("socket closed");
        }).when(badOutputStream).write(any());

        OutputStream goodOutputStream = mock(OutputStream.class, "goodOutputStream");
        when(goodSocket.getOutputStream()).thenReturn(goodOutputStream);

        // Start the appender and verify it is actually started
        // At this point, it should be connected to primary.
        appender.start();
        verify(encoder).start();

        appender.append(event1);

        verify(goodOutputStream, timeout(1000)).write(any());
        verify(goodOutputStream, timeout(1000)).flush();
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
        
        // Primary accepts first connection then refuse subsequent attempts
        doNothing()
            .doThrow(SocketTimeoutException.class)
            .when(socket).connect(host("localhost", 10000), anyInt());

        // Secondary refuses all attempts
        doThrow(SocketTimeoutException.class)
            .when(socket).connect(host("localhost", 10001), anyInt());

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
        // TWO connection attempts must have been made in total:
        verify(socket, timeout(VERIFICATION_TIMEOUT).times(2)).connect(any(SocketAddress.class), anyInt());
        InOrder inOrder = inOrder(socket);

        // 1) connected to primary at startup
        inOrder.verify(socket).connect(host("localhost", 10000), anyInt());

        // 2) retry on secondary after failed attempt to send event
        inOrder.verify(socket).connect(host("localhost", 10001), anyInt());
    }
    
    
    /**
     * At least one valid destination must be configured. 
     * The appender refuses to start in case of error.
     */
    @Test
    public void testDestination_None() {
        appender.start();
        Assertions.assertFalse(appender.isStarted());
    }
    
    @Test
    public void testRoundRobin() throws Exception {
        appender.addDestination("localhost:10000");
        appender.addDestination("localhost:10001");
        RoundRobinDestinationConnectionStrategy strategy = new RoundRobinDestinationConnectionStrategy();
        strategy.setConnectionTTL(Duration.buildByMilliseconds(100));
        appender.setConnectionStrategy(strategy);

        appender.start();

        verify(encoder).start();

        appender.append(event1);

        // Wait for round robin to occur, then send an event.
        Thread.sleep(strategy.getConnectionTTL().getMilliseconds() + 50);
        appender.append(event1);

        verify(socket, timeout(VERIFICATION_TIMEOUT).times(2)).connect(any(SocketAddress.class), anyInt());
        InOrder inOrder = inOrder(socket);

        // 1) connected to primary at startup
        inOrder.verify(socket).connect(host("localhost", 10000), anyInt());

        // 2) connected to next destination by round-robin
        inOrder.verify(socket).connect(host("localhost", 10001), anyInt());


    }
    
    private SocketAddress host(final String host, final int port) {
        return argThat(hasHostAndPort(host, port));
    }
    
    private ArgumentMatcher<SocketAddress> hasHostAndPort(final String host, final int port) {
        return new ArgumentMatcher<SocketAddress>() {

            @Override
            public boolean matches(SocketAddress argument) {
                InetSocketAddress sockAddr = (InetSocketAddress) argument;
                return host.equals(sockAddr.getHostName()) && port == sockAddr.getPort();
            }

            @Override
            public String toString() {
                return host + ":" + port;
            }
        };
    }
}
