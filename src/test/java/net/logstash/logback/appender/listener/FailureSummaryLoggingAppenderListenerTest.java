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
package net.logstash.logback.appender.listener;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import java.net.InetSocketAddress;
import java.net.Socket;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class FailureSummaryLoggingAppenderListenerTest {

    @Mock
    private Appender<ILoggingEvent> appender;

    @Mock
    private Socket socket;

    @Mock
    private InetSocketAddress address;

    @Mock
    private ILoggingEvent event;

    @BeforeEach
    public void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(FailureSummaryLoggingAppenderListener.class);
        logger.addAppender(appender);
    }

    @Test
    public void appendFailures() {
        FailureSummaryLoggingAppenderListener<ILoggingEvent> listener = new FailureSummaryLoggingAppenderListener<>();

        listener.eventAppended(appender, event, 10L);
        listener.eventAppended(appender, event, 10L);

        Throwable firstThrowableA = new Throwable("firstA");
        listener.eventAppendFailed(appender, event, firstThrowableA);
        Throwable mostRecentThrowableA = new Throwable("mostRecentA");
        listener.eventAppendFailed(appender, event, mostRecentThrowableA);

        listener.eventAppended(appender, event, 10L);
        listener.eventAppended(appender, event, 10L);

        Throwable firstThrowableB = new Throwable("firstB");
        listener.eventAppendFailed(appender, event, firstThrowableB);
        Throwable secondThrowableB = new Throwable("secondB");
        listener.eventAppendFailed(appender, event, secondThrowableB);
        Throwable mostRecentThrowableB = new Throwable("mostRecentB");
        listener.eventAppendFailed(appender, event, mostRecentThrowableB);

        listener.eventAppended(appender, event, 10L);
        listener.eventAppended(appender, event, 10L);

        verify(appender).doAppend(argThat(e -> {
            ILoggingEvent loggingEvent = (ILoggingEvent) e;
            return loggingEvent.getFormattedMessage().contains("2 append failures since ")
                    && loggingEvent.getThrowableProxy().getMessage().contains("mostRecentA")
                    && loggingEvent.getThrowableProxy().getSuppressed()[0].getMessage().contains("firstA");
        }));

        verify(appender).doAppend(argThat(e -> {
            ILoggingEvent loggingEvent = (ILoggingEvent) e;
            return loggingEvent.getFormattedMessage().contains("3 append failures since ")
                    && loggingEvent.getThrowableProxy().getMessage().contains("mostRecentB")
                    && loggingEvent.getThrowableProxy().getSuppressed()[0].getMessage().contains("firstB");
        }));
    }

    @Test
    public void sendFailures() {
        FailureSummaryLoggingAppenderListener<ILoggingEvent> listener = new FailureSummaryLoggingAppenderListener<>();

        listener.eventSent(appender, socket, event, 10L);
        listener.eventSent(appender, socket, event, 10L);

        Throwable firstThrowableA = new Throwable("firstA");
        listener.eventSendFailure(appender, event, firstThrowableA);
        Throwable mostRecentThrowableA = new Throwable("mostRecentA");
        listener.eventSendFailure(appender, event, mostRecentThrowableA);

        listener.eventSent(appender, socket, event, 10L);
        listener.eventSent(appender, socket, event, 10L);

        Throwable firstThrowableB = new Throwable("firstB");
        listener.eventSendFailure(appender, event, firstThrowableB);
        Throwable secondThrowableB = new Throwable("secondB");
        listener.eventSendFailure(appender, event, secondThrowableB);
        Throwable mostRecentThrowableB = new Throwable("mostRecentB");
        listener.eventSendFailure(appender, event, mostRecentThrowableB);

        listener.eventSent(appender, socket, event, 10L);
        listener.eventSent(appender, socket, event, 10L);

        verify(appender).doAppend(argThat(e -> {
            ILoggingEvent loggingEvent = (ILoggingEvent) e;
            return loggingEvent.getFormattedMessage().contains("2 send failures since ")
                    && loggingEvent.getThrowableProxy().getMessage().contains("mostRecentA")
                    && loggingEvent.getThrowableProxy().getSuppressed()[0].getMessage().contains("firstA");
        }));

        verify(appender).doAppend(argThat(e -> {
            ILoggingEvent loggingEvent = (ILoggingEvent) e;
            return loggingEvent.getFormattedMessage().contains("3 send failures since ")
                    && loggingEvent.getThrowableProxy().getMessage().contains("mostRecentB")
                    && loggingEvent.getThrowableProxy().getSuppressed()[0].getMessage().contains("firstB");
        }));
    }

    @Test
    public void connectFailures() {
        FailureSummaryLoggingAppenderListener<ILoggingEvent> listener = new FailureSummaryLoggingAppenderListener<>();

        listener.connectionOpened(appender, socket);
        listener.connectionOpened(appender, socket);

        Throwable firstThrowableA = new Throwable("firstA");
        listener.connectionFailed(appender, address, firstThrowableA);
        Throwable mostRecentThrowableA = new Throwable("mostRecentA");
        listener.connectionFailed(appender, address, mostRecentThrowableA);

        listener.connectionOpened(appender, socket);
        listener.connectionOpened(appender, socket);

        Throwable firstThrowableB = new Throwable("firstB");
        listener.connectionFailed(appender, address, firstThrowableB);
        Throwable secondThrowableB = new Throwable("secondB");
        listener.connectionFailed(appender, address, secondThrowableB);
        Throwable mostRecentThrowableB = new Throwable("mostRecentB");
        listener.connectionFailed(appender, address, mostRecentThrowableB);

        listener.connectionOpened(appender, socket);
        listener.connectionOpened(appender, socket);

        verify(appender).doAppend(argThat(e -> {
            ILoggingEvent loggingEvent = (ILoggingEvent) e;
            return loggingEvent.getFormattedMessage().contains("2 connect failures since ")
                    && loggingEvent.getThrowableProxy().getMessage().contains("mostRecentA")
                    && loggingEvent.getThrowableProxy().getSuppressed()[0].getMessage().contains("firstA");
        }));

        verify(appender).doAppend(argThat(e -> {
            ILoggingEvent loggingEvent = (ILoggingEvent) e;
            return loggingEvent.getFormattedMessage().contains("3 connect failures since ")
                    && loggingEvent.getThrowableProxy().getMessage().contains("mostRecentB")
                    && loggingEvent.getThrowableProxy().getSuppressed()[0].getMessage().contains("firstB");
        }));
    }

    @Test
    public void allFailures() {
        FailureSummaryLoggingAppenderListener<ILoggingEvent> listener = new FailureSummaryLoggingAppenderListener<>();

        listener.connectionOpened(appender, socket);
        listener.connectionOpened(appender, socket);
        listener.eventAppended(appender, event, 10L);
        listener.eventAppended(appender, event, 10L);
        listener.eventSent(appender, socket, event, 10L);
        listener.eventSent(appender, socket, event, 10L);

        Throwable firstThrowableA = new Throwable("firstA");
        Throwable mostRecentThrowableA = new Throwable("mostRecentA");
        listener.connectionFailed(appender, address, firstThrowableA);
        listener.connectionFailed(appender, address, mostRecentThrowableA);
        listener.eventAppendFailed(appender, event, firstThrowableA);
        listener.eventAppendFailed(appender, event, mostRecentThrowableA);
        listener.eventSendFailure(appender, event, firstThrowableA);
        listener.eventSendFailure(appender, event, mostRecentThrowableA);

        listener.connectionOpened(appender, socket);
        listener.eventAppended(appender, event, 10L);
        listener.eventSent(appender, socket, event, 10L);

        verify(appender).doAppend(argThat(e -> {
            ILoggingEvent loggingEvent = (ILoggingEvent) e;
            return loggingEvent.getFormattedMessage().contains("2 connect failures since ")
                    && loggingEvent.getThrowableProxy().getMessage().contains("mostRecentA")
                    && loggingEvent.getThrowableProxy().getSuppressed()[0].getMessage().contains("firstA");
        }));
        verify(appender).doAppend(argThat(e -> {
            ILoggingEvent loggingEvent = (ILoggingEvent) e;
            return loggingEvent.getFormattedMessage().contains("2 append failures since ")
                    && loggingEvent.getThrowableProxy().getMessage().contains("mostRecentA")
                    && loggingEvent.getThrowableProxy().getSuppressed()[0].getMessage().contains("firstA");
        }));
        verify(appender).doAppend(argThat(e -> {
            ILoggingEvent loggingEvent = (ILoggingEvent) e;
            return loggingEvent.getFormattedMessage().contains("2 send failures since ")
                    && loggingEvent.getThrowableProxy().getMessage().contains("mostRecentA")
                    && loggingEvent.getThrowableProxy().getSuppressed()[0].getMessage().contains("firstA");
        }));
    }
}
