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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import net.logstash.logback.appender.AsyncDisruptorAppender.LogEvent;
import net.logstash.logback.appender.listener.AppenderListener;

import ch.qos.logback.classic.spi.ILoggingEvent;
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
    
    @Mock
    private StatusManager statusManager;
    
    @Mock
    private ILoggingEvent event1;
    
    @Mock
    private ILoggingEvent event2;
    
    @Mock
    private AppenderListener<ILoggingEvent> listener;
    
    @BeforeEach
    public void setup() {
        when(context.getStatusManager()).thenReturn(statusManager);

        appender.setAddDefaultStatusListener(false);
        appender.addListener(listener);
    }
    
    @AfterEach
    public void tearDown() {
        appender.stop();
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

}
