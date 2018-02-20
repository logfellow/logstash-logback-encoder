package net.logstash.logback.appender.listener;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.Appender;

/**
 * Implementation of {@link AppenderListener} for {@link IAccessEvent}s that does nothing.
 */
public class AccessEventAppenderListenerImpl implements AppenderListener<IAccessEvent> {

    @Override
    public void appenderStarted(Appender<IAccessEvent> appender) {
    }

    @Override
    public void appenderStopped(Appender<IAccessEvent> appender) {
    }

    @Override
    public void eventAppended(Appender<IAccessEvent> appender, IAccessEvent event, long durationInNanos) {
    }

    @Override
    public void eventAppendFailed(Appender<IAccessEvent> appender, IAccessEvent event, Throwable reason) {
    }
}
