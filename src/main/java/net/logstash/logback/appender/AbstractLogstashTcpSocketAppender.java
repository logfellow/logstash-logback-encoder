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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;

import javax.net.SocketFactory;

import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.util.CloseUtil;
import ch.qos.logback.core.util.Duration;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

/**
 * An {@link AsyncDisruptorAppender} appender that writes
 * events to a TCP {@link Socket} outputStream.
 * <p>
 * 
 * The behavior is similar to a {@link ch.qos.logback.classic.net.SocketAppender}, except that:
 * <ul>
 * <li>it uses a {@link RingBuffer} instead of a {@link BlockingQueue}</li>
 * <li>it writes using an {@link Encoder} instead of serialization</li>
 * </ul>
 *
 *
 * @author <a href="mailto:mirko.bernardoni@gmail.com">Mirko Bernardoni</a> (original, which did not use disruptor)
 * @since 11 Jun 2014 (creation date)
 */
public abstract class AbstractLogstashTcpSocketAppender<Event extends DeferredProcessingAware>
        extends AsyncDisruptorAppender<Event> {

    /**
     * The default port number of remote logging server (4560).
     */
    public static final int DEFAULT_PORT = 4560;

    /**
     * The default reconnection delay (30000 milliseconds or 30 seconds).
     */
    public static final int DEFAULT_RECONNECTION_DELAY = 30000;

    /**
     * Default size of the queue used to hold logging events that are destined
     * for the remote peer.
     * Assuming an average log entry to take 1k, this would result in the application
     * using about 10MB additional memory if the queue is full
     */
    public static final int DEFAULT_QUEUE_SIZE = DEFAULT_RING_BUFFER_SIZE;

    /**
     * Default timeout when waiting for the remote server to accept our
     * connection.
     */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 5000;

    public static final int DEFAULT_WRITE_BUFFER_SIZE = 8192;
    
    /**
     * The host to which to connect and send events
     */
    private String remoteHost;

    /**
     * The TCP port on the host to which to connect and send events
     */
    private int port = DEFAULT_PORT;

    /**
     * The resolved remote address.
     */
    private InetAddress remoteAddress;

    /**
     * Time period for which to wait after a connection fails,
     * before attempting to reconnect.
     * Default is {@value #DEFAULT_RECONNECTION_DELAY} milliseconds.
     */
    private Duration reconnectionDelay = new Duration(DEFAULT_RECONNECTION_DELAY);

    /**
     * Socket connection timeout in milliseconds. 
     */
    private int acceptConnectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    
    /**
     * Human readable identifier of the client (used for logback status messages) 
     */
    private String peerId;

    /**
     * The encoder which is ultimately responsible for writing the event
     * to the socket's {@link java.io.OutputStream}.
     */
    private Encoder<Event> encoder;
    
    /**
     * The number of bytes available in the write buffer.
     */
    private int writeBufferSize = DEFAULT_WRITE_BUFFER_SIZE;
    
    /**
     * Used to create client {@link Socket}s to which to communicate.
     * By default, it is the system default SocketFactory.
     */
    private SocketFactory socketFactory = SocketFactory.getDefault();

    /**
     * Event handler responsible for performing the TCP transmission.
     */
    private class TcpSendingEventHandler implements EventHandler<LogEvent<Event>>, LifecycleAware {
        
        /**
         * Max number of consecutive failed connection attempts for which
         * logback status messages will be logged.
         * 
         * After this many failed attempts, reconnection will still
         * be attempted, but failures will not be logged again
         * (until after the connection is successful, and then fails again.)
         */
        private static final int MAX_REPEAT_CONNECTION_ERROR_LOG = 5;
        
        /**
         * Number of times we try to write an event before it is discarded.
         * Between each attempt, the socket will be reconnected.
         */
        private static final int MAX_REPEAT_WRITE_ATTEMPTS = 5;

        /**
         * True when this event handler is started.
         * It will be started by the {@link Disruptor}.
         */
        private volatile boolean started;
        
        private volatile Socket socket;
        private volatile OutputStream outputStream;
        
        @Override
        public void onEvent(LogEvent<Event> logEvent, long sequence, boolean endOfBatch) throws Exception {
            
            for (int i = 0; i < MAX_REPEAT_WRITE_ATTEMPTS; i++) {
                if (!started) {
                    return;
                }
                try {
                    encoder.doEncode(logEvent.event);
                    if (endOfBatch) {
                        outputStream.flush();
                    }
                    break;
                } catch (SocketException e) {
                    addWarn(peerId + "unable to send event: " + e.getMessage(), e);
                    reopenSocket();
                } catch (IOException e) {
                    addWarn(peerId + "unable to send event: " + e.getMessage(), e);
                }
            }
        }

        @Override
        public void onStart() {
            /*
             * Set started = true before attempting to openSocket,
             * because openSocket checks the started state.
             */
            started = true;
            openSocket();
        }
        
        @Override
        public void onShutdown() {
            started = false;
            closeEncoder();
            closeSocket();
        }
        
        private synchronized void reopenSocket() {
            closeSocket();
            openSocket();
        }

        /**
         * Repeatedly tries to open a socket until it is successful,
         * or the hander is stopped, or the handler thread is interrupted.
         * 
         * If the socket is non-null when this method returns,
         * then it should be able to be used to send.
         */
        private synchronized void openSocket() {
            try {
                int errorCount = 0;
                while (socket == null && started && !Thread.currentThread().isInterrupted()) {
                    long startTime = System.currentTimeMillis();
                    try {
                        socket = socketFactory.createSocket();
                        socket.connect(new InetSocketAddress(remoteAddress, port), acceptConnectionTimeout);
                        outputStream = new BufferedOutputStream(socket.getOutputStream(), writeBufferSize);
                        
                        encoder.init(outputStream);
                        
                        addInfo(peerId + "connection established.");
                        
                    } catch (IOException e) {
                        
                        closeSocket();
                        /*
                         * If the connection timed out, then take the elapsed time into account
                         * when calculating time to sleep
                         */
                        long sleepTime = reconnectionDelay.getMilliseconds() - (System.currentTimeMillis() - startTime);
                        
                        /*
                         * Avoid spamming status messages by checking the MAX_REPEAT_CONNECTION_ERROR_LOG.
                         */
                        if (errorCount++ < MAX_REPEAT_CONNECTION_ERROR_LOG) {
                            addWarn(peerId + "connection failed. Waiting " + sleepTime + "ms before attempting reconnection.", e);
                        }
                        
                        if (sleepTime > 0) {
                            Thread.sleep(sleepTime);
                        }
                    } 
                }
            } catch (InterruptedException e) {
                addWarn(peerId + "connection interrupted");
            }
        }
        
        private synchronized void closeSocket() {
            CloseUtil.closeQuietly(outputStream);
            outputStream = null;
            
            CloseUtil.closeQuietly(socket);
            socket = null;
        }
        
        private void closeEncoder() {
            try {
                encoder.close();
            } catch (IOException ioe) {
                addStatus(new ErrorStatus(
                        "Failed to close encoder for appender named [" + name + "].", this, ioe));
            }
            
            encoder.stop();
        }

    }

    public AbstractLogstashTcpSocketAppender() {
        super();
        setEventHandler(new TcpSendingEventHandler());
    }

    public void start() {
        if (isStarted()) {
            return;
        }
        int errorCount = 0;
        if (encoder == null) {
            errorCount++;
            addError("No encoder was configured for appender " + name + ".");
        }
        if (port <= 0) {
            errorCount++;
            addError("No port was configured for appender " + name + ".");
        }

        if (remoteHost == null) {
            errorCount++;
            addError("No remote host was configured for appender " + name + ".");
        }

        if (errorCount == 0) {
            try {
                remoteAddress = InetAddress.getByName(remoteHost);
            } catch (UnknownHostException ex) {
                addError("unknown host: " + remoteHost);
                errorCount++;
            }
        }

        if (errorCount == 0) {
            
            if (getThreadNamePrefix() == DEFAULT_THREAD_NAME_PREFIX) {
                setThreadNamePrefix(DEFAULT_THREAD_NAME_PREFIX + remoteHost + ":" + port + "-");
            }
            encoder.setContext(getContext());
            if (!encoder.isStarted()) {
                encoder.start();
            }
            peerId = "Log destination " + remoteHost + ":" + port + ": ";
            super.start();
        }
    }

    public Encoder<Event> getEncoder() {
        return encoder;
    }

    public void setEncoder(Encoder<Event> encoder) {
        this.encoder = encoder;
    }
    
    public SocketFactory getSocketFactory() {
        return socketFactory;
    }
    
    /**
     * Used to create client {@link Socket}s to which to communicate.
     * By default, it is the system default SocketFactory.
     */
    public void setSocketFactory(SocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    /**
     * The host to which to connect and send events
     */
    public void setRemoteHost(String host) {
        remoteHost = host;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    /**
     * The TCP port on the host to which to connect and send events
     */
    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    /**
     * Time period for which to wait after a connection fails,
     * before attempting to reconnect.
     * Default is {@value #DEFAULT_RECONNECTION_DELAY} milliseconds.
     */
    public void setReconnectionDelay(Duration delay) {
        if (delay == null || delay.getMilliseconds() <= 0) {
            throw new IllegalArgumentException("reconnectionDelay must be > 0");
        }
        this.reconnectionDelay = delay;
    }

    public Duration getReconnectionDelay() {
        return reconnectionDelay;
    }

    /**
     * Socket connection timeout in milliseconds. 
     */
    void setAcceptConnectionTimeout(int acceptConnectionTimeout) {
        this.acceptConnectionTimeout = acceptConnectionTimeout;
    }

    /**
     * Returns the maximum number of events in the queue.
     */
    public int getQueueSize() {
        return getRingBufferSize();
    }

    /**
     * Sets the maximum number of events in the queue. Once the queue is full
     * additional events will be dropped.
     *
     * @param queueSize the maximum number of entries in the queue.
     */
    public void setQueueSize(int queueSize) {
        if (queueSize <= 0) {
            throw new IllegalArgumentException("queueSize must be > 0");
        }
        setRingBufferSize(queueSize);
    }
}