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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import net.logstash.logback.appender.destination.DelegateDestinationConnectionStrategy;
import net.logstash.logback.appender.destination.DestinationConnectionStrategy;
import net.logstash.logback.appender.destination.DestinationParser;
import net.logstash.logback.appender.destination.PreferPrimaryDestinationConnectionStrategy;
import net.logstash.logback.appender.listener.TcpAppenderListener;
import net.logstash.logback.encoder.CompositeJsonEncoder;
import net.logstash.logback.encoder.SeparatorParser;
import net.logstash.logback.encoder.StreamingEncoder;
import net.logstash.logback.util.ReusableByteBuffer;

import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.joran.spi.DefaultClass;
import ch.qos.logback.core.net.ssl.ConfigurableSSLSocketFactory;
import ch.qos.logback.core.net.ssl.SSLConfigurableSocket;
import ch.qos.logback.core.net.ssl.SSLConfiguration;
import ch.qos.logback.core.net.ssl.SSLParametersConfiguration;
import ch.qos.logback.core.spi.DeferredProcessingAware;
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
public abstract class AbstractLogstashTcpSocketAppender<Event extends DeferredProcessingAware, Listener extends TcpAppenderListener<Event>>
        extends AsyncDisruptorAppender<Event, Listener> {

    protected static final String HOST_NAME_FORMAT = "%3$s";
    protected static final String PORT_FORMAT = "%4$d";
    public static final String DEFAULT_THREAD_NAME_FORMAT = "logback-appender-" + APPENDER_NAME_FORMAT + "-" + HOST_NAME_FORMAT + ":" + PORT_FORMAT + "-" + THREAD_INDEX_FORMAT;

    /**
     * The default port number of remote logging server (4560).
     */
    public static final int DEFAULT_PORT = 4560;

    /**
     * The default reconnection delay (30000 milliseconds or 30 seconds).
     */
    public static final int DEFAULT_RECONNECTION_DELAY = 30000;

    /**
     * The default write timeout in milliseconds (0 means no write timeout).
     */
    public static final int DEFAULT_WRITE_TIMEOUT = 0;

    /**
     * Default size of the queue used to hold logging events that are destined
     * for the remote peer.
     * Assuming an average log entry to take 1k, this would result in the application
     * using about 10MB additional memory if the queue is full
     * @deprecated Use {@link #DEFAULT_RING_BUFFER_SIZE} instead
     */
    @Deprecated
    public static final int DEFAULT_QUEUE_SIZE = DEFAULT_RING_BUFFER_SIZE;

    /**
     * Default timeout when waiting for the remote server to accept our
     * connection. The same timeout is used as a read timeout during SSL
     * handshake.
     */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 5000;

    public static final int DEFAULT_WRITE_BUFFER_SIZE = 8192;

    private static final NotConnectedException NOT_CONNECTED_EXCEPTION = new NotConnectedException();
    private static final ShutdownInProgressException SHUTDOWN_IN_PROGRESS_EXCEPTION = new ShutdownInProgressException();
    static {
        NOT_CONNECTED_EXCEPTION.setStackTrace(new StackTraceElement[] {new StackTraceElement(AbstractLogstashTcpSocketAppender.TcpSendingEventHandler.class.getName(), "onEvent(..)", null, -1)});
        SHUTDOWN_IN_PROGRESS_EXCEPTION.setStackTrace(new StackTraceElement[] {new StackTraceElement(AbstractLogstashTcpSocketAppender.TcpSendingEventHandler.class.getName(), "onEvent(..)", null, -1)});
    }

    /**
     * Destinations to which to attempt to send logs, in order of preference.
     * <p>
     *
     * Logs are only sent to one destination at a time.
     * <p>
     *
     * The interpretation of this list is up to the current {@link #connectionStrategy}.
     */
    private List<InetSocketAddress> destinations = new ArrayList<>(2);

    /**
     * When connected, this is the index into {@link #destinations}
     * to the currently connected destination.
     * <p>
     *
     * When a connection has never been established, the value is 0.
     * <p>
     *
     * When a connection has been established, but lost, the value is the
     * previously connected index.
     */
    private volatile int connectedDestinationIndex = 0;

    /**
     * When connected, this is the connected destination address.
     * When not connected, this is null.
     */
    private volatile InetSocketAddress connectedDestination;

    /**
     * Strategy used to determine to which destination to connect, and when to reconnect.
     * Default is {@link PreferPrimaryDestinationConnectionStrategy}.
     */
    private DestinationConnectionStrategy connectionStrategy = new PreferPrimaryDestinationConnectionStrategy();

    /**
     * Time period for which to wait after a connection fails to a specific destination
     * before attempting to reconnect to that destination.
     * Default is {@value #DEFAULT_RECONNECTION_DELAY} milliseconds.
     */
    private Duration reconnectionDelay = new Duration(DEFAULT_RECONNECTION_DELAY);

    /**
     * Socket connection timeout in milliseconds.
     * Must be positive. A value of zero is interpreted as an infinite timeout.
     */
    private Duration connectionTimeout = new Duration(DEFAULT_CONNECTION_TIMEOUT);

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
     * Defaults to {@value #DEFAULT_WRITE_BUFFER_SIZE}
     *
     * If less than or equal to zero, buffering the output stream will be disabled.
     * If buffering is disabled, the writer thread can slow down, but
     * it will also can prevent dropping events in the buffer on flaky connections.
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
    private String keepAliveMessage = SeparatorParser.parseSeparator("UNIX");

    /**
     * The charset to use when writing the {@link #keepAliveMessage}.
     * Defaults to UTF-8.
     */
    private Charset keepAliveCharset = StandardCharsets.UTF_8;

    /**
     * The {@link #keepAliveMessage} translated to bytes using the {@link #keepAliveCharset}.
     * Populated at startup time.
     */
    private byte[] keepAliveBytes;

    /**
     * Time period for which to wait for a write to complete before timing out
     * and attempting to reconnect to that destination.
     */
    private Duration writeTimeout = new Duration(DEFAULT_WRITE_TIMEOUT);

    /**
     * Used to signal the socket reconnect thread that the shutdown has occurred.
     * The latch will be non-zero when started, and zero when shutdown.
     */
    private volatile CountDownLatch shutdownLatch;

    /**
     * The {@link ScheduledExecutorService} used to execute house keeping tasks.
     */
    private ScheduledThreadPoolExecutor executorService;
    
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
         * If {@link AbstractLogstashTcpSocketAppender#writeBufferSize} is greater than zero, this will be a buffered wrapper of the socket output stream.
         * Otherwise, it will be the socket output stream.
         */
        private volatile OutputStream outputStream;

        /**
         * Time at which the last event send was started (e.g. before write/flush).
         * Used to detect write timeouts.
         */
        private volatile long lastSendStartNanoTime;
        /**
         * Time at which the last event send was completed (e.g. after write/flush).
         * Used to calculate if a keep alive message
         * needs to be scheduled/sent.
         */
        private volatile long lastSendEndNanoTime;

        /**
         * The most recent time that a connection to each destination was attempted.
         */
        private long[] destinationAttemptStartTimes;

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
         * Future for the currently scheduled {@link #writeTimeoutRunnable}.
         */
        private ScheduledFuture<?> writeTimeoutFuture;

        /**
         * See {@link WriteTimeoutRunnable}.
         * Initialized on startup if write timeout is enabled.
         */
        private WriteTimeoutRunnable writeTimeoutRunnable;

        /**
         * See {@link ReaderCallable}.
         * Initialized when a socket is opened.
         */
        private Future<?> readerFuture;

        /**
         * Intermediate ByteBuffer used to store content generated by {@link StreamingEncoder}.
         * Set when {@link #onStart()} but stays uninitialized if encoder is a "raw" {@link Encoder}.
         */
        private ReusableByteBuffer buffer;
        
        /**
         * When run, if the {@link AbstractLogstashTcpSocketAppender#keepAliveDuration}
         * has elapsed since the last event was sent,
         * then this runnable will publish a keepAlive event to the ringBuffer.
         * <p>
         * The runnable will reschedule itself to execute in the future
         * after the calculated {@link AbstractLogstashTcpSocketAppender#keepAliveDuration}
         * from the last sent event using {@link TcpSendingEventHandler#scheduleKeepAlive(long)}.
         *
         * When the keepAlive event is processed by the event handler,
         * if the {@link AbstractLogstashTcpSocketAppender#keepAliveDuration}
         * has elapsed since the last event was sent,
         * then the event handler will send the {@link AbstractLogstashTcpSocketAppender#keepAliveMessage}
         * to the socket OutputStream.
         *
         */
        private class KeepAliveRunnable implements Runnable {

            private int previousDestinationIndex = connectedDestinationIndex;

            @Override
            public void run() {
                long lastSendEnd = lastSendEndNanoTime;
                long currentNanoTime = System.nanoTime();
                if (hasKeepAliveDurationElapsed(lastSendEnd, currentNanoTime)) {
                    /*
                     * Publish a keep alive message to the RingBuffer.
                     *
                     * A null event indicates that this is a keep alive message.
                     *
                     * Use tryPublishEvent instead of publishEvent, because if the ring buffer is full,
                     * there's really no need to send a keep alive, since
                     * there are other messages waiting to be sent.
                     */
                    getDisruptor().getRingBuffer().tryPublishEvent(getEventTranslator(), null);
                    scheduleKeepAlive(currentNanoTime);
                } else {
                    scheduleKeepAlive(lastSendEnd);
                }

                if (previousDestinationIndex != connectedDestinationIndex) {
                    /*
                     * Destination has changed since last keep alive event,
                     * so update the thread name
                     */
                    updateCurrentThreadName();
                }
                previousDestinationIndex = connectedDestinationIndex;
            }
        }

        /**
         * Keeps reading the {@link ReaderCallable#inputStream} until the
         * end of the stream is reached.
         *
         * This helps pro-actively detect server-side socket disconnections,
         * specifically in the case of Amazon's Elastic Load Balancers (ELB).
         */
        private class ReaderCallable implements Callable<Void> {

            private final InputStream inputStream;

            ReaderCallable(InputStream inputStream) {
                super();
                this.inputStream = inputStream;
            }

            @Override
            public Void call() throws Exception {
                updateCurrentThreadName();
                try {
                    while (true) {
                        try {
                            if (inputStream.read() == -1) {
                                /*
                                 * End of stream reached, so we're done.
                                 */
                                return null;
                            }
                        } catch (SocketTimeoutException e) {
                            /*
                             * ignore, and try again
                             */
                        } catch (Exception e) {
                            /*
                             * Something else bad happened, so we're done.
                             */
                            throw e;
                        }
                    }
                } finally {
                    if (!Thread.currentThread().isInterrupted()) {
                        executorService.submit(() ->
                            /*
                             * https://github.com/logstash/logstash-logback-encoder/issues/341
                             *
                             * Pro-actively trigger the event handler's onEvent method in the handler thread
                             * by publishing a null event (which usually means a keepAlive event).
                             *
                             * When onEvent handles the event in the handler thread,
                             * it will detect that readerFuture.isDone() and reopen the socket.
                             *
                             * Without this, onEvent would not be called until the next event,
                             * which might not occur for a while.
                             * So, this is really just an optimization to reopen the socket as soon as possible.
                             *
                             * We can't reopen the socket from this thread,
                             * since all socket open/close must be done from the event handler thread.
                             *
                             * There is a potential race condition here as well, since
                             * onEvent could be triggered before the readerFuture completes.
                             * We reduce (but not eliminate) the chance of that happening by
                             * scheduling this task on the executorService.
                             */
                            getDisruptor().getRingBuffer().tryPublishEvent(getEventTranslator(), null)
                        );
                    }
                }
            }

        }

        /**
         * Detects write timeouts by inspecting {@link #lastSendStartNanoTime} and {@link #lastSendEndNanoTime}
         */
        private class WriteTimeoutRunnable implements Runnable {

            /**
             * The lastSendStartNanoTime of the last detected timeout.
             * Used to ensure we only detect a write timeout for a single write once
             * (especially if the log rate is very low).
             */
            private volatile long lastDetectedStartNanoTime;

            @Override
            public void run() {
                long lastSendStart = lastSendStartNanoTime; // volatile read
                long lastSendEnd = lastSendEndNanoTime;     // volatile read

                /*
                 * A write is in progress if the start is greater than the end
                 */
                if (lastSendStart > lastSendEnd && lastSendStart != lastDetectedStartNanoTime) {

                    long elapsedSendTimeInMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastSendStart);
                    if (elapsedSendTimeInMillis > writeTimeout.getMilliseconds()) {
                        lastDetectedStartNanoTime = lastSendStart;
                        addWarn(peerId + "Detected write timeout after " + elapsedSendTimeInMillis + "ms.  Write timeout=" + getWriteTimeout() + ".  Closing socket to force reconnect");
                        closeSocket();
                    }
                }
            }
        }
        
        
        @Override
        public void onEvent(LogEvent<Event> logEvent, long sequence, boolean endOfBatch) throws Exception {

            Exception sendFailureException = null;
            for (int i = 0; i < MAX_REPEAT_WRITE_ATTEMPTS; i++) {
                /*
                 * Save local references to the outputStream and socket
                 * in case the WriteTimeoutRunnable closes the socket.
                 */
                Socket socket = this.socket; // volatile read
                OutputStream outputStream = this.outputStream; // volatile read

                if (socket == null && (!isStarted() || Thread.currentThread().isInterrupted())) {
                    /*
                     * Handle shutdown in progress
                     *
                     * This will occur if shutdown occurred during reopen()
                     */
                    sendFailureException = SHUTDOWN_IN_PROGRESS_EXCEPTION;
                    break;
                }

                Future<?> readerFuture = this.readerFuture;  // volatile read
                if (readerFuture.isDone() || socket == null) {
                    /*
                     * If readerFuture.isDone(), then the destination has shut down its output (our input),
                     * and the destination is probably no longer listening to its input (our output).
                     * This will be the case for Amazon's Elastic Load Balancers (ELB)
                     * when an instance behind the ELB becomes unhealthy while we're connected to it.
                     *
                     * If socket == null here, it means that a write timed out,
                     * and the socket was closed by the WriteTimeoutRunnable.
                     *
                     * Therefore, attempt reconnection.
                     */
                    addInfo(peerId + "destination terminated the connection. Reconnecting.");
                    reopenSocket();
                    try {
                        readerFuture.get();
                        sendFailureException = NOT_CONNECTED_EXCEPTION;
                    } catch (Exception e) {
                        sendFailureException = e;
                    }
                    continue;
                }
                try {
                    writeEvent(socket, outputStream, logEvent, endOfBatch);
                    return;
                } catch (Exception e) {
                    sendFailureException = e;
                    addWarn(peerId + "unable to send event: " + e.getMessage() + " Reconnecting.", e);
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

            if (logEvent.event != null) {
                fireEventSendFailure(logEvent.event, sendFailureException);
            }
        }

        private void writeEvent(Socket socket, OutputStream outputStream, LogEvent<Event> logEvent, boolean endOfBatch) throws IOException {

            long startWallTime = System.currentTimeMillis();
            long startNanoTime = System.nanoTime();
            lastSendStartNanoTime = startNanoTime;
            /*
             * A null event indicates that this is a keep alive message,
             * or an event sent from the ReaderCallable.
             */
            if (logEvent.event != null) {
                /*
                 * This is a standard (non-keepAlive) event.
                 * Therefore, we need to send the event.
                 */
                encode(logEvent.event, outputStream);
            } else if (hasKeepAliveDurationElapsed(lastSendEndNanoTime, startNanoTime)) {
                /*
                 * This is a keep alive event, and the keepAliveDuration has passed,
                 * Therefore, we need to send the keepAliveMessage.
                 */
                outputStream.write(keepAliveBytes);
            }
            if (endOfBatch) {
                outputStream.flush();
            }
            long endNanoTime = System.nanoTime();
            lastSendEndNanoTime = endNanoTime;

            if (logEvent.event != null) {
                fireEventSent(socket, logEvent.event, endNanoTime - startNanoTime);
            }

            /*
             * Should we close the current connection, and attempt to reconnect to another destination?
             */
            if (connectionStrategy.shouldReconnect(startWallTime, connectedDestinationIndex, destinations.size())) {
                addInfo(peerId + "reestablishing connection.");
                outputStream.flush();
                reopenSocket();
            }
        }

        
        @SuppressWarnings("unchecked")
        private void encode(Event event, OutputStream outputStream) throws IOException {
            if (encoder instanceof StreamingEncoder) {
                /*
                 * Generate content in a temporary buffer to avoid writing "partial" content in the output
                 * stream if the Encoder throws an exception.
                 */
                try {
                    ((StreamingEncoder<Event>) encoder).encode(event, buffer);
                    buffer.writeTo(outputStream);
                } finally {
                    buffer.reset();
                }
            } else {
                byte[] data = encoder.encode(event);
                if (data != null) {
                    outputStream.write(data);
                }
            }
        }
        
        
        private boolean hasKeepAliveDurationElapsed(long lastSentNanoTime, long currentNanoTime) {
            return isKeepAliveEnabled()
                    && lastSentNanoTime + TimeUnit.MILLISECONDS.toNanos(keepAliveDuration.getMilliseconds()) < currentNanoTime;
        }

        @Override
        public void onStart() {
            this.destinationAttemptStartTimes = new long[destinations.size()];
            
            if (encoder instanceof CompositeJsonEncoder) {
                this.buffer = new ReusableByteBuffer(((CompositeJsonEncoder<Event>) encoder).getMinBufferSize());
            } else if (encoder instanceof StreamingEncoder) {
                this.buffer = new ReusableByteBuffer();
            }
            
            openSocket();
            scheduleKeepAlive(System.nanoTime());
            scheduleWriteTimeout();
        }

        @Override
        public void onShutdown() {
            unscheduleWriteTimeout();
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
            int errorCount = 0;
            int destinationIndex = connectedDestinationIndex;
            while (isStarted() && !Thread.currentThread().isInterrupted()) {
                destinationIndex = connectionStrategy.selectNextDestinationIndex(destinationIndex, destinations.size());
                long startWallTime = System.currentTimeMillis();
                Socket tempSocket = null;
                OutputStream tempOutputStream = null;

                /*
                 * Choose next server
                 */
                InetSocketAddress currentDestination = destinations.get(destinationIndex);
                try {
                    /*
                     * Update peerId (for status message)
                     */
                    peerId = "Log destination " + currentDestination + ": ";

                    /*
                     * Delay the connection attempt if the last attempt to the selected destination
                     * was less than the reconnectionDelay.
                     */
                    final long millisSinceLastAttempt = startWallTime - destinationAttemptStartTimes[destinationIndex];
                    if (millisSinceLastAttempt < reconnectionDelay.getMilliseconds()) {
                        final long sleepTime = reconnectionDelay.getMilliseconds() - millisSinceLastAttempt;
                        if (errorCount < MAX_REPEAT_CONNECTION_ERROR_LOG * destinations.size()) {
                            addWarn(peerId + "Waiting " + sleepTime + "ms before attempting reconnection.");
                        }
                        try {
                            shutdownLatch.await(sleepTime, TimeUnit.MILLISECONDS);
                            if (!isStarted()) {
                                return;
                            }
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            addWarn(peerId + "connection interrupted. Will no longer attempt reconnection.");
                            return;
                        }
                        // reset the start time to be after the wait period.
                        startWallTime = System.currentTimeMillis();
                    }
                    destinationAttemptStartTimes[destinationIndex] = startWallTime;

                    /*
                     * Set the SO_TIMEOUT so that SSL handshakes will timeout if they take too long.
                     *
                     * Note that SO_TIMEOUT only applies to reads (which occur during the handshake process).
                     */
                    tempSocket = socketFactory.createSocket();
                    tempSocket.setSoTimeout((int) connectionTimeout.getMilliseconds());
                    
                    /*
                     * currentDestination is unresolved, so a new InetSocketAddress
                     * must be created to resolve the hostname.
                     */
                    tempSocket.connect(new InetSocketAddress(getHostString(currentDestination), currentDestination.getPort()), (int) connectionTimeout.getMilliseconds());
                    
                    /*
                     * Trigger SSL handshake immediately and declare the socket unconnected if it fails
                     */
                    if (tempSocket instanceof SSLSocket) {
                        ((SSLSocket) tempSocket).startHandshake();
                    }

                    /*
                     * Issue #218, make buffering the output stream optional.
                     */
                    tempOutputStream = writeBufferSize > 0
                            ? new BufferedOutputStream(tempSocket.getOutputStream(), writeBufferSize)
                            : tempSocket.getOutputStream();

                    addInfo(peerId + "connection established.");

                    this.socket = tempSocket;
                    this.outputStream = tempOutputStream;

                    boolean shouldUpdateThreadName = (destinationIndex != connectedDestinationIndex);
                    connectedDestinationIndex = destinationIndex;
                    connectedDestination = currentDestination;

                    connectionStrategy.connectSuccess(startWallTime, destinationIndex, destinations.size());

                    if (shouldUpdateThreadName) {
                        /*
                         * destination has changed, so update the thread name
                         */
                        updateCurrentThreadName();
                    }

                    this.readerFuture = scheduleReaderCallable(
                            new ReaderCallable(tempSocket.getInputStream()));

                    fireConnectionOpened(this.socket);

                    return;

                } catch (Exception e) {
                    CloseUtil.closeQuietly(tempOutputStream);
                    CloseUtil.closeQuietly(tempSocket);

                    connectionStrategy.connectFailed(startWallTime, destinationIndex, destinations.size());
                    fireConnectionFailed(currentDestination, e);

                    /*
                     * Avoid spamming status messages by checking the MAX_REPEAT_CONNECTION_ERROR_LOG.
                     */
                    if (errorCount++ < MAX_REPEAT_CONNECTION_ERROR_LOG * destinations.size()) {
                        addWarn(peerId + "connection failed.", e);
                    }
                }
            }
        }

        private synchronized void closeSocket() {
            connectedDestination = null;
            CloseUtil.closeQuietly(outputStream);
            outputStream = null;

            CloseUtil.closeQuietly(socket);
            fireConnectionClosed(socket);
            socket = null;

            if (this.readerFuture != null) {
                /*
                 * This shouldn't be necessary, since closing the socket
                 * should cause the read() call to throw an exception.
                 *
                 * But cancel it anyway to be extra-safe.
                 */
                this.readerFuture.cancel(true);
            }
        }

        private void closeEncoder() {
            encoder.stop();
            buffer = null;
        }

        private synchronized void scheduleKeepAlive(long basedOnNanoTime) {
            if (isKeepAliveEnabled() && !Thread.currentThread().isInterrupted()) {
                if (keepAliveRunnable == null) {
                    keepAliveRunnable = new KeepAliveRunnable();
                }
                long delay = TimeUnit.MILLISECONDS.toNanos(keepAliveDuration.getMilliseconds()) - (System.nanoTime() - basedOnNanoTime);
                try {
                    keepAliveFuture = executorService.schedule(
                        keepAliveRunnable,
                        delay,
                        TimeUnit.NANOSECONDS);
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
        private synchronized void scheduleWriteTimeout() {
            if (isWriteTimeoutEnabled() && !Thread.currentThread().isInterrupted()) {
                if (writeTimeoutRunnable == null) {
                    writeTimeoutRunnable = new WriteTimeoutRunnable();
                }
                long delay = writeTimeout.getMilliseconds();
                try {
                    writeTimeoutFuture = executorService.scheduleWithFixedDelay(
                            writeTimeoutRunnable,
                            delay,
                            delay,
                            TimeUnit.MILLISECONDS);
                } catch (RejectedExecutionException e) {
                    /*
                     * if scheduling failed, it means that the appender is shutting down.
                     */
                    writeTimeoutFuture = null;
                }
            }
        }
        private synchronized void unscheduleWriteTimeout() {
            if (writeTimeoutFuture != null) {
                writeTimeoutFuture.cancel(true);
                try {
                    writeTimeoutFuture.get();
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

        UnconnectedConfigurableSSLSocketFactory(SSLParametersConfiguration parameters, SSLSocketFactory delegate) {
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

    public AbstractLogstashTcpSocketAppender() {
        super();
        setThreadNameFormat(DEFAULT_THREAD_NAME_FORMAT);
    }

    @Override
    protected EventHandler<LogEvent<Event>> createEventHandler() {
        return new TcpSendingEventHandler();
    }
    
    @Override
    public boolean isStarted() {
        CountDownLatch latch = this.shutdownLatch;
        return latch != null && latch.getCount() != 0;
    }

    @Override
    public synchronized void start() {
        if (isStarted()) {
            return;
        }
        int errorCount = 0;
        if (encoder == null) {
            errorCount++;
            addError("No encoder was configured. Use <encoder> to specify the fully qualified class name of the encoder to use");
        }

        /*
         * Make sure at least one destination has been specified
         */
        if (destinations.isEmpty()) {
            errorCount++;
            addError("No destination was configured. Use <destination> to add one or more destinations to the appender");
        }

        /*
         * Create socket factory
         */
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

        if (keepAliveMessage != null) {
            keepAliveBytes = keepAliveMessage.getBytes(keepAliveCharset);
        }

        if (errorCount == 0) {
          
            encoder.setContext(getContext());
            if (!encoder.isStarted()) {
                encoder.start();
            }

            /*
             * Start with an initial core size of 1 to handle the Reader thread
             */
            int threadPoolCoreSize = 1;
            /*
             * Increase the core size to handle the keep alive thread
             */
            if (isKeepAliveEnabled()) {
                threadPoolCoreSize++;
            }
            /*
             * Increase the core size to handle the write timeout detection thread
             */
            if (isWriteTimeoutEnabled()) {
                threadPoolCoreSize++;
            }
            this.executorService = new ScheduledThreadPoolExecutor(
                    threadPoolCoreSize,
                    getThreadFactory());

            /*
             * This ensures that cancelled tasks do not hold up shutdown.
             */
            this.executorService.setRemoveOnCancelPolicy(true);
            
            this.shutdownLatch = new CountDownLatch(1);
            super.start();
        }
    }

    @Override
    public synchronized void stop() {
        if (!isStarted()) {
            return;
        }
        
        super.stop();

        /*
         * Stop waiting to reconnect (if reconnect logic is currently waiting)
         */
        this.shutdownLatch.countDown();
             
        /*
         * Stop executor service
         */
        this.executorService.shutdown();
        try {
            if (!this.executorService.awaitTermination(1, TimeUnit.MINUTES)) {
                addWarn("Some queued events have not been logged due to requested shutdown");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            addWarn("Some queued events have not been logged due to requested shutdown", e);
        }
    }

    protected Future<?> scheduleReaderCallable(Callable<Void> readerCallable) {
        return executorService.submit(readerCallable);
    }

    protected void fireEventSent(Socket socket, Event event, long durationInNanos) {
        safelyFireEvent(l -> l.eventSent(this, socket, event, durationInNanos));
    }

    protected void fireEventSendFailure(Event event, Throwable reason) {
        safelyFireEvent(l -> l.eventSendFailure(this, event, reason));
    }

    protected void fireConnectionOpened(Socket socket) {
        safelyFireEvent(l -> l.connectionOpened(this, socket));
    }

    protected void fireConnectionClosed(Socket socket) {
        safelyFireEvent(l -> l.connectionClosed(this, socket));
    }

    protected void fireConnectionFailed(InetSocketAddress address, Throwable throwable) {
        safelyFireEvent(l -> l.connectionFailed(this, address, throwable));
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
     * Set the {@link SocketFactory} used to create client {@link Socket}s to which to communicate.
     * Use {@code null} to use the system default SocketFactory.
     * 
     * @param socketFactory the socket factory to use to create connections with remote destinations.
     */
    public void setSocketFactory(SocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    /**
     * Adds the given destination (or destinations) to the list of potential destinations
     * to which to send logs.
     * <p>
     *
     * The string is a comma separated list of destinations in the form of hostName[:portNumber].
     * <p>
     * If portNumber is not provided, then the default ({@value #DEFAULT_PORT}) will be used
     * <p>
     *
     * For example, "host1.domain.com,host2.domain.com:5560"
     * 
     * @param destination comma-separated list of destinations in the form of {@code hostName[:portNumber]}
     */
    public void addDestination(final String destination) throws IllegalArgumentException {

        List<InetSocketAddress> parsedDestinations = DestinationParser.parse(destination, DEFAULT_PORT);

        addDestinations(parsedDestinations.toArray(new InetSocketAddress[0]));
    }

    /**
     * Adds the given destinations to the list of potential destinations.
     * 
     * @param destinations the {@link InetSocketAddress} to add to the list of valid destinations
     */
    public void addDestinations(InetSocketAddress... destinations) throws IllegalArgumentException  {
        if (destinations == null) {
            return;
        }

        for (InetSocketAddress destination : destinations) {
            try {
                InetAddress.getByName(getHostString(destination));
            } catch (UnknownHostException ex) {
                /*
                 * Warn, but don't fail startup, so that transient
                 * DNS problems are allowed to resolve themselves eventually.
                 */
                addWarn("Invalid destination '" + getHostString(destination) + "': host unknown (was '" + getHostString(destination) + "').");
            }
            this.destinations.add(destination);
        }
    }

    /**
     * Returns the host string from the given destination,
     * avoiding a DNS hit if possible.
     * 
     * @param destination the {@link InetSocketAddress} to get the host string from
     * @return the host string of the given destination
     */
    protected String getHostString(InetSocketAddress destination) {

        /*
         * Avoid the potential DNS hit by using getHostString() instead of getHostName()
         */
        return destination.getHostString();
    }

    protected void updateCurrentThreadName() {
        Thread.currentThread().setName(calculateThreadName());
    }

    @Override
    protected List<Object> getThreadNameFormatParams() {
        List<Object> superThreadNameFormatParams = super.getThreadNameFormatParams();
        List<Object> threadNameFormatParams = new ArrayList<>(superThreadNameFormatParams.size() + 2);

        threadNameFormatParams.addAll(superThreadNameFormatParams);
        InetSocketAddress currentDestination = this.destinations.get(connectedDestinationIndex);
        threadNameFormatParams.add(getHostString(currentDestination));
        threadNameFormatParams.add(currentDestination.getPort());
        return threadNameFormatParams;
    }

    /**
     * Return the destinations in which to attempt to send logs.
     * 
     * @return an ordered list of {@link InetSocketAddress} representing the configured destinations
     */
    public List<InetSocketAddress> getDestinations() {
        return Collections.unmodifiableList(destinations);
    }

    /**
     * Time period for which to wait after failing to connect to all servers,
     * before attempting to reconnect.
     * Default is {@value #DEFAULT_RECONNECTION_DELAY} milliseconds.
     * 
     * @param delay the reconnection delay
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
     * Convenience method for setting {@link PreferPrimaryDestinationConnectionStrategy#setSecondaryConnectionTTL(Duration)}.
     *
     * When the {@link #connectionStrategy} is a {@link PreferPrimaryDestinationConnectionStrategy},
     * this will set its {@link PreferPrimaryDestinationConnectionStrategy#setSecondaryConnectionTTL(Duration)}.
     *
     * @see PreferPrimaryDestinationConnectionStrategy#setSecondaryConnectionTTL(Duration)
     * @param secondaryConnectionTTL the TTL of a connection when connected to a secondary destination
     * @throws IllegalStateException if the {@link #connectionStrategy} is not a {@link PreferPrimaryDestinationConnectionStrategy}
     * 
     * @deprecated use {@link PreferPrimaryDestinationConnectionStrategy#setSecondaryConnectionTTL(Duration)} instead.
     */
    @Deprecated
    public void setSecondaryConnectionTTL(Duration secondaryConnectionTTL) {
        addWarn(
              "Setting <secondaryConnectionTTL> directly on the appender is deprecated. "
            + "Instead you should explicitly set the connection strategy to <preferPrimary> and set its <secondaryConnectionTTL> property to the desired value.");

        if (connectionStrategy instanceof PreferPrimaryDestinationConnectionStrategy) {
            ((PreferPrimaryDestinationConnectionStrategy) connectionStrategy).setSecondaryConnectionTTL(secondaryConnectionTTL);
        } else {
            throw new IllegalStateException(String.format("When setting the secondaryConnectionTTL, the strategy must be a %s. It is currently a %s", PreferPrimaryDestinationConnectionStrategy.class, connectionStrategy));
        }
    }

    /**
     * Convenience method for accessing {@link PreferPrimaryDestinationConnectionStrategy#getSecondaryConnectionTTL()}.
     * 
     * @return the secondary connection TTL or {@code null} if the connection strategy is not a {@link PreferPrimaryDestinationConnectionStrategy}.
     * @deprecated use {@link PreferPrimaryDestinationConnectionStrategy#getSecondaryConnectionTTL()} instead.
     * 
     * @see #getConnectionStrategy()
     * @see PreferPrimaryDestinationConnectionStrategy#getSecondaryConnectionTTL()
     */
    @Deprecated
    public Duration getSecondaryConnectionTTL() {
        if (connectionStrategy instanceof PreferPrimaryDestinationConnectionStrategy) {
            return ((PreferPrimaryDestinationConnectionStrategy) connectionStrategy).getSecondaryConnectionTTL();
        }
        return null;
    }

    /**
     * Set the connection timeout when establishing a connection to a remote destination.
     * 
     * Use {@code 0} for an "infinite timeout" which often really means "use the OS defaults".
     * 
     * @param connectionTimeout connection timeout
     */
    public void setConnectionTimeout(Duration connectionTimeout) {
        if (Objects.requireNonNull(connectionTimeout).getMilliseconds() < 0) {
            throw new IllegalArgumentException("connectionTimeout must be a positive value");
        }
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Get the connection timeout used when establishing a TCP connection to a remote destination.
     * 
     * @return the connection timeout (never null).
     */
    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }
    
    
    public int getWriteBufferSize() {
        return writeBufferSize;
    }

    /**
     * The number of bytes available in the write buffer.
     * Defaults to {@value #DEFAULT_WRITE_BUFFER_SIZE}.
     *
     * <p>
     * If less than or equal to zero, buffering the output stream will be disabled.
     * If buffering is disabled, the writer thread can slow down, but
     * it will also can prevent dropping events in the buffer on flaky connections.
     * 
     * @param writeBufferSize the write buffer size in bytes
     */
    public void setWriteBufferSize(int writeBufferSize) {
        this.writeBufferSize = writeBufferSize;
    }

    /**
     * Returns the maximum number of events in the queue.
     * Alias for {@link #getRingBufferSize()}.
     * 
     * @return the size of the ring buffer
     * @deprecated use {@link #getRingBufferSize()} instead.
     */
    @Deprecated
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
     * @deprecated use {@link #setRingBufferSize(int)} instead.
     */
    @Deprecated
    public void setQueueSize(int queueSize) {
        addWarn("<queueSize> is deprecated, use <ringBufferSize> instead");
        setRingBufferSize(queueSize);
    }

    public SSLConfiguration getSsl() {
        return sslConfiguration;
    }
    
    /**
     * Set this to non-null to use SSL.
     * See <a href="http://logback.qos.ch/manual/usingSSL.html"> the logback manual</a>
     * for details on how to configure SSL for a client.
     * 
     * @param sslConfiguration the SSL configuration
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
     * When {@code null}, zero or negative, no keepAlive messages will be sent.
     * 
     * @param keepAliveDuration duration between consecutive keep alive messages
     */
    public void setKeepAliveDuration(Duration keepAliveDuration) {
        this.keepAliveDuration = keepAliveDuration;
    }

    public String getKeepAliveMessage() {
        return keepAliveMessage;
    }
    
    /**
     * Message to send for keeping the connection alive
     * if {@link #keepAliveDuration} is non-null and strictly positive.
     *
     * The following values have special meaning:
     * <ul>
     * <li>{@code null} or empty string = no keep alive.</li>
     * <li>"{@code SYSTEM}" = operating system new line (default).</li>
     * <li>"{@code UNIX}" = unix line ending (\n).</li>
     * <li>"{@code WINDOWS}" = windows line ending (\r\n).</li>
     * </ul>
     * <p>
     * Any other value will be used as-is.
     * 
     * @param keepAliveMessage the keep alive message
     */
    public void setKeepAliveMessage(String keepAliveMessage) {
        this.keepAliveMessage = SeparatorParser.parseSeparator(keepAliveMessage);
    }

    public boolean isKeepAliveEnabled() {
        return this.keepAliveDuration != null && this.keepAliveDuration.getMilliseconds() > 0
            && this.keepAliveMessage != null && !this.keepAliveMessage.isEmpty();
    }

    /**
     * Whether the write timeout feature is enabled or not.
     * 
     * @return {@code true} when the appender should try to detect write timeouts, {@code false} otherwise.
     */
    public boolean isWriteTimeoutEnabled() {
        return this.writeTimeout.getMilliseconds() > 0;
    }

    public Charset getKeepAliveCharset() {
        return keepAliveCharset;
    }

    /**
     * The charset to use when writing the {@link #keepAliveMessage}.
     * Defaults to UTF-8.
     * 
     * @param keepAliveCharset charset encoding for the keep alive message
     */
    public void setKeepAliveCharset(Charset keepAliveCharset) {
        this.keepAliveCharset = Objects.requireNonNull(keepAliveCharset);
    }

    /**
     * Pattern used by the to set the handler thread name.
     * Defaults to {@value #DEFAULT_THREAD_NAME_FORMAT}.
     * <p>
     *
     * If you change the {@link #threadFactory}, then this
     * value may not be honored.
     * <p>
     *
     * The string is a format pattern understood by {@link Formatter#format(String, Object...)}.
     * {@link Formatter#format(String, Object...)} is used to
     * construct the actual thread name prefix.
     * The first argument (%1$s) is the string appender name.
     * The second argument (%2$d) is the numerical thread index.
     * The third argument (%3$s) is the string hostname of the currently connected destination.
     * The fourth argument (%4$d) is the numerical port of the currently connected destination.
     * Other arguments can be made available by subclasses.
     * 
     * @param threadNameFormat thread name format pattern
     */
    @Override
    public void setThreadNameFormat(String threadNameFormat) {
        super.setThreadNameFormat(threadNameFormat);
    }

    public DestinationConnectionStrategy getConnectionStrategy() {
        return connectionStrategy;
    }
    @DefaultClass(DelegateDestinationConnectionStrategy.class)
    public void setConnectionStrategy(DestinationConnectionStrategy destinationConnectionStrategy) {
        this.connectionStrategy = Objects.requireNonNull(destinationConnectionStrategy);
    }

    /**
     * Returns the currently connected destination as an {@link Optional}.
     * The {@link Optional} will be absent if the appender is not currently connected.
     *
     * @return the currently connected destination as an {@link Optional}.
     *         The {@link Optional} will be absent if the appender is not currently connected.
     */
    public Optional<InetSocketAddress> getConnectedDestination() {
        return Optional.ofNullable(this.connectedDestination);
    }

    public Duration getWriteTimeout() {
        return writeTimeout;
    }

    /**
     * Sets the time period for which to wait for a write to complete before timing out
     * and attempting to reconnect to that destination.
     * This timeout is used to detect connections where the receiver stops reading.
     * 
     * <p>The timeout must be &gt; 0. A timeout of zero is interpreted as an infinite timeout
     * which effectively means "no write timeout".
     *
     * <p>Note that since a blocking java socket output stream does not have a concept
     * of a write timeout, a task will be scheduled with the same frequency as the write
     * timeout in order to detect stuck writes.
     * It is recommended to use longer write timeouts (e.g. &gt; 30s, or minutes),
     * rather than short write timeouts, so that this task does not execute too frequently.
     * Also, this approach means that it could take up to two times the write timeout
     * before a write timeout is detected.
     * 
     * @param writeTimeout the write timeout
     */
    public void setWriteTimeout(Duration writeTimeout) {
        if (writeTimeout == null || writeTimeout.getMilliseconds() < 0) {
            throw new IllegalArgumentException("writeTimeout must be >= 0");
        }
        this.writeTimeout = writeTimeout;
    }
}
