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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.DeferredProcessingAware;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
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
 * Subclasses are required to set the {@link #eventHandler} to define
 * the logic that executes in the handler thread.
 * For example, {@link DelegatingAsyncDisruptorAppender} for will delegate
 * appending of the event to another appender in the handler thread.
 *
 * @param <Event> type of event ({@link ILoggingEvent} or {@link IAccessEvent}).
 */
public abstract class AsyncDisruptorAppender<Event extends DeferredProcessingAware> extends UnsynchronizedAppenderBase<Event> {
    
    public static final int DEFAULT_RING_BUFFER_SIZE = 8192;
    public static final ProducerType DEFAULT_PRODUCER_TYPE = ProducerType.MULTI;
    public static final SleepingWaitStrategy DEFAULT_WAIT_STRATEGY = new SleepingWaitStrategy();
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
     * The {@link WaitStrategy} to use when publishing events to
     * the ringBuffer.  This is never really used right now,
     * since this appender always uses <tt>tryPublishEvent</tt>
     * instead of <tt>publishEvent</tt>
     */
    private WaitStrategy waitStrategy = DEFAULT_WAIT_STRATEGY;
    
    /**
     * Used as a prefix by the {@link WorkerThreadFactory} to set the
     * handler thread name.
     * Defaults to {@value #DEFAULT_THREAD_NAME_PREFIX}.
     * 
     * If you change the {@link #threadFactory}, then this
     * value may not be honored.
     */
    private String threadNamePrefix = DEFAULT_THREAD_NAME_PREFIX;
    
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
     * The {@link ExecutorService} used to execute the handler task.
     */
    private ExecutorService executorService;
    
    /**
     * The {@link Disruptor} containing the {@link RingBuffer} onto
     * which to publish events.
     */
    private Disruptor<LogEvent<Event>> disruptor;
    
    /**
     * Sets the {@link LogEvent#event} to the logback Event.
     * Used when publishing events to the {@link RingBuffer}. 
     */
    private final EventTranslatorOneArg<LogEvent<Event>, Event> eventTranslator = new LogEventTranslator<Event>();
    
    /**
     * Used by the handler thread to process the event.
     */
    private EventHandler<LogEvent<Event>> eventHandler;
    
    /**
     * Defines what happens when there is an exception during
     * {@link RingBuffer} processing.
     */
    private ExceptionHandler exceptionHandler = new LogEventExceptionHandler();
    
    /**
     * Consecutive number of dropped events.
     */
    private final AtomicLong consecutiveDroppedCount = new AtomicLong(); 
    
    /**
     * Event wrapper object used for each element of the {@link RingBuffer}.
     */
    protected static final class LogEvent<Event> {
        /**
         * The logback event.
         */
        public Event event;
    }
    
    /**
     * Factory for creating the initial {@link LogEvent}s to populate
     * the {@link RingBuffer}.
     */
    private static final class LogEventFactory<Event> implements EventFactory<LogEvent<Event>> {

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
            return t;
        }
    }
    
    /**
     * Sets the {@link LogEvent#event} to the logback Event.
     * Used when publishing events to the {@link RingBuffer}. 
     */
    private static class LogEventTranslator<Event> implements EventTranslatorOneArg<LogEvent<Event>, Event> {

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
    private class LogEventExceptionHandler implements ExceptionHandler {

        @Override
        public void handleEventException(Throwable ex, long sequence, Object event) {
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
    
    @SuppressWarnings("unchecked")
    @Override
    public void start() {
        if (this.eventHandler == null) {
            addError("No eventHandler was configured for appender " + name + ".");
            return;
        }
        
        this.executorService = Executors.newCachedThreadPool(this.threadFactory);
        
        this.disruptor = new Disruptor<LogEvent<Event>>(
                new LogEventFactory<Event>(),
                this.ringBufferSize,
                this.executorService,
                this.producerType,
                this.waitStrategy);
        
        /*
         * Define the exceptionHandler first, so that it applies
         * to all future eventHandlers.
         */
        this.disruptor.handleExceptionsWith(this.exceptionHandler);
        
        this.disruptor.handleEventsWith(this.eventHandler);
        
        this.disruptor.start();
        super.start();
    }
    
    @Override
    public void stop() {
        if (!isStarted()) {
            return;
        }
        /*
         * Don't allow any more events to be appended.
         */
        super.stop();
        this.disruptor.shutdown();
        
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
    
}
