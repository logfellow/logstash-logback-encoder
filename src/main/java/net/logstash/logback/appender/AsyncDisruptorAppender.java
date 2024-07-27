/*
 * Copyright 2013-2023 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import net.logstash.logback.appender.listener.AppenderListener;
import net.logstash.logback.status.LevelFilteringStatusListener;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.Duration;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.PhasedBackoffWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceReportingEventHandler;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

/**
 * An asynchronous appender that uses an LMAX Disruptor {@link RingBuffer}
 * as the interthread data exchange mechanism (as opposed to a {@link BlockingQueue}
 * used by logback's {@link AsyncAppender}).
 * <p>
 *
 * See the <a href="https://lmax-exchange.github.io/disruptor/">LMAX Disruptor documentation</a>
 * for more information about the advantages of using a {@link RingBuffer} over a {@link BlockingQueue}.
 * <p>
 *
 * The behavior of the appender when the RingBuffer is full and the event cannot be published
 * is controlled by the {@link #appendTimeout} configuration parameter.
 * By default the appender drops the event immediately, and emits a warning message every
 * {@link #droppedWarnFrequency} consecutive dropped events.
 * It can also be configured to wait until some space is available, with or without timeout.
 * <p>
 *
 * A single handler thread will be used to handle the actual handling of the event.
 * <p>
 *
 * Subclasses must implement {@link #createEventHandler()} to provide a {@link EventHandler} to
 * define the logic that executes in the handler thread.
 * For example, {@link DelegatingAsyncDisruptorAppender} will delegate
 * appending of the event to another appender in the handler thread.
 * <p>
 *
 * By default, child threads created by this appender will be daemon threads,
 * and therefore allow the JVM to exit gracefully without
 * needing to explicitly shut down the appender.
 * Note that in this case, it is possible for appended log events to not
 * be handled (if the child thread has not had a chance to process them yet).
 *
 * By setting {@link #setDaemon(boolean)} to false, you can change this behavior.
 * When false, child threads created by this appender will not be daemon threads,
 * and therefore will prevent the JVM from shutting down
 * until the appender is explicitly shut down.
 * Set this to false if you want to ensure that every log event
 * prior to shutdown is handled.
 *
 * @param <Event> type of event ({@link ILoggingEvent} or {@link IAccessEvent}).
 */
public abstract class AsyncDisruptorAppender<Event extends DeferredProcessingAware, Listener extends AppenderListener<Event>> extends UnsynchronizedAppenderBase<Event> {
    /**
     * Time in nanos to wait between drain attempts during the shutdown phase
     */
    private static final long SLEEP_TIME_DURING_SHUTDOWN = 50 * 1_000_000L; // 50ms

    protected static final String APPENDER_NAME_FORMAT = "%1$s";
    protected static final String THREAD_INDEX_FORMAT = "%2$d";
    public static final String DEFAULT_THREAD_NAME_FORMAT = "logback-appender-" + APPENDER_NAME_FORMAT + "-" + THREAD_INDEX_FORMAT;

    public static final int DEFAULT_RING_BUFFER_SIZE = 8192;
    public static final ProducerType DEFAULT_PRODUCER_TYPE = ProducerType.MULTI;
    public static final WaitStrategy DEFAULT_WAIT_STRATEGY = new BlockingWaitStrategy();
    public static final int DEFAULT_DROPPED_WARN_FREQUENCY = 1000;

    private static final RingBufferFullException RING_BUFFER_FULL_EXCEPTION = new RingBufferFullException();
    static {
        RING_BUFFER_FULL_EXCEPTION.setStackTrace(new StackTraceElement[] {new StackTraceElement(AsyncDisruptorAppender.class.getName(), "append(..)", null, -1)});
    }

    /**
     * The size of the {@link RingBuffer}.
     * Defaults to {@value #DEFAULT_RING_BUFFER_SIZE}.
     * <p>
     * Must be a positive power of 2.
     */
    private int ringBufferSize = DEFAULT_RING_BUFFER_SIZE;

    /**
     * The {@link ProducerType} to use to configure the Disruptor.
     * Only set to {@link ProducerType#SINGLE} if only one thread
     * will ever be appending to this appender.
     */
    private ProducerType producerType = DEFAULT_PRODUCER_TYPE;

    /**
     * The {@link WaitStrategy} to used by the RingBuffer
     * when pulling events to be processed by {@link #eventHandler}.
     * <p>
     * By default, a {@link BlockingWaitStrategy} is used, which is the most
     * CPU conservative, but results in a higher latency.
     * If you need lower latency (at the cost of higher CPU usage),
     * consider using a {@link SleepingWaitStrategy} or a {@link PhasedBackoffWaitStrategy}.
     */
    private WaitStrategy waitStrategy = DEFAULT_WAIT_STRATEGY;

    /**
     * Pattern used by the {@link WorkerThreadFactory} to set the
     * handler thread name.
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
     * Other arguments can be made available by subclasses.
     */
    private String threadNameFormat = DEFAULT_THREAD_NAME_FORMAT;

    /**
     * When true, child threads created by this appender will be daemon threads,
     * and therefore allow the JVM to exit gracefully without
     * needing to explicitly shut down the appender.
     * Note that in this case, it is possible for log events to not
     * be handled.
     * <p>
     *
     * When false, child threads created by this appender will not be daemon threads,
     * and therefore will prevent the JVM from shutting down
     * until the appender is explicitly shut down.
     * Set this to false if you want to ensure that every log event
     * prior to shutdown is handled.
     * <p>
     *
     * If you change the {@link #threadFactory}, then this
     * value may not be honored.
     */
    private boolean useDaemonThread = true;

    /**
     * When true, if no status listener is registered, then a default {@link OnConsoleStatusListener}
     * will be registered, so that error messages are seen on the console.
     */
    private boolean addDefaultStatusListener = true;

    /**
     * For every droppedWarnFrequency consecutive dropped events, log a warning.
     * Defaults to {@value #DEFAULT_DROPPED_WARN_FREQUENCY}.
     */
    private int droppedWarnFrequency = DEFAULT_DROPPED_WARN_FREQUENCY;

    /**
     * The {@link ThreadFactory} used to create the handler thread.
     */
    private ThreadFactory threadFactory = new WorkerThreadFactory();

    /**
     * The {@link Disruptor} containing the {@link RingBuffer} onto
     * which to publish events.
     */
    private Disruptor<LogEvent<Event>> disruptor;

    /**
     * Sets the {@link LogEvent#event} to the logback Event.
     * Used when publishing events to the {@link RingBuffer}.
     */
    private EventTranslatorOneArg<LogEvent<Event>, Event> eventTranslator = new LogEventTranslator<>();

    /**
     * Defines what happens when there is an exception during
     * {@link RingBuffer} processing.
     */
    private ExceptionHandler<LogEvent<Event>> exceptionHandler = new LogEventExceptionHandler();

    /**
     * Consecutive number of dropped events.
     */
    private final AtomicLong consecutiveDroppedCount = new AtomicLong();

    /**
     * The {@link EventFactory} used to create {@link LogEvent}s for the RingBuffer.
     */
    private LogEventFactory<Event> eventFactory = new LogEventFactory<>();

    /**
     * Incrementor number used as part of thread names for uniqueness.
     */
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    /**
     * These listeners will be notified when certain events occur on this appender.
     */
    protected final List<Listener> listeners = new ArrayList<>();

    /**
     * Maximum time to wait when appending events to the ring buffer when full before the event
     * is dropped. Use the following values:
     * <ul>
     * <li>{@code -1} to disable timeout and wait until space becomes available.
     * <li>{@code 0} for no timeout and drop the event immediately when the buffer is full.
     * <li>{@code > 0} to retry during the specified amount of time.
     * </ul>
     */
    private Duration appendTimeout = Duration.buildByMilliseconds(0);

    /**
     * Delay between consecutive attempts to append an event in the ring buffer when
     * full.
     */
    private Duration appendRetryFrequency = Duration.buildByMilliseconds(5);
    
    /**
     * How long to wait for in-flight events during shutdown.
     */
    private Duration shutdownGracePeriod = Duration.buildByMinutes(1);

    /**
     * Lock used to limit the number of concurrent threads retrying at the same time
     */
    private final ReentrantLock lock = new ReentrantLock();

    
    /**
     * Event wrapper object used for each element of the {@link RingBuffer}.
     */
    protected static class LogEvent<Event> {
        /**
         * The logback event.
         */
        public volatile Event event;
        
        /**
         * Recycle the instance before it is reused by the RingBuffer.
         */
        public void recycle() {
            this.event = null;
        }
    }

    /**
     * Factory for creating the initial {@link LogEvent}s to populate
     * the {@link RingBuffer}.
     */
    protected static class LogEventFactory<Event> implements EventFactory<LogEvent<Event>> {

        @Override
        public LogEvent<Event> newInstance() {
            return new LogEvent<>();
        }
    }

    /**
     * The default {@link ThreadFactory} used to create the handler thread.
     */
    private class WorkerThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName(calculateThreadName());
            t.setDaemon(useDaemonThread);
            return t;
        }
    }

    /**
     * Sets the {@link LogEvent#event} to the logback Event.
     * Used when publishing events to the {@link RingBuffer}.
     */
    protected static class LogEventTranslator<Event> implements EventTranslatorOneArg<LogEvent<Event>, Event> {

        @Override
        public void translateTo(LogEvent<Event> logEvent, long sequence, Event event) {
            logEvent.event = event;
        }
    }

    /**
     * Defines what happens when there is an exception during
     * {@link RingBuffer} processing.
     *
     * Currently, just logs to the logback context.
     */
    private class LogEventExceptionHandler implements ExceptionHandler<LogEvent<Event>> {

        @Override
        public void handleEventException(Throwable ex, long sequence, LogEvent<Event> event) {
            addError("Unable to process event: " + ex.getMessage(), ex);
        }

        @Override
        public void handleOnStartException(Throwable ex) {
            addError("Unable start disruptor", ex);
        }

        @Override
        public void handleOnShutdownException(Throwable ex) {
            addError("Unable shutdown disruptor", ex);
        }
    }

    /**
     * Clears the event after a delegate event handler has processed the event,
     * so that the event can be garbage collected.
     */
    private static class EventClearingEventHandler<Event> implements SequenceReportingEventHandler<LogEvent<Event>>, LifecycleAware {

        private final EventHandler<LogEvent<Event>> delegate;
        private Sequence sequenceCallback;

        EventClearingEventHandler(EventHandler<LogEvent<Event>> delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public void onEvent(LogEvent<Event> event, long sequence, boolean endOfBatch) throws Exception {
            try {
                delegate.onEvent(event, sequence, endOfBatch);
            } finally {
                /*
                 * Clear the event so that it can be garbage collected.
                 */
                event.recycle();
                
                /*
                 * Notify the BatchEventProcessor that the sequence has progressed.
                 * Without this callback the sequence would not be progressed
                 * until the batch has completely finished.
                 */
                sequenceCallback.set(sequence);
            }
        }

        @Override
        public void onStart() {
            if (delegate instanceof LifecycleAware) {
                ((LifecycleAware) delegate).onStart();
            }
        }

        @Override
        public void onShutdown() {
            if (delegate instanceof LifecycleAware) {
                ((LifecycleAware) delegate).onShutdown();
            }
        }

        @Override
        public void setSequenceCallback(final Sequence sequenceCallback) {
            this.sequenceCallback = sequenceCallback;
        }
    }

    @Override
    public void start() {
        if (addDefaultStatusListener && getStatusManager() != null && getStatusManager().getCopyOfStatusListenerList().isEmpty()) {
            LevelFilteringStatusListener statusListener = new LevelFilteringStatusListener();
            statusListener.setLevelValue(Status.WARN);
            statusListener.setDelegate(new OnConsoleStatusListener());
            statusListener.setContext(getContext());
            statusListener.start();
            getStatusManager().add(statusListener);
        }

        this.disruptor = new Disruptor<>(
                this.eventFactory,
                this.ringBufferSize,
                this.threadFactory,
                this.producerType,
                this.waitStrategy);

        /*
         * Define the exceptionHandler first, so that it applies
         * to all future eventHandlers.
         */
        this.disruptor.setDefaultExceptionHandler(this.exceptionHandler);

        this.disruptor.handleEventsWith(new EventClearingEventHandler<>(createEventHandler()));

        this.disruptor.start();
        super.start();
        fireAppenderStarted();
    }

    @Override
    public void stop() {
        /*
         * Check super.isStarted() instead of isStarted() because subclasses
         * might override isStarted() to perform other comparisons that we don't
         * want to check here.  Those should be checked by subclasses
         * prior to calling super.stop()
         */
        if (!super.isStarted()) {
            return;
        }
        
        /*
         * Don't allow any more events to be appended.
         */
        super.stop();

        
        /*
         * Shutdown Disruptor
         *
         * Calling Disruptor#shutdown() will wait until all enqueued events are fully processed,
         * but this waiting happens in a busy-spin. To avoid wasting CPU we wait for at most the configured
         * grace period before asking the Disruptor for an immediate shutdown.
         */
        long deadline = getShutdownGracePeriod().getMilliseconds() < 0 ? Long.MAX_VALUE : System.currentTimeMillis() + getShutdownGracePeriod().getMilliseconds();
        while (!isRingBufferEmpty() && (System.currentTimeMillis() < deadline)) {
            LockSupport.parkNanos(SLEEP_TIME_DURING_SHUTDOWN);
        }
        
        this.disruptor.halt();

        if (!isRingBufferEmpty()) {
            addWarn("Some queued events have not been logged due to requested shutdown");
        }
        fireAppenderStopped();
    }

    
    /**
     * Create the {@link EventHandler} to process events as they become available from the RingBuffer.
     * This method is invoked when the appender is started by {@link #start()} and a new {@link Disruptor} is initialized.
     * 
     * @return a {@link EventHandler} instance.
     */
    protected abstract EventHandler<LogEvent<Event>> createEventHandler();

    
    /**
     * Test whether the ring buffer is empty or not
     * 
     * @return {@code true} if the ring buffer is empty, {@code false} otherwise
     */
    protected boolean isRingBufferEmpty() {
        return this.disruptor.getRingBuffer().hasAvailableCapacity(this.getRingBufferSize());
    }
    
    @Override
    protected void append(Event event) {
        long startTime = System.nanoTime();
        
        try {
            prepareForDeferredProcessing(event);
        } catch (RuntimeException e) {
            addWarn("Unable to prepare event for deferred processing. Event output might be missing data.", e);
        }

        try {
            if (enqueue(event)) {
                // Log warning if we had drop before
                //
                long consecutiveDropped = this.consecutiveDroppedCount.get();
                if (consecutiveDropped != 0 && this.consecutiveDroppedCount.compareAndSet(consecutiveDropped, 0L)) {
                    addWarn("Dropped " + consecutiveDropped + " total events due to ring buffer at max capacity [" + this.ringBufferSize + "]");
                }
                
                // Notify listeners
                //
                fireEventAppended(event, System.nanoTime() - startTime);
                
            } else {
                // Log a warning status about the failure
                //
                long consecutiveDropped = this.consecutiveDroppedCount.incrementAndGet();
                if ((consecutiveDropped % this.droppedWarnFrequency) == 1) {
                    addWarn("Dropped " + consecutiveDropped + " events (and counting...) due to ring buffer at max capacity [" + this.ringBufferSize + "]");
                }
                
                // Notify listeners
                //
                fireEventAppendFailed(event, RING_BUFFER_FULL_EXCEPTION);
            }
            
        } catch (ShutdownInProgressException e) {
            // Same message as if Appender#append is called after the appender is stopped...
            addWarn("Attempted to append to non started appender [" + this.getName() + "].");
            
        } catch (InterruptedException e) {
            // be silent but re-interrupt the thread
            Thread.currentThread().interrupt();
        }
    }

    
    /**
     * Enqueue an event in the ring buffer, retrying if allowed by the configuration.
     * 
     * @param event the event to add to the ring buffer
     * @return {@code true} if the event is successfully enqueued, {@code false} if the event
     *         could not be added to the ring buffer.
     * @throws ShutdownInProgressException thrown when the appender is shutdown while retrying
     *         to enqueue the event
     * @throws InterruptedException thrown when the logging thread is interrupted while retrying
     */
    private boolean enqueue(Event event) throws ShutdownInProgressException, InterruptedException {
        // Try enqueue the "normal" way
        //
        if (this.disruptor.getRingBuffer().tryPublishEvent(this.eventTranslator, event)) {
            return true;
        }
        
        // Drop event immediately when no retry
        //
        if (this.appendTimeout.getMilliseconds() == 0) {
            return false;
        }
        
        // Limit retries to a single thread at once to avoid burning CPU cycles "for nothing"
        // in CPU constraint environments.
        //
        long deadline = Long.MAX_VALUE;
        if (this.appendTimeout.getMilliseconds() < 0) {
            lock.lockInterruptibly();
            
        } else {
            deadline = System.currentTimeMillis() + this.appendTimeout.getMilliseconds();
            if (!lock.tryLock(this.appendTimeout.getMilliseconds(), TimeUnit.MILLISECONDS)) {
                return false;
            }
        }
        
        // Retry until deadline
        //
        long backoff = 1L;
        long backoffLimit = TimeUnit.MILLISECONDS.toNanos(this.appendRetryFrequency.getMilliseconds());
        
        try {
            do {
                if (!isStarted()) {
                    throw new ShutdownInProgressException();
                }

                if (deadline <= System.currentTimeMillis()) {
                    return false;
                }
                
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                
                LockSupport.parkNanos(backoff);
                backoff = Math.min(backoff * 2, backoffLimit);
                
            } while (!this.disruptor.getRingBuffer().tryPublishEvent(this.eventTranslator, event));

            return true;
            
        } finally {
            lock.unlock();
        }
    }
    
    protected void prepareForDeferredProcessing(Event event) {
        event.prepareForDeferredProcessing();
    }
    
    
    protected String calculateThreadName() {
        List<Object> threadNameFormatParams = getThreadNameFormatParams();
        return String.format(threadNameFormat, threadNameFormatParams.toArray());
    }

    protected List<Object> getThreadNameFormatParams() {
        return Arrays.<Object>asList(
            getName(),
            threadNumber.incrementAndGet());
    }

    protected void fireAppenderStarted() {
        safelyFireEvent(l -> l.appenderStarted(this));
    }

    protected void fireAppenderStopped() {
        safelyFireEvent(l -> l.appenderStopped(this));
    }

    protected void fireEventAppended(Event event, long durationInNanos) {
        safelyFireEvent(l -> l.eventAppended(this, event, durationInNanos));
    }

    protected void fireEventAppendFailed(Event event, Throwable reason) {
        safelyFireEvent(l -> l.eventAppendFailed(this, event, reason));
    }

    protected void safelyFireEvent(Consumer<Listener> callback) {
        for (Listener listener : listeners) {
            try {
                callback.accept(listener);
            } catch (Exception e) {
                addError("Failed to invoke listener " + listener, e);
            }
        }
    }

    protected void setEventFactory(LogEventFactory<Event> eventFactory) {
        this.eventFactory = eventFactory;
    }

    protected EventTranslatorOneArg<LogEvent<Event>, Event> getEventTranslator() {
        return eventTranslator;
    }

    protected void setEventTranslator(EventTranslatorOneArg<LogEvent<Event>, Event> eventTranslator) {
        this.eventTranslator = eventTranslator;
    }

    protected Disruptor<LogEvent<Event>> getDisruptor() {
        return disruptor;
    }

    public String getThreadNameFormat() {
        return threadNameFormat;
    }
    /**
     * Pattern used by the to set the handler thread names.
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
     * Other arguments can be made available by subclasses.
     * 
     * @param threadNameFormat the thread name format pattern
     */
    public void setThreadNameFormat(String threadNameFormat) {
        this.threadNameFormat = Objects.requireNonNull(threadNameFormat);
    }

    /**
     * Returns the maximum number of events allowed in the queue.
     * 
     * @return the size of the ring buffer
     */
    public int getRingBufferSize() {
        return ringBufferSize;
    }
    
    /**
     * Sets the size of the {@link RingBuffer}.
     * Must be a positive power of 2.
     * Defaults to {@value #DEFAULT_RING_BUFFER_SIZE}.
     * 
     * <p>If the handler thread is not as fast as the producing threads, then the {@link RingBuffer}
     * will eventually fill up, at which point events will be dropped or the producing threads are
     * blocked depending on {@link #appendTimeout}.
     *
     * @param ringBufferSize the maximum number of entries in the queue.
     */
    public void setRingBufferSize(int ringBufferSize) {
        if (ringBufferSize <= 0 || !isPowerOfTwo(ringBufferSize)) {
            throw new IllegalArgumentException("ringBufferSize must be a positive power of 2");
        }
        this.ringBufferSize = ringBufferSize;
    }

    /**
     * Get the {@link ProducerType} configured for the Disruptor.
     * 
     * @return the {@link ProducerType}.
     */
    public ProducerType getProducerType() {
        return producerType;
    }
    
    /**
     * The {@link ProducerType} to use to configure the Disruptor.
     * By default this is {@link ProducerType#MULTI}.
     * 
     * Can be set to {@link ProducerType#SINGLE} for increase performance if (and only if) only
     * one thread will ever be appending to this appender.
     * 
     * <p>WARNING: unexpected behavior may occur if this parameter is set to {@link ProducerType#SINGLE}
     * and multiple threads are appending to this appender.
     * 
     * @deprecated ProducerType will be fixed to MULTI in future release and this method removed without any replacement.
     * @param producerType the type of producer
     */
    @Deprecated
    public void setProducerType(ProducerType producerType) {
        this.producerType = Objects.requireNonNull(producerType);
        addWarn("<producerType> is deprecated and will be removed without replacement in future release");
    }

    public WaitStrategy getWaitStrategy() {
        return waitStrategy;
    }
    public void setWaitStrategy(WaitStrategy waitStrategy) {
        this.waitStrategy = Objects.requireNonNull(waitStrategy);
    }

    public void setWaitStrategyType(String waitStrategyType) {
        setWaitStrategy(WaitStrategyFactory.createWaitStrategyFromString(waitStrategyType));
    }
    
    public Duration getAppendRetryFrequency() {
        return appendRetryFrequency;
    }
    public void setAppendRetryFrequency(Duration appendRetryFrequency) {
        if (Objects.requireNonNull(appendRetryFrequency).getMilliseconds() <= 0) {
            throw new IllegalArgumentException("appendRetryFrequency must be > 0");
        }
        this.appendRetryFrequency = appendRetryFrequency;
    }
    
    public Duration getAppendTimeout() {
        return appendTimeout;
    }
    public void setAppendTimeout(Duration appendTimeout) {
        this.appendTimeout = Objects.requireNonNull(appendTimeout);
    }
    
    public void setShutdownGracePeriod(Duration shutdownGracePeriod) {
        this.shutdownGracePeriod = Objects.requireNonNull(shutdownGracePeriod);
    }
    public Duration getShutdownGracePeriod() {
        return shutdownGracePeriod;
    }
    
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }
    public void setThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = Objects.requireNonNull(threadFactory);
    }

    public int getDroppedWarnFrequency() {
        return droppedWarnFrequency;
    }
    public void setDroppedWarnFrequency(int droppedWarnFrequency) {
        this.droppedWarnFrequency = droppedWarnFrequency;
    }

    public boolean isDaemon() {
        return useDaemonThread;
    }
    public void setDaemon(boolean useDaemonThread) {
        this.useDaemonThread = useDaemonThread;
    }

    public void addListener(Listener listener) {
        this.listeners.add(Objects.requireNonNull(listener));
    }
    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    public boolean isAddDefaultStatusListener() {
        return addDefaultStatusListener;
    }

    public void setAddDefaultStatusListener(boolean addDefaultStatusListener) {
        this.addDefaultStatusListener = addDefaultStatusListener;
    }
    
    
    private static boolean isPowerOfTwo(int x) {
        /* First x in the below expression is for the case when x is 0 */
        return x != 0 && ((x & (x - 1)) == 0);
    }
}
