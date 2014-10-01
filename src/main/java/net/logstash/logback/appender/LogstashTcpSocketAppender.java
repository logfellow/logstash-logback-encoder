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
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import ch.qos.logback.classic.ClassicConstants;
import ch.qos.logback.classic.net.LoggingEventPreSerializationTransformer;
import ch.qos.logback.classic.net.SocketAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.net.DefaultSocketConnector;
import ch.qos.logback.core.net.SocketConnector;
import ch.qos.logback.core.spi.PreSerializationTransformer;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.util.CloseUtil;
import ch.qos.logback.core.util.Duration;

/**
 * This class is a modification of {@link SocketAppender}. The queue type and
 * the dispatch method is different than the original version. <br/>
 * The connection thread is going to be closed only if an event with the marker {@link ClassicConstants.FINALIZE_SESSION_MARKER} is sent. <br/>
 * <br/>
 * For example:<br/>
 * <code>logger.info(ClassicConstants.FINALIZE_SESSION_MARKER, "About to end the job");</code>
 * 
 * 
 * @author <a href="mailto:mirko.bernardoni@gmail.com">Mirko Bernardoni</a>
 * @since 11 Jun 2014 (creation date)
 */
public class LogstashTcpSocketAppender extends AppenderBase<ILoggingEvent>
        implements Runnable, SocketConnector.ExceptionHandler {
    
    private static final PreSerializationTransformer<ILoggingEvent> PST = new LoggingEventPreSerializationTransformer();
    
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
    public static final int DEFAULT_QUEUE_SIZE = 10000;
    
    /**
     * Default timeout when waiting for the remote server to accept our
     * connection.
     */
    private static final int DEFAULT_ACCEPT_CONNECTION_DELAY = 5000;
    
    /**
     * Default timeout for how long to wait when inserting an event into the
     * BlockingQueue.
     */
    private static final int DEFAULT_EVENT_DELAY_TIMEOUT = 100;
    
    private String remoteHost;
    
    private int port = DEFAULT_PORT;
    
    private InetAddress address;
    
    private Duration reconnectionDelay = new Duration(
            DEFAULT_RECONNECTION_DELAY);
    
    private int acceptConnectionTimeout = DEFAULT_ACCEPT_CONNECTION_DELAY;
    
    private Duration eventDelayLimit = new Duration(DEFAULT_EVENT_DELAY_TIMEOUT);

    private int queueSize = DEFAULT_QUEUE_SIZE;
    
    private BlockingQueue<ILoggingEvent> queue;
    
    private String peerId;
    
    private Future<?> task;
    
    private Future<Socket> connectorTask;
    
    private volatile Socket socket;
    
    /**
     * It is the encoder which is ultimately responsible for writing the event
     * to an {@link OutputStream}.
     */
    protected Encoder<ILoggingEvent> encoder;
    
    /**
     * @return the encoder
     */
    public Encoder<ILoggingEvent> getEncoder() {
        return encoder;
    }
    
    /**
     * @param encoder
     *            the encoder to set
     */
    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }
    
    /**
     * Get the pre-serialization transformer that will be used to transform each
     * event into a Serializable object before delivery to the remote receiver.
     * 
     * @return transformer object
     */
    public PreSerializationTransformer<ILoggingEvent> getPST() {
        return PST;
    }
    
    protected void encoderInit(OutputStream outputStream) {
        if (encoder != null && outputStream != null) {
            try {
                encoder.init(outputStream);
            } catch (IOException ioe) {
                this.started = false;
                addStatus(new ErrorStatus(
                        "Failed to initialize encoder for appender named ["
                                + name + "].", this, ioe));
            }
        }
    }
    
    protected void encoderClose(OutputStream outputStream) {
        if (encoder != null && outputStream != null) {
            try {
                encoder.close();
            } catch (IOException ioe) {
                this.started = false;
                addStatus(new ErrorStatus(
                        "Failed to write footer for appender named [" + name
                                + "].", this, ioe));
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void start() {
        if (isStarted())
            return;
        int errorCount = 0;
        if (port <= 0) {
            errorCount++;
            addError("No port was configured for appender"
                    + name
                    + " For more information, please visit http://logback.qos.ch/codes.html#socket_no_port");
        }
        
        if (remoteHost == null) {
            errorCount++;
            addError("No remote host was configured for appender"
                    + name
                    + " For more information, please visit http://logback.qos.ch/codes.html#socket_no_host");
        }
        
        if (errorCount == 0) {
            try {
                address = InetAddress.getByName(remoteHost);
            } catch (UnknownHostException ex) {
                addError("unknown host: " + remoteHost);
                errorCount++;
            }
        }
        
        if (errorCount == 0) {
            queue = new LinkedBlockingQueue<ILoggingEvent>(queueSize);
            peerId = "remote peer " + remoteHost + ":" + port + ": ";
            task = getContext().getExecutorService().submit(this);
            super.start();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (!isStarted())
            return;
        CloseUtil.closeQuietly(socket);
        task.cancel(true);
        if (connectorTask != null)
            connectorTask.cancel(true);
        super.stop();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void append(ILoggingEvent event) {
        if (event == null || !isStarted())
            return;
        
        try {
            final boolean inserted = queue.offer(event,
                    eventDelayLimit.getMilliseconds(), TimeUnit.MILLISECONDS);
            if (!inserted) {
                addInfo("Dropping event due to timeout limit of ["
                        + eventDelayLimit + "] being exceeded");
            }
        } catch (InterruptedException e) {
            addError("Interrupted while appending event to SocketAppender", e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public final void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                SocketConnector connector = createConnector(address, port, 0,
                        reconnectionDelay.getMilliseconds());
                
                connectorTask = activateConnector(connector);
                if (connectorTask == null)
                    break;
                
                socket = waitForConnectorToReturnASocket();
                if (socket == null)
                    break;
                dispatchEvents();
            }
        } catch (InterruptedException ex) {
            assert true; // ok... we'll exit now
        }
        addInfo("shutting down");
    }
    
    private SocketConnector createConnector(InetAddress address, int port,
            int initialDelay, long retryDelay) {
        SocketConnector connector = newConnector(address, port, initialDelay,
                retryDelay);
        connector.setExceptionHandler(this);
        connector.setSocketFactory(getSocketFactory());
        return connector;
    }
    
    private Future<Socket> activateConnector(SocketConnector connector) {
        try {
            return getContext().getExecutorService().submit(connector);
        } catch (RejectedExecutionException ex) {
            return null;
        }
    }
    
    private Socket waitForConnectorToReturnASocket()
            throws InterruptedException {
        try {
            Socket s = connectorTask.get();
            connectorTask = null;
            return s;
        } catch (ExecutionException e) {
            return null;
        }
    }
    
    /**
     * Inifinte loop that send the messages from the queue to the remote host
     * 
     * @throws InterruptedException
     */
    private void dispatchEvents() throws InterruptedException {
        OutputStream outputStream = null;
        try {
            socket.setSoTimeout(acceptConnectionTimeout);
            outputStream = new BufferedOutputStream(socket.getOutputStream());
            encoderInit(outputStream);
            socket.setSoTimeout(0);
            addInfo(peerId + "connection established");
            int counter = 0;
            while (true) {
                ILoggingEvent event = queue.take();
                this.encoder.doEncode(event);
                outputStream.flush();
                if (++counter >= CoreConstants.OOS_RESET_FREQUENCY) {
                    // Failing to reset the object output stream every now and
                    // then creates a serious memory leak.
                    outputStream.flush();
                    counter = 0;
                }
            }
        } catch (IOException ex) {
            addInfo(peerId + "connection failed: " + ex);
        } finally {
            if (outputStream != null) {
                encoderClose(outputStream);
            }
            CloseUtil.closeQuietly(socket);
            socket = null;
            addInfo(peerId + "connection closed");
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void connectionFailed(SocketConnector connector, Exception ex) {
        if (ex instanceof InterruptedException) {
            addInfo("connector interrupted");
        } else if (ex instanceof ConnectException) {
            addInfo(peerId + "connection refused");
        } else {
            addInfo(peerId + ex);
        }
    }
    
    /**
     * Creates a new {@link SocketConnector}.
     * <p>
     * The default implementation creates an instance of {@link DefaultSocketConnector}. A subclass may override to provide a different {@link SocketConnector} implementation.
     * 
     * @param address
     *            target remote address
     * @param port
     *            target remote port
     * @param initialDelay
     *            delay before the first connection attempt
     * @param retryDelay
     *            delay before a reconnection attempt
     * @return socket connector
     */
    protected SocketConnector newConnector(InetAddress address, int port,
            long initialDelay, long retryDelay) {
        return new DefaultSocketConnector(address, port, initialDelay,
                retryDelay);
    }
    
    /**
     * Gets the default {@link SocketFactory} for the platform.
     * <p>
     * Subclasses may override to provide a custom socket factory.
     */
    protected SocketFactory getSocketFactory() {
        return SocketFactory.getDefault();
    }
    
    /**
     * The <b>RemoteHost</b> property takes the name of of the host where a
     * corresponding server is running.
     */
    public void setRemoteHost(String host) {
        remoteHost = host;
    }
    
    /**
     * Returns value of the <b>RemoteHost</b> property.
     */
    public String getRemoteHost() {
        return remoteHost;
    }
    
    /**
     * The <b>Port</b> property takes a positive integer representing the port
     * where the server is waiting for connections.
     */
    public void setPort(int port) {
        this.port = port;
    }
    
    /**
     * Returns value of the <b>Port</b> property.
     */
    public int getPort() {
        return port;
    }
    
    /**
     * The <b>reconnectionDelay</b> property takes a positive {@link Duration} value representing the time to wait between each failed connection
     * attempt to the server. The default value of this option is to 30 seconds.
     * 
     * <p>
     * Setting this option to zero turns off reconnection capability.
     */
    public void setReconnectionDelay(Duration delay) {
        this.reconnectionDelay = delay;
    }
    
    /**
     * Returns value of the <b>reconnectionDelay</b> property.
     */
    public Duration getReconnectionDelay() {
        return reconnectionDelay;
    }
    
    /**
     * The <b>eventDelayLimit</b> takes a non-negative integer representing the
     * number of milliseconds to allow the appender to block if the underlying
     * BlockingQueue is full. Once this limit is reached, the event is dropped.
     * 
     * @param eventDelayLimit
     *            the event delay limit
     */
    public void setEventDelayLimit(Duration eventDelayLimit) {
        this.eventDelayLimit = eventDelayLimit;
    }
    
    /**
     * Returns the value of the <b>eventDelayLimit</b> property.
     */
    public Duration getEventDelayLimit() {
        return eventDelayLimit;
    }
    
    /**
     * Sets the timeout that controls how long we'll wait for the remote peer to
     * accept our connection attempt.
     * <p>
     * This property is configurable primarily to support instrumentation for unit testing.
     * 
     * @param acceptConnectionTimeout
     *            timeout value in milliseconds
     */
    void setAcceptConnectionTimeout(int acceptConnectionTimeout) {
        this.acceptConnectionTimeout = acceptConnectionTimeout;
    }

    /**
     * Returns the value of the <b>queueSize</b> property.
     */
    public int getQueueSize() {
        return queueSize;
    }

    /**
     * Sets the maximum number of entries in the queue. Once the queue is full additional entries will be dropped
     * if in the time given by the <b>eventDelayLimit</b> no space becomes available.
     *
     * @param queueSize the maximum number of entries in the queue
     */
    public void setQueueSize(int queueSize) {
        if(queue != null) {
            throw new IllegalStateException("Queue size must be set before initialization");
        }
        this.queueSize = queueSize;
    }
}