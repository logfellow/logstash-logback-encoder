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
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import net.logstash.logback.appender.AsyncDisruptorAppender.LogEvent;
import net.logstash.logback.appender.listener.AppenderListener;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.BasicStatusManager;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusManager;
import com.lmax.disruptor.EventHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
public class AsyncDisruptorAppenderTest {
    
    private static final int VERIFICATION_TIMEOUT = 1000 * 30;

    @InjectMocks
    private AsyncDisruptorAppender<ILoggingEvent, AppenderListener<ILoggingEvent>> appender = new AsyncDisruptorAppender<ILoggingEvent, AppenderListener<ILoggingEvent>>() { };
    
    @Mock
    private EventHandler<LogEvent<ILoggingEvent>> eventHandler;
    
    @Mock(lenient = true)
    private Context context;
    
    @Spy
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
        when(context.getStatusManager()).thenReturn(statusManager);

        appender.setAddDefaultStatusListener(false);
        appender.addListener(listener);
    }
    
    @AfterEach
    public void tearDown() {
        appender.stop();
        executorService.shutdownNow();
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testEventHandlerCalled() throws Exception {
        final AtomicReference<Object> capturedEvent = new AtomicReference<Object>();
        
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                capturedEvent.set(invocation.<LogEvent>getArgument(0).event);
                return null;
            }
        }).when(eventHandler).onEvent(any(LogEvent.class), anyLong(), eq(true));
        
        appender.start();
        
        verify(listener).appenderStarted(appender);
        
        appender.append(event1);
        
        verify(listener).eventAppended(eq(appender), eq(event1), anyLong());
        
        
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<LogEvent> captor = ArgumentCaptor.forClass(LogEvent.class);
        verify(eventHandler, timeout(VERIFICATION_TIMEOUT)).onEvent(captor.capture(), anyLong(), eq(true));

        // When eventHandler is invoked, the event should be event1
        Assertions.assertEquals(event1, capturedEvent.get());
        // The event should be set back to null after invocation
        Assertions.assertNull(captor.getValue().event);
        
        verify(event1).prepareForDeferredProcessing();
        
        appender.stop();

        verify(listener).appenderStopped(appender);
        
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

    @SuppressWarnings("unchecked")
    @Test
    public void testEventDroppedWhenFull() throws Exception {
        appender.setRingBufferSize(1);
        appender.start();
        
        final CountDownLatch eventHandlerWaiter = new CountDownLatch(1);
        final CountDownLatch mainWaiter = new CountDownLatch(1);
        
        /*
         * Cause the first event handling to block until we're done with the test.
         */
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                mainWaiter.countDown();
                eventHandlerWaiter.await();
                return null;
            }
        }).when(eventHandler).onEvent(any(LogEvent.class), anyLong(), anyBoolean());
        
        /*
         * This one will block during event handling
         */
        appender.append(event1);
        
        mainWaiter.await(VERIFICATION_TIMEOUT, TimeUnit.MILLISECONDS);
        /*
         * This one should be dropped
         */
        appender.append(event2);
        
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(statusManager, timeout(VERIFICATION_TIMEOUT)).add(statusCaptor.capture());
        
        Assertions.assertEquals(Status.WARN, statusCaptor.getValue().getLevel());
        Assertions.assertTrue(statusCaptor.getValue().getMessage().startsWith("Dropped"));
        
        eventHandlerWaiter.countDown();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEventHandlerThrowsException() throws Exception {
        appender.start();
        
        final Throwable throwable = new RuntimeException("message");
        
        doThrow(throwable).when(eventHandler).onEvent(any(LogEvent.class), anyLong(), anyBoolean());
        
        appender.append(event1);
        
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(statusManager, timeout(VERIFICATION_TIMEOUT)).add(statusCaptor.capture());
        
        Assertions.assertEquals(Status.ERROR, statusCaptor.getValue().getLevel());
        Assertions.assertTrue(statusCaptor.getValue().getMessage().startsWith("Unable to process event"));
        
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
             */
            Future<?> future = execute(() -> appender.append(event2));
            
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
            verify(listener, times(0)).eventAppendFailed(eq(appender), eq(event2), any());
            
        } finally {
            eventHandlerWaiter.countDown();
        }
    }
    
    
    @Test
    public void configRingBufferSize_negative() {
        appender.setRingBufferSize(-1);
        appender.start();
        
        assertThat(appender.isStarted()).isFalse();
        
        assertThat(statusManager.getCopyOfStatusList())
            .anyMatch(s -> s.getMessage().startsWith("<ringBufferSize> must be > 0") && s.getLevel() == Status.ERROR);
    }
    
    
    @Test
    public void configRingBufferSize_powerOfTwo() {
        appender.setRingBufferSize(3);
        appender.start();
        
        assertThat(appender.isStarted()).isFalse();
        
        assertThat(statusManager.getCopyOfStatusList())
            .anyMatch(s -> s.getMessage().startsWith("<ringBufferSize> must be a power of 2") && s.getLevel() == Status.ERROR);
    }
    
    
    @Test
    public void configAppendRetryFrequency() {
        appender.setAppendRetryFrequency(toLogback(Duration.ofMillis(-1)));
        appender.start();
        
        assertThat(appender.isStarted()).isFalse();
        
        assertThat(statusManager.getCopyOfStatusList())
            .anyMatch(s -> s.getMessage().startsWith("<appendRetryFrequency> must be > 0") && s.getLevel() == Status.ERROR);
    }
    
    
    
    // --------------------------------------------------------------------------------------------

   
    private Future<?> execute(Runnable runnable) {
        return executorService.submit(runnable);
    }
       
    private static class TestEventHandler implements EventHandler<LogEvent<ILoggingEvent>> {
        private final List<ILoggingEvent> events = new ArrayList<>();
        private final CountDownLatch waiter;
        
        TestEventHandler(CountDownLatch waiter) {
            this.waiter = waiter;
        }
        @Override
        public void onEvent(LogEvent<ILoggingEvent> event, long sequence, boolean endOfBatch) throws Exception {
            if (waiter != null) {
                waiter.await();
            }
            this.events.add(event.event);
        }
        
        public List<ILoggingEvent> getEvents() {
            return events;
        }
    }
    
    private static ch.qos.logback.core.util.Duration toLogback(Duration duration) {
        return ch.qos.logback.core.util.Duration.buildByMilliseconds(duration.toMillis());
    }
}
