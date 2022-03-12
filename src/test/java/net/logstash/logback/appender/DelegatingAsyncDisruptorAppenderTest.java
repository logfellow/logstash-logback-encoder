/*
 * Copyright 2013-2022 the original author or authors.
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;

import net.logstash.logback.appender.listener.AppenderListener;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.LogbackException;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.EchoEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DelegatingAsyncDisruptorAppenderTest {

    private static final int VERIFICATION_TIMEOUT = 1000 * 30;

    private DelegatingAsyncDisruptorAppender<ILoggingEvent, AppenderListener<ILoggingEvent>> appender = new DelegatingAsyncDisruptorAppender<ILoggingEvent, AppenderListener<ILoggingEvent>>() { };

    @Mock
    private ILoggingEvent event;

    @Spy
    private OutputStreamAppender<ILoggingEvent> outputStreamDelegate;

    @Mock
    private OutputStream outputStream;

    @BeforeEach
    public void setup() {
        outputStreamDelegate.setImmediateFlush(false);
        outputStreamDelegate.setEncoder(new EchoEncoder<ILoggingEvent>());
        outputStreamDelegate.setOutputStream(outputStream);

        appender.setContext(new LoggerContext());
    }

    @AfterEach
    public void tearDown() {
        appender.stop();
    }


    @Test
    public void appenderStartStop() {
        Appender<ILoggingEvent> delegate1 = spy(new TestAppender<ILoggingEvent>());
        Appender<ILoggingEvent> delegate2 = spy(new TestAppender<ILoggingEvent>());

        appender.addAppender(delegate1);
        appender.addAppender(delegate2);

        //
        // START
        //
        appender.start();
        assertThat(appender.isStarted()).isTrue();
        verify(delegate1).start();
        verify(delegate2).start();

        // Already started - start() not called a second time on child appenders
        appender.start();
        verify(delegate1, times(1)).start();
        verify(delegate2, times(1)).start();


        //
        // STOP
        //
        appender.stop();
        verify(delegate1).stop();
        verify(delegate2).stop();

        // Already stopped - stop() not called a second time on child appenders
        appender.stop();
        verify(delegate1, times(1)).stop();
        verify(delegate2, times(1)).stop();
    }


    /*
     * Event is forwarded to appenders in the order they were registered.
     */
    @Test
    public void handlerCalled() throws Exception {
        Appender<ILoggingEvent> delegate1 = spy(new TestAppender<ILoggingEvent>());
        Appender<ILoggingEvent> delegate2 = spy(new TestAppender<ILoggingEvent>());

        appender.addAppender(delegate1);
        appender.addAppender(delegate2);
        appender.start();

        appender.append(event);

        InOrder inOrder = inOrder(delegate1, delegate2);
        inOrder.verify(delegate1, timeout(VERIFICATION_TIMEOUT)).doAppend(event);
        inOrder.verify(delegate2, timeout(VERIFICATION_TIMEOUT)).doAppend(event);
    }


    /*
     * Event is forwarded to all appenders even after first threw an exception
     */
    @Test
    public void handlerCalledAfterException() throws Exception {
        Appender<ILoggingEvent> delegate1 = spy(new TestAppender<ILoggingEvent>());
        LogbackException exception = new LogbackException("boum");
        doThrow(exception).when(delegate1).doAppend(event);

        Appender<ILoggingEvent> delegate2 = spy(new TestAppender<ILoggingEvent>());

        appender.addAppender(delegate1);
        appender.addAppender(delegate2);
        appender.start();

        appender.append(event);

        // second appender received the event although first threw an exception
        //
        verify(delegate1, timeout(VERIFICATION_TIMEOUT).times(1)).doAppend(event);
        verify(delegate2, timeout(VERIFICATION_TIMEOUT).times(1)).doAppend(event);

        // an error status is logged
        assertThat(appender.getStatusManager().getCount()).isEqualTo(1);
        assertThat(appender.getStatusManager().getCopyOfStatusList().get(0).getOrigin()).isEqualTo(appender);
        assertThat(appender.getStatusManager().getCopyOfStatusList().get(0).getThrowable()).isEqualTo(exception);


        // Append one more event -> no more error statuses
        //
        appender.append(event);

        verify(delegate1, timeout(VERIFICATION_TIMEOUT).times(2)).doAppend(event);
        verify(delegate2, timeout(VERIFICATION_TIMEOUT).times(2)).doAppend(event);
        assertThat(appender.getStatusManager().getCount()).isEqualTo(1);


        // This event won't throw any exception -> reset "error state"
        //
        appender.append(mock(ILoggingEvent.class));


        // This event will again trigger an exception -> a new error status is logged
        //
        appender.append(event);

        verify(delegate1, timeout(VERIFICATION_TIMEOUT).times(3)).doAppend(event);
        verify(delegate2, timeout(VERIFICATION_TIMEOUT).times(3)).doAppend(event);
        assertThat(appender.getStatusManager().getCount()).isEqualTo(2);
    }


    /*
     * Flush OutputStreamAppender at end of batch
     */
    @Test
    public void flushOutputStreamAppender() throws Exception {
        outputStreamDelegate.setImmediateFlush(false);

        appender.addAppender(outputStreamDelegate);
        appender.start();

        appender.append(event);

        verify(outputStreamDelegate, timeout(VERIFICATION_TIMEOUT)).doAppend(event);
        verify(outputStream,         timeout(VERIFICATION_TIMEOUT)).flush();
    }


    /*
     * OutputStreamAppender may return a null OutputStream
     */
    @Test
    public void flushWithNullOutputStream() {
        appender.addAppender(outputStreamDelegate);
        appender.start();

        outputStreamDelegate.setOutputStream(null);
        appender.doAppend(event);

        assertThat(appender.getStatusManager().getCount()).isZero();
    }


    /*
     * Don't flush OutputStreamAppender at end of batch when they are configured
     * to "immediate flush"
     */
    @Test
    public void notFlushedWhenImmediateFlush() throws Exception {
        outputStreamDelegate.setImmediateFlush(true);
        outputStreamDelegate.setOutputStream(new ByteArrayOutputStream()); // use a real output stream for the appender to flush itself

        appender.addAppender(outputStreamDelegate);
        appender.start();

        appender.append(event);

        verify(outputStreamDelegate, timeout(VERIFICATION_TIMEOUT)).doAppend(event);
        verify(outputStream, never()).flush();
    }


    /*
     * Flush appenders implementing "Flushable" at end of batch
     */
    @Test
    public void flushFlushable() throws Exception {
        FlushableAppender flushableDelegate = spy(new FlushableAppender());
        
        appender.addAppender(flushableDelegate);
        appender.start();
        
        appender.append(event);
        
        verify(flushableDelegate, timeout(VERIFICATION_TIMEOUT)).doAppend(event);
        verify(flushableDelegate, timeout(VERIFICATION_TIMEOUT)).flush();
    }


    /*
     * Don't flush stopped appenders
     */
    @Test
    public void notFlushedWhenStopped() throws Exception {
        appender.addAppender(outputStreamDelegate);
        appender.start();

        outputStreamDelegate.stop();

        appender.append(event);

        verify(outputStreamDelegate, timeout(VERIFICATION_TIMEOUT)).doAppend(event);
        verify(outputStream, never()).flush();
    }


    /*
     * Assert flush is called on all delegates even after one throws an exception
     */
    @Test
    public void flushThrowsIOException() throws Exception {
        IOException exception = new IOException("boum");
        doThrow(exception).when(outputStream).flush();

        FlushableAppender flushableDelegate = spy(new FlushableAppender());


        appender.addAppender(outputStreamDelegate); // throws exception when flushed
        appender.addAppender(flushableDelegate);
        appender.start();

        appender.append(event);

        verify(outputStreamDelegate, timeout(VERIFICATION_TIMEOUT)).doAppend(event);

        verify(flushableDelegate, timeout(VERIFICATION_TIMEOUT)).doAppend(event);
        verify(flushableDelegate, timeout(VERIFICATION_TIMEOUT)).flush();
    }


    private static class TestAppender<E> extends AppenderBase<E> {
        private TestAppender() {
            super();
        }
        protected void append(E eventObject) {
        }
    }

    private static class FlushableAppender extends TestAppender<ILoggingEvent> implements Flushable {
        @Override
        public void flush() throws IOException {
        }
    }
}
