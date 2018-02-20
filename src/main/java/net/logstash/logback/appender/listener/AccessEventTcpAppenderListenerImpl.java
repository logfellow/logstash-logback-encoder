package net.logstash.logback.appender.listener;

import java.net.InetSocketAddress;
import java.net.Socket;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.Appender;

/**
 * Implementation of {@link TcpAppenderListener} for {@link IAccessEvent}s that does nothing.
 */
public class AccessEventTcpAppenderListenerImpl extends AccessEventAppenderListenerImpl implements TcpAppenderListener<IAccessEvent> {

    @Override
    public void eventSent(Appender<IAccessEvent> appender, Socket socket, IAccessEvent event, long durationInNanos) {
    }

    @Override
    public void eventSendFailure(Appender<IAccessEvent> appender, IAccessEvent event, Throwable reason) {
    }

    @Override
    public void connectionOpened(Appender<IAccessEvent> appender, Socket socket) {
    }

    @Override
    public void connectionFailed(Appender<IAccessEvent> appender, InetSocketAddress address, Throwable reason) {
    }

    @Override
    public void connectionClosed(Appender<IAccessEvent> appender, Socket socket) {
    }

}
