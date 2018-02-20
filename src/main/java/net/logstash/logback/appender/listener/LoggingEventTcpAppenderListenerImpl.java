package net.logstash.logback.appender.listener;

import java.net.InetSocketAddress;
import java.net.Socket;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

/**
 * Implementation of {@link TcpAppenderListener} for {@link ILoggingEvent}s that does nothing.
 */
public class LoggingEventTcpAppenderListenerImpl extends LoggingEventAppenderListenerImpl implements TcpAppenderListener<ILoggingEvent> {

    @Override
    public void eventSent(Appender<ILoggingEvent> appender, Socket socket, ILoggingEvent event, long durationInNanos) {
    }

    @Override
    public void eventSendFailure(Appender<ILoggingEvent> appender, ILoggingEvent event, Throwable reason) {
    }

    @Override
    public void connectionOpened(Appender<ILoggingEvent> appender, Socket socket) {
    }

    @Override
    public void connectionFailed(Appender<ILoggingEvent> appender, InetSocketAddress address, Throwable reason) {
    }

    @Override
    public void connectionClosed(Appender<ILoggingEvent> appender, Socket socket) {
    }

}
