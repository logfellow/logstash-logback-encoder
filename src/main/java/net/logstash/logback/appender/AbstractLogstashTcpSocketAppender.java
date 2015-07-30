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
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import net.logstash.logback.encoder.SeparatorParser;

import org.apache.commons.lang.StringUtils;

import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.net.ssl.ConfigurableSSLSocketFactory;
import ch.qos.logback.core.net.ssl.SSLConfigurableSocket;
import ch.qos.logback.core.net.ssl.SSLConfiguration;
import ch.qos.logback.core.net.ssl.SSLParametersConfiguration;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.util.CloseUtil;
import ch.qos.logback.core.util.Duration;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.RingBuffer;

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
 * <p>
 * 
 * In addition, SSL can be enabled by setting the SSL configuration via {@link #setSsl(SSLConfiguration)}. 
 * See <a href="http://logback.qos.ch/manual/usingSSL.html">the logback manual</a>
 * for details on how to configure client-side SSL.
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
     * Comma separated list of host addresses
     */
    private String hostAddresses;
    
    /**
     * Array with the host/port information sorted by preference
     */
    private HostInfo[] hostInfo;
    
    /**
     * Time period for which to wait after a connection fails,
     * before attempting to reconnect.
     * Default is {@value #DEFAULT_RECONNECTION_DELAY} milliseconds.
     */
    private Duration reconnectionDelay = new Duration(DEFAULT_RECONNECTION_DELAY);

    /**
     * Time period to wait before attempting to reconnect to primary server
     * when multiple servers are specified. Only happens when the appender
     * is currently connected to a secondary.
     * 
     * The value is set to null when the feature is disabled: the appender will
     * stay on the current server until an error occurs.
     */
    private Duration reattemptPrimaryConnectionDelay = null;
    
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
     * 
     * If set prior to startup, it will be used.
     * <p>
     * 
     * If not set prior to startup, and {@link #sslConfiguration} is null,
     * then the default socket factory ({@link SocketFactory#getDefault()}) will be used.
     * <p>
     * 
     * If not set prior to startup, and {@link #sslConfiguration} is not null,
     * then a socket factory created from the
     * {@link SSLConfiguration#createContext(ch.qos.logback.core.spi.ContextAware)} will be used.
     */
    private SocketFactory socketFactory;
    
    /**
     * Set this to non-null to use SSL.
     * See <a href="http://logback.qos.ch/manual/usingSSL.html"> the logback manual</a>
     * for details on how to configure SSL for a client.
     */
    private SSLConfiguration sslConfiguration;
    
    /**
     * If this duration elapses without an event being sent,
     * then the {@link #keepAliveMessage} will be sent to the socket in
     * order to keep the connection alive.
     * 
     * When null (the default), no keepAlive messages will be sent.
     */
    private Duration keepAliveDuration;
    
    /**
     * Message to send for keeping the connection alive
     * if {@link #keepAliveDuration} is non-null.
     */
    private String keepAliveMessage = System.getProperty("line.separator");
    
    /**
     * The charset to use when writing the {@link #keepAliveMessage}.
     * Defaults to UTF-8.
     */
    private Charset keepAliveCharset = Charset.forName("UTF-8");
    
    /**
     * The {@link #keepAliveMessage} translated to bytes using the {@link #keepAliveCharset}.
     * Populated at startup time.
     */
    private byte[] keepAliveBytes;
    
    /**
     * Used to signal the socket reconnect thread that the shutdown has occurred.
     * The latch will be non-zero when started, and zero when shutdown.  
     */
    private volatile CountDownLatch shutdownLatch;
    
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
         * The destination socket to which to send events.
         */
        private volatile Socket socket;
        
        /**
         * The destination output stream to which to send events.
         * This is a buffered wrapper of the socket output stream.
         */
        private volatile OutputStream outputStream;
        
        /**
         * Time at which the last event was sent.
         * Used to calculate if a keep alive message
         * needs to be scheduled/sent.
         */
        private volatile long lastSentTimestamp;
        
        /**
         * Time at which the current connection should be automatically closed
         * to force an attempt to reconnect to the primary server
         */
        private volatile long secondaryConnectionTTL = Long.MAX_VALUE;
        
        /**
         * Future for the currently scheduled {@link #keepAliveRunnable}.
         */
        private ScheduledFuture<?> keepAliveFuture;
        
        /**
         * See {@link KeepAliveRunnable}.
         * Initialized on startup if keep alive is enabled.
         */
        private KeepAliveRunnable keepAliveRunnable;
        
        /**
         * When run, if the {@link AbstractLogstashTcpSocketAppender#keepAliveDuration}
         * has elasped since the last event was sent,
         * then this runnable will publish a keepAlive event to the ringBuffer.
         * <p>
         * The runnable will reschedule itself to execute in the future
         * after the calculated {@link AbstractLogstashTcpSocketAppender#keepAliveDuration}
         * from the last sent event using {@link TcpSendingEventHandler#scheduleKeepAlive(long)}.
         * 
         * When the keepAlive event is processed by the event handler,
         * if the {@link AbstractLogstashTcpSocketAppender#keepAliveDuration}
         * has elasped since the last event was sent,
         * then the event handler will send the {@link AbstractLogstashTcpSocketAppender#keepAliveMessage}
         * to the socket outputstream.
         *
         */
        private class KeepAliveRunnable implements Runnable {

            @Override
            public void run() {
                long lastSent = lastSentTimestamp;
                long currentTime = System.currentTimeMillis();
                if (hasKeepAliveDurationElapsed(lastSent, currentTime)) {
                    /*
                     * Publish a keep alive message to the RingBuffer.
                     * 
                     * A null event indicates that this is a keep alive message. 
                     */
                    getDisruptor().getRingBuffer().publishEvent(getEventTranslator(), null);
                    scheduleKeepAlive(currentTime);
                } else {
                    scheduleKeepAlive(lastSent);
                }
            }
        }

        @Override
        public void onEvent(LogEvent<Event> logEvent, long sequence, boolean endOfBatch) throws Exception {
            
            for (int i = 0; i < MAX_REPEAT_WRITE_ATTEMPTS; i++) {
                if (this.socket == null) {
                    /*
                     * socket could be null if reconnect failed due to shutdown in progress.
                     */
                    return;
                }
                try {
                    long currentTime = System.currentTimeMillis();
                    /*
                     * A null event indicates that this is a keep alive message. 
                     */
                    if (logEvent.event != null) {
                        /*
                         * This is a standard (non-keepAlive) event.
                         * Therefore, we need to send the event.
                         */
                        encoder.doEncode(logEvent.event);
                    } else if (hasKeepAliveDurationElapsed(lastSentTimestamp, currentTime)){
                        /*
                         * This is a keep alive event, and the keepAliveDuration has passed,
                         * Therefore, we need to send the keepAliveMessage.
                         */
                        outputStream.write(keepAliveBytes);
                    }
                    if (endOfBatch) {
                        outputStream.flush();
                    }
                    lastSentTimestamp = currentTime;
                    
                    /*
                     * Should we close the current connection?
                     */
                    if( shouldCloseConnection(currentTime) ) {
                    	addInfo(peerId + "closing connection and attempt to reconnect to primary server.");
                    	outputStream.flush();
                    	reopenSocket();
                    }
                    break;
                } catch (Exception e) {
                    addWarn(peerId + "unable to send event: " + e.getMessage(), e);
                    /*
                     * Need to re-open the socket in case of IOExceptions.
                     * 
                     * Reopening the socket probably won't help other exceptions
                     * (like NullPointerExceptions),
                     * but we're doing so anyway, just in case.
                     */
                    reopenSocket();
                }
            }
        }

        private boolean hasKeepAliveDurationElapsed(long lastSent, long currentTime) {
            return isKeepAliveEnabled()
                    && lastSent + keepAliveDuration.getMilliseconds() < currentTime;
        }

        private boolean shouldCloseConnection(long currentTime) {
        	return secondaryConnectionTTL <= currentTime;
        }

        private HostInfo getHost(int hostIndex) {
        	return hostInfo[hostIndex];
        }

        private boolean isPrimaryHost(int hostIndex) {
        	return hostIndex == 0;
        }

        @Override
        public void onStart() {
            openSocket();
            scheduleKeepAlive(System.currentTimeMillis());
        }

        @Override
        public void onShutdown() {
            unscheduleKeepAlive();
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
        	int hostIndex = 0; 
            int errorCount = 0;
            while (isStarted() && !Thread.currentThread().isInterrupted()) {
                long startTime = System.currentTimeMillis();
                Socket tempSocket = null;
                OutputStream tempOutputStream = null;
                try {
                    /*
                     * Choose next server and update peerId (for status message)
                     */
                    HostInfo currentHost = getHost(hostIndex);
                    peerId = "Log destination " + currentHost + ": ";
                    
                    /*
                     * Set the SO_TIMEOUT so that SSL handshakes will timeout if they take too long.
                     * 
                     * Note that SO_TIMEOUT only applies to reads (which occur during the handshake process).
                     */
                    tempSocket = socketFactory.createSocket();
                    tempSocket.setSoTimeout(acceptConnectionTimeout);
                    tempSocket.connect(new InetSocketAddress(currentHost.host, currentHost.port), acceptConnectionTimeout);
                    tempOutputStream = new BufferedOutputStream(tempSocket.getOutputStream(), writeBufferSize);
                    
                    encoder.init(tempOutputStream);
                    
                    addInfo(peerId + "connection established.");
                    
                    this.socket = tempSocket;
                    this.outputStream = tempOutputStream;
                    
                    /*
                     * If connected to a secondary, remember when the connection should be closed to
                     * force attempt to reconnect to primary
                     */
                    if( reattemptPrimaryConnectionDelay != null && !isPrimaryHost(hostIndex) ) {
                    	secondaryConnectionTTL = startTime + reattemptPrimaryConnectionDelay.getMilliseconds();
                    }
                    else {
                    	secondaryConnectionTTL = Long.MAX_VALUE;
                    }
                    
                    return;
                    
                } catch (Exception e) {
                    
                    CloseUtil.closeQuietly(tempOutputStream);
                    CloseUtil.closeQuietly(tempSocket);
                    
                    /*
                     * Retry immediately with next available host if any. Otherwise, sleep and retry with primary
                     */
                    hostIndex++;
                    if( hostIndex >= hostInfo.length ) {
                        hostIndex = 0;
                        
                        /*
                         * If the connection timed out, then take the elapsed time into account
                         * when calculating time to sleep
                         */
                        long sleepTime = Math.max(0, reconnectionDelay.getMilliseconds() - (System.currentTimeMillis() - startTime));
                        
                        /*
                         * Avoid spamming status messages by checking the MAX_REPEAT_CONNECTION_ERROR_LOG.
                         */
                        if (errorCount++ < MAX_REPEAT_CONNECTION_ERROR_LOG * hostInfo.length) {
                            addWarn(peerId + "connection failed. Waiting " + sleepTime + "ms before attempting reconnection.", e);
                        }
                        
                        if (sleepTime > 0) {
                            try {
                                shutdownLatch.await(sleepTime, TimeUnit.MILLISECONDS);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                addWarn(peerId + "connection interrupted. Will no longer attempt reconnection");
                            }
                        }
                    }
                    else {
                        /*
                         * Avoid spamming status messages by checking the MAX_REPEAT_CONNECTION_ERROR_LOG.
                         */
                        if (errorCount++ < MAX_REPEAT_CONNECTION_ERROR_LOG * hostInfo.length) {
                            addWarn(peerId + "connection failed. Retry with next server ("+hostInfo[hostIndex]+")", e);
                        }
                    }
                }
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
        
        private synchronized void scheduleKeepAlive(long basedOnTime) {
            if (isKeepAliveEnabled() && !Thread.currentThread().isInterrupted()) {
                if (keepAliveRunnable == null) {
                    keepAliveRunnable = new KeepAliveRunnable();
                }
                long delay = keepAliveDuration.getMilliseconds() - (System.currentTimeMillis() - basedOnTime);
                try {
                    keepAliveFuture = getExecutorService().schedule(
                        keepAliveRunnable,
                        delay,
                        TimeUnit.MILLISECONDS);
                } catch (RejectedExecutionException e) {
                    /*
                     * if scheduling failed, it means that the appender is shutting down.
                     */
                    keepAliveFuture = null;
                }
            }
        }
        private synchronized void unscheduleKeepAlive() {
            if (keepAliveFuture != null) {
                keepAliveFuture.cancel(true);
                try {
                    keepAliveFuture.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    // ignore
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }
    
    /**
     * An extension of logback's {@link ConfigurableSSLSocketFactory}
     * that supports creating unconnected sockets
     * (via {@link UnconnectedConfigurableSSLSocketFactory#createSocket()})
     * so that a custom connection timeout can be used when connecting. 
     */
    private static class UnconnectedConfigurableSSLSocketFactory extends ConfigurableSSLSocketFactory {

        private final SSLParametersConfiguration parameters;
        private final SSLSocketFactory delegate;

        public UnconnectedConfigurableSSLSocketFactory(SSLParametersConfiguration parameters, SSLSocketFactory delegate) {
            super(parameters, delegate);
            this.parameters = parameters;
            this.delegate = delegate;
        }
        
        @Override
        public Socket createSocket() throws IOException {
            SSLSocket socket = (SSLSocket) delegate.createSocket();
            parameters.configure(new SSLConfigurableSocket(socket));
            return socket;
        }
        
    }

    /**
     * An immutable representation of a host and port
     */
    private static class HostInfo {
        private final String host;
        private final int port;

        public HostInfo(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String toString() {
            return host+":"+port;
        }
    }
    
    
    public AbstractLogstashTcpSocketAppender() {
        super();
        setEventHandler(new TcpSendingEventHandler());
    }
    
    @Override
    public boolean isStarted() {
        CountDownLatch latch = this.shutdownLatch;
        return latch != null && latch.getCount() != 0;
    }
        
    public synchronized void start() {
        if (isStarted()) {
            return;
        }
        int errorCount = 0;
        if (encoder == null) {
            errorCount++;
            addError("No encoder was configured for appender " + name + ".");
        }
        
        if( hostAddresses != null && remoteHost != null ) {
        	errorCount++;
        	addError("Use 'remoteHost' or 'remoteAddresses' but not both");
        }
        else {
            if( hostAddresses != null ) {
                String[] hostAndPorts = StringUtils.split(hostAddresses, ",");

                hostInfo = new HostInfo[hostAndPorts.length];
                int hostIndex = 0;

                for(String hostAndPort: hostAndPorts) {
                    String[] parts = StringUtils.split(hostAndPort,':');

                    if( parts.length > 2 ) {
                        errorCount++;
                        addError("Invalid hostAddress '"+hostAndPort+"': unparseable value (expected format 'host:port')");
                        continue;
                    }

                    String host = StringUtils.trim(parts[0]);
                    try {
                        InetAddress.getByName(host);
                    } catch (UnknownHostException ex) {
                        addError("Invalid hostAddress '"+hostAndPort+"': host unknown (was '"+host+"')");
                        errorCount++;
                        continue;
                    }

                    int port = DEFAULT_PORT;
                    if( parts.length == 2 ) {
                        try {
                            port = Integer.parseInt(StringUtils.trim(parts[1]));
                        }
                        catch(NumberFormatException e) {
                            addError("Invalid hostAddress '"+hostAndPort+"': unparseable port (was '"+parts[1]+"')");
                            errorCount++;
                            continue;
                        }
                    }
                    if( port <= 0 ) {
                        addError("Invalid hostAddress '"+hostAndPort+"': port must be greater than 0 (was "+port+")");
                        errorCount++;
                        continue;
                    }

                    hostInfo[hostIndex] = new HostInfo(host, port);
                    hostIndex++;
                }

                if( remoteHost != null ) {
                    addWarn("'hostAddresses' specified: ignoring 'port' and 'remoteHost' parameters");
                }
            }
            else {
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
                        InetAddress.getByName(remoteHost);
                        this.hostInfo = new HostInfo[] { new HostInfo(remoteHost, port) };
                    } catch (UnknownHostException ex) {
                        addError("unknown host: " + remoteHost);
                        errorCount++;
                    }
                }
            }
        }
        
        if (errorCount == 0 && socketFactory == null) {
            if (sslConfiguration == null) {
                socketFactory = SocketFactory.getDefault();
            } else {
                
                try {
                    SSLContext sslContext = getSsl().createContext(this);
                    SSLParametersConfiguration parameters = getSsl().getParameters();
                    parameters.setContext(getContext());
                    
                    socketFactory = new UnconnectedConfigurableSSLSocketFactory(
                            parameters, 
                            sslContext.getSocketFactory());
                } catch (Exception e) {
                    addError("Unable to create ssl context", e);
                    errorCount++;
                }
            }
        }
        
        if (keepAliveMessage != null && keepAliveCharset != null) {
            keepAliveBytes = keepAliveMessage.getBytes(keepAliveCharset);
        }

        if (errorCount == 0) {
            
            if (getThreadNamePrefix() == DEFAULT_THREAD_NAME_PREFIX) {
                setThreadNamePrefix(DEFAULT_THREAD_NAME_PREFIX + remoteHost + ":" + port + "-");	//FIXME what if multiple hosts?
            }
            encoder.setContext(getContext());
            if (!encoder.isStarted()) {
                encoder.start();
            }
            
            if (keepAliveDuration != null) {
                setThreadPoolCoreSize(getThreadPoolCoreSize() + 1);
            }
            this.shutdownLatch = new CountDownLatch(1);
            super.start();
        }
    }
    
    @Override
    public synchronized void stop() {
        if (!isStarted()) {
            return;
        }
        /*
         * Stop waiting to reconnect (if reconnect logic is currently waiting)
         */
        this.shutdownLatch.countDown();
        super.stop();
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
    
    public void setHostAddresses(String hostAddresses) {
		this.hostAddresses = hostAddresses;
	}
    
    public String getHostAddresses() {
        return this.hostAddresses;
    }
    
    /**
     * Time period for which to wait after failing to connect to all servers,
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
     * Time period for which to wait before attempting to reconnect to the primary server.
     * Default is to stay on the current server, would it be primary or secondary, until 
     * an error occur.
     */
    public void setReattemptPrimaryConnectionDelay(Duration delay) {
    	if (delay != null && delay.getMilliseconds() <= 0) {
            throw new IllegalArgumentException("reattemptPrimaryConnectionDelay must be > 0");
        }
        this.reattemptPrimaryConnectionDelay = delay;
	}
    
    public Duration getReattemptPrimaryConnectionDelay() {
        return reattemptPrimaryConnectionDelay;
    }

    /**
     * Socket connection timeout in milliseconds. 
     */
    void setAcceptConnectionTimeout(int acceptConnectionTimeout) {
        this.acceptConnectionTimeout = acceptConnectionTimeout;
    }

    public int getWriteBufferSize() {
        return writeBufferSize;
    }
    
    /**
     * The number of bytes available in the write buffer.
     */
   public void setWriteBufferSize(int writeBufferSize) {
        this.writeBufferSize = writeBufferSize;
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
     * <p>
     * Must be a positive power of 2.
     *
     * @param queueSize the maximum number of entries in the queue.
     */
    public void setQueueSize(int queueSize) {
        setRingBufferSize(queueSize);
    }
    
    public SSLConfiguration getSsl() {
        return sslConfiguration;
    }
    /**
     * Set this to non-null to use SSL.
     * See <a href="http://logback.qos.ch/manual/usingSSL.html"> the logback manual</a>
     * for details on how to configure SSL for a client.
     */
    public void setSsl(SSLConfiguration sslConfiguration) {
        this.sslConfiguration = sslConfiguration;
    }
    
    public Duration getKeepAliveDuration() {
        return keepAliveDuration;
    }
    /**
     * If this duration elapses without an event being sent,
     * then the {@link #keepAliveMessage} will be sent to the socket in
     * order to keep the connection alive.
     * 
     * When null, no keepAlive messages will be sent.
     */
    public void setKeepAliveDuration(Duration keepAliveDuration) {
        this.keepAliveDuration = keepAliveDuration;
    }
    
    public String getKeepAliveMessage() {
        return keepAliveMessage;
    }
    /**
     * Message to send for keeping the connection alive
     * if {@link #keepAliveDuration} is non-null.
     * 
     * The following values have special meaning:
     * <ul>
     * <li><tt>null</tt> or empty string = no keep alive.</li>
     * <li>"<tt>SYSTEM</tt>" = operating system new line (default).</li>
     * <li>"<tt>UNIX</tt>" = unix line ending (\n).</li>
     * <li>"<tt>WINDOWS</tt>" = windows line ending (\r\n).</li>
     * </ul>
     * <p>
     * Any other value will be used as-is.
     */
    public void setKeepAliveMessage(String keepAliveMessage) {
        this.keepAliveMessage = SeparatorParser.parseSeparator(keepAliveMessage);
    }
    
    public boolean isKeepAliveEnabled() {
        return this.keepAliveDuration != null
                && this.keepAliveMessage != null;
    }
    
    public Charset getKeepAliveCharset() {
        return keepAliveCharset;
    }
    
    /**
     * The charset to use when writing the {@link #keepAliveMessage}.
     * Defaults to UTF-8.
     */
    public void setKeepAliveCharset(String keepAliveCharset) {
        this.keepAliveCharset = Charset.forName(keepAliveCharset);
    }
}
