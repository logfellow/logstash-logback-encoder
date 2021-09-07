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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.lang.Thread.State;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import net.logstash.logback.appender.AsyncDisruptorAppender.LogEvent;
import net.logstash.logback.appender.listener.AppenderListener;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.BasicStatusManager;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusManager;
import com.lmax.disruptor.EventHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AsyncDisruptorAppenderTest {

    @InjectMocks
    private AsyncDisruptorAppender<ILoggingEvent, AppenderListener<ILoggingEvent>> appender = new AsyncDisruptorAppender<ILoggingEvent, AppenderListener<ILoggingEvent>>() { };
    
    @Mock
    private EventHandler<LogEvent<ILoggingEvent>> eventHandler;
    
    private StatusManager statusManager = new BasicStatusManager();
    
    @Mock
    private ILoggingEvent event1;
    
    @Mock
    private ILoggingEvent event2;
    
    @Mock
    private AppenderListener<ILoggingEvent> listener;
    
    private ExecutorService executorService = Executors.newCachedThreadPool();

    
    @BeforeEach
    public void setup() {
        LoggerContext ctx = new LoggerContext();
        ctx.setStatusManager(statusManager);
        ctx.start();
        
        appender.setContext(ctx);
        appender.addListener(listener);
    }
    
    @AfterEach
    public void tearDown() {
        appender.stop();
        executorService.shutdownNow();
    }
    
    
    /*
     * Verify that AppenderListenre#start()/stop() are invoked
     */
    @Test
    public void startStopListeners() {
        appender.start();
        verify(listener).appenderStarted(appender);
        
        appender.stop();
        verify(listener).appenderStopped(appender);
    }
    
    
    /*
     * Verify that the EventHandler is invoked and LogEvent cleared after processing
     */
    @Test
    public void testEventHandlerCalled() throws Exception {

        TestEventHandler eventHandler = new TestEventHandler();
        appender.setEventHandler(eventHandler);
        appender.start();
        
        appender.append(event1);
        
        verify(listener).eventAppended(eq(appender), eq(event1), anyLong());
        
        // Wait until "event1" is async processed
        await().until(() -> !eventHandler.events.isEmpty());
        assertThat(eventHandler.events).containsExactly(event1);
        
        // Assert that "event1" has been prepared for deferred processing
        verify(event1).prepareForDeferredProcessing();
        
        // Assert the LogEvent holder is cleared after event is processed by the handler
        assertThat(eventHandler.getLogEventHolders())
            .hasSize(1)
            .allMatch(logevent -> logevent.event == null);
    }
    
    
    @Test
    public void testThreadDaemon() throws Exception {
        
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
            }
        };
        
        appender.setDaemon(true);
        assertThat(appender.getThreadFactory().newThread(runnable).isDaemon()).isTrue();
        
        appender.setDaemon(false);
        assertThat(appender.getThreadFactory().newThread(runnable).isDaemon()).isFalse();
    }

    
    @Test
    public void testThreadName() throws Exception {
        
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
            }
        };
        
        appender.setThreadNameFormat("threadNamePrefix");
        assertThat(appender.getThreadFactory().newThread(runnable).getName()).startsWith("threadNamePrefix");
    }

    
    /*
     * Assert event is dropped when buffer is full
     */
    @Test
    public void testEventDroppedWhenFull() throws Exception {
        final CountDownLatch eventHandlerWaiter = new CountDownLatch(1);
        
        try {
            TestEventHandler eventHandler = new TestEventHandler(eventHandlerWaiter);

            appender.setRingBufferSize(1);
            appender.setAppendTimeout(toLogback(Duration.ZERO)); // no timeout - drop when full
            appender.setEventHandler(eventHandler);
            appender.start();
            
            /*
             * First event blocks the ring buffer until eventHandlerWaiter is released
             */
            appender.append(event1);
            await().until(() -> eventHandlerWaiter.getCount() == 1); // wait until the handler is invoked before going any further
            
            /*
             * RingBuffer is full - second event is dropped and warning emitted
             */
            appender.append(event2);

            /*
             * Failed to append event...
             */
            verify(listener).eventAppendFailed(eq(appender), eq(event2), any());
            
            // NOTE:
            //   no need to wait for the completion of async processing -> everything happens
            //   on the logging thread
            
            /*
             * ... event dropped -> WARN status message
             */
            assertThat(statusManager.getCopyOfStatusList())
                .hasSize(1)
                .allMatch(s -> s.getMessage().startsWith("Dropped"));
            
            
        } finally {
            eventHandlerWaiter.countDown();
        }
    }
    
    
    @SuppressWarnings("unchecked")
    @Test
    public void testEventHandlerThrowsException() throws Exception {
        appender.start();
        
        /*
         *  Make the EventHandler throw an exception when called
         */
        final Throwable throwable = new RuntimeException("message");
        doThrow(throwable).when(eventHandler).onEvent(any(LogEvent.class), anyLong(), anyBoolean());
        
        /*
         *  Append event
         */
        appender.append(event1);
        
        /*
         * Event successfully appended...
         */
        verify(listener).eventAppended(eq(appender), eq(event1), anyLong());
        
        // NOTE:
        //   need to wait until async processing is completed.
        //   In this case, waiting for the event handler to be called is not enough -> it throws an exception
        //   that needs to be captured by the ExceptionHandler then logged... Better to wait for the StatusManager
        //   to contain what we expect...
        
        
        /*
         * ... but async processing failed -> ERROR status message
         */
        await().untilAsserted(() ->
            assertThat(statusManager.getCopyOfStatusList())
                .hasSize(1)
                .allMatch(s -> s.getMessage().startsWith("Unable to process event") && s.getLevel() == Status.ERROR));
    }
    
    
    /*
     * Appender is configured to block indefinitely when ring buffer is full
     */
    @Test
    public void appendBlockingWhenFull() {
        CountDownLatch eventHandlerWaiter = new CountDownLatch(1);

        try {
            TestEventHandler eventHandler = new TestEventHandler(eventHandlerWaiter);
            appender.setRingBufferSize(1);
            appender.setAppendTimeout(toLogback(Duration.ofMillis(-1))); // block until space is available
            appender.setEventHandler(eventHandler);
            appender.start();
            
            /*
             * First event blocks the ring buffer until eventHandlerWaiter is released
             */
            appender.append(event1);
            await().until(() -> eventHandlerWaiter.getCount() == 1); // wait until the handler is actually invoked before going any further
            
            /*
             * Publishing the second event is blocked until the first is released (buffer full)
             * Wait until async exec is actually started and assert thread is blocked
             */
            final AtomicReference<Thread> asyncThread = new AtomicReference<>();
            Future<?> future = execute(() -> {
                asyncThread.set(Thread.currentThread());
                appender.append(event2);
            });
            
            await().until(() -> asyncThread.get() != null && asyncThread.get().getState().equals(State.TIMED_WAITING));
            assertThat(future).isNotDone();
            
            /*
             * Release the handler -> both events are now unblocked
             */
            eventHandlerWaiter.countDown();
    
            await().untilAsserted(() -> assertThat(eventHandler.getEvents()).containsExactly(event1, event2));
            assertThat(future).isDone();
            assertThat(statusManager.getCopyOfStatusList()).isEmpty();
            
        } finally {
            eventHandlerWaiter.countDown();
        }
    }
    
    
    /*
     * Appender configured to block with a timeout -> assert appending threads are blocked for the
     * configured timeout.
     */
    @Test
    public void appendBlockingWithTimeout() throws Exception {
        // Block for the specified timeout
        final Duration timeout = Duration.ofMillis(150);
        
        final CountDownLatch eventHandlerWaiter = new CountDownLatch(1);
        
        try {
            TestEventHandler eventHandler = new TestEventHandler(eventHandlerWaiter);
            appender.setRingBufferSize(1);
            appender.setAppendTimeout(toLogback(timeout));
            appender.setEventHandler(eventHandler);
            appender.start();
            
            /*
             * First event blocks the ring buffer until eventHandlerWaiter is released
             */
            appender.append(event1);
            await().until(() -> eventHandlerWaiter.getCount() == 1); // wait until the handler is actually invoked before going any further
            
            
            /*
             * Second event is blocked until the first is released (buffer full) - but no more than the configured timeout
             */
            Future<?> future = execute(() -> appender.append(event2));
            
            // wait for the timeout
            await().atLeast(timeout).and().atMost(timeout.plusMillis(100)).until(future::isDone);
            
            // a WARN status is logged
            assertThat(statusManager.getCopyOfStatusList())
                .hasSize(1)
                .allMatch(s -> s.getMessage().startsWith("Dropped"));
            
            // listeners invoked with appendFailed
            verify(listener).eventAppendFailed(eq(appender), eq(event2), any());
            
            
            /*
             * Unlock the handler and assert only the first event went through
             */
            eventHandlerWaiter.countDown();
            await().untilAsserted(() -> assertThat(eventHandler.getEvents()).containsExactly(event1));
            
        } finally {
            eventHandlerWaiter.countDown();
        }
    }
    
    
    /*
     * Appender configured to block until space is available -> assert threads blocked waiting for free space are
     * released when the appender is stopped
     */
    @Test
    public void appendBlockingReleasedOnStop() {
        final CountDownLatch eventHandlerWaiter = new CountDownLatch(1);
        
        try {
            TestEventHandler eventHandler = new TestEventHandler(eventHandlerWaiter);
            appender.setRingBufferSize(1);
            appender.setAppendTimeout(toLogback(Duration.ofMillis(-1))); // block until space is available
            appender.setShutdownGracePeriod(toLogback(Duration.ofMillis(0))); // don't want to wait for inflight events...
            appender.setEventHandler(eventHandler);
            appender.start();
            
            /*
             * First event will block the ring buffer until eventHandlerWaiter is released
             */
            appender.append(event1);
            await().until(() -> eventHandlerWaiter.getCount() == 1); // wait until the handler is actually invoked before going any further
            
            /*
             * Publishing the second event is blocked until the first is released (buffer full)
             */
            Future<?> future = execute(() -> appender.append(event2));
            
            /*
             * Stop appender
             */
            appender.stop();
            
            // appending thread is released
            await().until(future::isDone);

            // no events handled
            assertThat(eventHandler.getEvents()).isEmpty();

            // no listener invoked
            verify(listener, never()).eventAppendFailed(eq(appender), eq(event2), any());
            
        } finally {
            eventHandlerWaiter.countDown();
        }
    }
    
    
    /*
     * Assert that LogEvent are released from the RingBuffer before the end of a batch.
     */
    @Test
    public void logEventsClearedBeforeEndOfBatch() throws Exception {
        final CyclicBarrier barrier = new CyclicBarrier(2);
        
        try {
            TestEventHandler eventHandler = new TestEventHandler(barrier);
            appender.setRingBufferSize(4);
            appender.setShutdownGracePeriod(toLogback(Duration.ofMillis(0))); // don't want to wait for inflight events...
            appender.setEventHandler(eventHandler);
            appender.setAddDefaultStatusListener(true);
            appender.start();
            
            /*
             * Append enough events to fill the buffer
             */
            appender.append(event1);
            appender.append(event1);
            appender.append(event1);
            appender.append(event1);
            
            /*
             * We now have 1 event followed by a batch of 3.
             * Release 2 events which means the batch is not yet fully processed but we should have room
             * for 2 additional events in the buffer.
             */
            barrier.await();
            barrier.await();
            await().until(() -> eventHandler.getEvents().size() == 2);

            appender.append(event1);
            appender.append(event1);
            verify(listener, never()).eventAppendFailed(eq(appender), any(), any()); // nothing dropped - they all fit in the buffer
            
            /*
             * Release them all and assert we got 6 in total
             */
            barrier.await();
            barrier.await();
            barrier.await();
            barrier.await();
            
            await().until(() -> eventHandler.getEvents().size() == 6);
            
        } finally {
            barrier.reset();
        }
    }
    
    
    @SuppressWarnings("deprecation")
    @Test
    public void testConfigParams() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> appender.setRingBufferSize(-1));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> appender.setRingBufferSize(3));
        
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> appender.setAppendRetryFrequency(toLogback(Duration.ofMillis(-1))));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> appender.setAppendRetryFrequency(null));

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> appender.setThreadNameFormat(null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> appender.setProducerType(null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> appender.setWaitStrategy(null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> appender.setThreadFactory(null));
    }
    
    
    
    // --------------------------------------------------------------------------------------------

   
    private Future<?> execute(Runnable runnable) {
        return executorService.submit(runnable);
    }
       
    private static class TestEventHandler implements EventHandler<LogEvent<ILoggingEvent>> {
        // LogEvent holders - may be reused multiple times with different ILoggingEvent payload
        private final List<LogEvent<ILoggingEvent>> logEventHolders = new ArrayList<>();
        // Captured ILoggingEvent (need to be extracted from the LogEvent holder before it is reset)
        private final List<ILoggingEvent> events = new ArrayList<>();
        private CountDownLatch waiter;
        private CyclicBarrier barrier;
        
        TestEventHandler() {
        }
        TestEventHandler(CountDownLatch waiter) {
            this.waiter = waiter;
        }
        TestEventHandler(CyclicBarrier barrier) {
            this.barrier = barrier;
        }
        
        @Override
        public void onEvent(LogEvent<ILoggingEvent> event, long sequence, boolean endOfBatch) throws Exception {
            if (waiter != null) {
                waiter.await();
            }
            if (barrier != null) {
                barrier.await();
            }
            this.logEventHolders.add(event);
            this.events.add(event.event);
        }
        
        public List<ILoggingEvent> getEvents() {
            return events;
        }
        public List<LogEvent<ILoggingEvent>> getLogEventHolders() {
            return logEventHolders;
        }
    }
    
    private static ch.qos.logback.core.util.Duration toLogback(Duration duration) {
        return ch.qos.logback.core.util.Duration.buildByMilliseconds(duration.toMillis());
    }
}
