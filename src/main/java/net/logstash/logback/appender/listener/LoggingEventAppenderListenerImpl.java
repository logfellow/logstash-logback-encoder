package net.logstash.logback.appender.listener;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

/**
 * Implementation of {@link AppenderListener} for {@link ILoggingEvent}s that does nothing.
 */
public class LoggingEventAppenderListenerImpl implements AppenderListener<ILoggingEvent> {

    @Override
    public void appenderStarted(Appender<ILoggingEvent> appender) {
    }

    @Override
    public void appenderStopped(Appender<ILoggingEvent> appender) {
    }

    @Override
    public void eventAppended(Appender<ILoggingEvent> appender, ILoggingEvent event, long durationInNanos) {
    }

    @Override
    public void eventAppendFailed(Appender<ILoggingEvent> appender, ILoggingEvent event, Throwable reason) {
    }
}
