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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.DeferredProcessingAware;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.PhasedBackoffWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.TimeoutException;
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
 * This appender will never block the logging thread, since it uses
 * {@link RingBuffer#tryPublishEvent(EventTranslatorOneArg, Object)}
 * to publish events (rather than {@link RingBuffer#publishEvent(EventTranslatorOneArg, Object)}).
 * <p>
 * 
 * If the RingBuffer is full, and the event cannot be published, 
 * the event will be dropped.  A warning message will be logged to 
 * logback's context every {@link #droppedWarnFrequency} consecutive dropped events.
 * <p>
 * 
 * A single handler thread will be used to handle the actual handling of the event.
 * <p>
 * 
 * Subclasses are required to set the {@link #eventHandler} to define
 * the logic that executes in the handler thread.
 * For example, {@link DelegatingAsyncDisruptorAppender} for will delegate
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
public abstract class AsyncDisruptorAppender<Event extends DeferredProcessingAware> extends UnsynchronizedAppenderBase<Event> {
    
    public static final int DEFAULT_RING_BUFFER_SIZE = 8192;
    public static final ProducerType DEFAULT_PRODUCER_TYPE = ProducerType.MULTI;
    public static final WaitStrategy DEFAULT_WAIT_STRATEGY = new BlockingWaitStrategy();
    public static final String DEFAULT_THREAD_NAME_PREFIX = "logback-async-disruptor-appender-";
    public static final int DEFAULT_DROPPED_WARN_FREQUENCY = 1000;
    
    /**
     * The size of the {@link RingBuffer}.
     * Defaults to {@value #DEFAULT_RING_BUFFER_SIZE}.
     * If the handler thread is not as fast as the producing threads,
     * then the {@link RingBuffer} will eventually fill up,
     * at which point events will be dropped.
     * <p>
     * Must be a positive power of 2.
     */
    private int ringBufferSize = DEFAULT_RING_BUFFER_SIZE;
    
    /**
     * The {@link ProducerType} to use to configure the disruptor.
     * By default this is {@link ProducerType#MULTI}.
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
     * Used as a prefix by the {@link WorkerThreadFactory} to set the
     * handler thread name.
     * Defaults to {@value #DEFAULT_THREAD_NAME_PREFIX}.
     * <p>
     * 
     * If you change the {@link #threadFactory}, then this
     * value may not be honored.
     */
    private String threadNamePrefix = DEFAULT_THREAD_NAME_PREFIX;
    
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
     * For every droppedWarnFrequency consecutive dropped events, log a warning.
     * Defaults to {@value #DEFAULT_DROPPED_WARN_FREQUENCY}. 
     */
    private int droppedWarnFrequency = DEFAULT_DROPPED_WARN_FREQUENCY;
    
    /**
     * The {@link ThreadFactory} used to create the handler thread.
     */
    private ThreadFactory threadFactory = new WorkerThreadFactory();
    
    /**
     * The {@link ScheduledExecutorService} used to execute the handler task.
     */
    private ScheduledThreadPoolExecutor executorService;
    
    /**
     * Size of the thread pool to create.
     */
    private int threadPoolCoreSize = 1;
    
    /**
     * The {@link Disruptor} containing the {@link RingBuffer} onto
     * which to publish events.
     */
    private Disruptor<LogEvent<Event>> disruptor;
    
    /**
     * Sets the {@link LogEvent#event} to the logback Event.
     * Used when publishing events to the {@link RingBuffer}. 
     */
    private EventTranslatorOneArg<LogEvent<Event>, Event> eventTranslator = new LogEventTranslator<Event>();
    
    /**
     * Used by the handler thread to process the event.
     */
    private EventHandler<LogEvent<Event>> eventHandler;
    
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
    private LogEventFactory<Event> eventFactory = new LogEventFactory<Event>();
    
    /**
     * Event wrapper object used for each element of the {@link RingBuffer}.
     */
    protected static class LogEvent<Event> {
        /**
         * The logback event.
         */
        public volatile Event event;
    }
    
    /**
     * Factory for creating the initial {@link LogEvent}s to populate
     * the {@link RingBuffer}.
     */
    private static class LogEventFactory<Event> implements EventFactory<LogEvent<Event>> {

        @Override
        public LogEvent<Event> newInstance() {
            return new LogEvent<Event>();
        }
    }

    /**
     * The default {@link ThreadFactory} used to create the handler thread.
     */
    private class WorkerThreadFactory implements ThreadFactory {
        
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName(threadNamePrefix + threadNumber.getAndIncrement());
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
    private static class EventClearingEventHandler<Event> implements EventHandler<LogEvent<Event>>, LifecycleAware {
        
        private final EventHandler<LogEvent<Event>> delegate;
        
        public EventClearingEventHandler(EventHandler<LogEvent<Event>> delegate) {
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
                event.event = null;
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
        
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void start() {
        if (this.eventHandler == null) {
            addError("No eventHandler was configured for appender " + name + ".");
            return;
        }
        
        this.executorService = new ScheduledThreadPoolExecutor(
                getThreadPoolCoreSize(),
                this.threadFactory);
        
        setRemoveOnCancelPolicy();
        
        this.disruptor = new Disruptor<LogEvent<Event>>(
                this.eventFactory,
                this.ringBufferSize,
                this.executorService,
                this.producerType,
                this.waitStrategy);
        
        /*
         * Define the exceptionHandler first, so that it applies
         * to all future eventHandlers.
         */
        this.disruptor.handleExceptionsWith(this.exceptionHandler);
        
        this.disruptor.handleEventsWith(new EventClearingEventHandler<Event>(this.eventHandler));
        
        this.disruptor.start();
        super.start();
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
        try {
            this.disruptor.shutdown(1, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            addWarn("Some queued events have not been logged due to requested shutdown");
        }
        
        this.executorService.shutdown();
        
        try {
            if (!this.executorService.awaitTermination(1, TimeUnit.MINUTES)) {
                addWarn("Some queued events have not been logged due to requested shutdown");
            }
        } catch (InterruptedException e) {
            addWarn("Some queued events have not been logged due to requested shutdown", e);
        }
    }

    @Override
    protected void append(Event event) {
        prepareForDeferredProcessing(event);
        
        if (!this.disruptor.getRingBuffer().tryPublishEvent(this.eventTranslator, event)) {
            long consecutiveDropped = this.consecutiveDroppedCount.incrementAndGet();
            if ((consecutiveDropped) % this.droppedWarnFrequency == 1) {
                addWarn("Dropped " + consecutiveDropped + " events (and counting...) due to ring buffer at max capacity [" + this.ringBufferSize + "]");
            }
        } else {
            long consecutiveDropped = this.consecutiveDroppedCount.get();
            if (consecutiveDropped != 0 && this.consecutiveDroppedCount.compareAndSet(consecutiveDropped, 0L)) {
                addWarn("Dropped " + consecutiveDropped + " total events due to ring buffer at max capacity [" + this.ringBufferSize + "]");
            }
        }
    }

    protected void prepareForDeferredProcessing(Event event) {
        event.prepareForDeferredProcessing();
    }
    
    @IgnoreJRERequirement
    private void setRemoveOnCancelPolicy() {
        /*
         * ScheduledThreadPoolExecutor.setRemoveOnCancelPolicy was added in 1.7.
         * 
         * Don't try to invoke it if running on a JVM less than 1.7
         * 
         * If running on less than 1.7, then shutdown will wait for all tasks
         * to complete (even if they are cancelled), or the max wait timeout,
         * whichever comes first.
         */
        if (isRemoveOnCancelPolicyPossible()) {
            /*
             * This ensures that cancelled tasks
             * (such as the keepAlive task in AbstractLogstashTcpSocketAppender)
             * do not hold up shutdown.
             */
            this.executorService.setRemoveOnCancelPolicy(true);
        }
    }
    
    private boolean isRemoveOnCancelPolicyPossible() {
        try {
            ScheduledThreadPoolExecutor.class.getMethod("setRemoveOnCancelPolicy", Boolean.TYPE);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        } catch (SecurityException e) {
            return false;
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
    
    protected ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    protected Disruptor<LogEvent<Event>> getDisruptor() {
        return disruptor;
    }
    
    protected int getThreadPoolCoreSize() {
        return threadPoolCoreSize;
    }
    protected void setThreadPoolCoreSize(int threadPoolCoreSize) {
        this.threadPoolCoreSize = threadPoolCoreSize;
    }
    
    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }
    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }
    
    public int getRingBufferSize() {
        return ringBufferSize;
    }
    public void setRingBufferSize(int ringBufferSize) {
        this.ringBufferSize = ringBufferSize;
    }

    public ProducerType getProducerType() {
        return producerType;
    }
    public void setProducerType(ProducerType producerType) {
        this.producerType = producerType;
    }
    
    public WaitStrategy getWaitStrategy() {
        return waitStrategy;
    }
    public void setWaitStrategy(WaitStrategy waitStrategy) {
        this.waitStrategy = waitStrategy;
    }
    
    public void setWaitStrategyType(String waitStrategyType) {
        setWaitStrategy(WaitStrategyFactory.createWaitStrategyFromString(waitStrategyType));
    }
    
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }
    public void setThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }
    
    public int getDroppedWarnFrequency() {
        return droppedWarnFrequency;
    }
    public void setDroppedWarnFrequency(int droppedWarnFrequency) {
        this.droppedWarnFrequency = droppedWarnFrequency;
    }
    
    protected EventHandler<LogEvent<Event>> getEventHandler() {
        return eventHandler;
    }
    protected void setEventHandler(EventHandler<LogEvent<Event>> eventHandler) {
        this.eventHandler = eventHandler;
    }
    
    public boolean isDaemon() {
        return useDaemonThread;
    }
    public void setDaemon(boolean useDaemonThread) {
        this.useDaemonThread = useDaemonThread;
    }
    
}
