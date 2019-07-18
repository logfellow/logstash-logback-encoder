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

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

import ch.qos.logback.core.OutputStreamAppender;
import net.logstash.logback.appender.listener.AppenderListener;
import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AsyncAppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import ch.qos.logback.core.spi.DeferredProcessingAware;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;

/**
 * An {@link AsyncDisruptorAppender} that delegates appending of an event
 * to delegate {@link #appenders}.
 * 
 * This is very similar to logback's {@link AsyncAppenderBase}, except that:
 * <ul>
 * <li>it uses a {@link RingBuffer} instead of a {@link BlockingQueue}</li>
 * <li>it allows any number of delegate appenders, instead of just one</li>
 * </ul>
 *
 * @param <Event> type of event ({@link ILoggingEvent} or {@link IAccessEvent}).
 */
public abstract class DelegatingAsyncDisruptorAppender<Event extends DeferredProcessingAware, Listener extends AppenderListener<Event>> extends AsyncDisruptorAppender<Event, Listener> implements AppenderAttachable<Event> {
    
    /**
     * The delegate appenders.
     */
    private final AppenderAttachableImpl<Event> appenders = new AppenderAttachableImpl<Event>();
    
    private class DelegatingEventHandler implements EventHandler<LogEvent<Event>> {
        
        @Override
        public void onEvent(LogEvent<Event> logEvent, long sequence, boolean endOfBatch) throws Exception {
            appenders.appendLoopOnAppenders(logEvent.event);

            /*
             * Optimization:
             *
             * If any of the delegate appenders are instances of OutputStreamAppender,
             * then flush the OutputStreams at the end of the batch.
             */
            if (endOfBatch) {
                for (Iterator<Appender<Event>> iter = appenders.iteratorForAppenders(); iter.hasNext(); ) {
                    Appender<Event> appender = iter.next();
                    if (appender instanceof OutputStreamAppender) {
                        OutputStreamAppender outputStreamAppender = (OutputStreamAppender) appender;
                        if (!outputStreamAppender.isImmediateFlush()) {
                            outputStreamAppender.getOutputStream().flush();
                        }
                    }
                }
            }
        }
    }
    
    public DelegatingAsyncDisruptorAppender() {
        super();
        setEventHandler(new DelegatingEventHandler());
    }
    
    @Override
    public void start() {
        startDelegateAppenders();
        super.start();
    }
    
    @Override
    public void stop() {
        if (!isStarted()) {
            return;
        }
        super.stop();
        stopDelegateAppenders();
    }

    private void startDelegateAppenders() {
        for (Iterator<Appender<Event>> appenderIter = appenders.iteratorForAppenders(); appenderIter.hasNext();) {
            Appender<Event> appender = appenderIter.next();
            if (appender.getContext() == null) {
                appender.setContext(getContext());
            }
            if (!appender.isStarted()) {
                appender.start();
            }
        }
    }

    private void stopDelegateAppenders() {
        for (Iterator<Appender<Event>> appenderIter = appenders.iteratorForAppenders(); appenderIter.hasNext();) {
            Appender<Event> appender = appenderIter.next();
            if (appender.isStarted()) {
                appender.stop();
            }
        }
    }

    @Override
    public void addAppender(Appender<Event> newAppender) {
        appenders.addAppender(newAppender);
    }

    @Override
    public Iterator<Appender<Event>> iteratorForAppenders() {
        return appenders.iteratorForAppenders();
    }

    @Override
    public Appender<Event> getAppender(String name) {
        return appenders.getAppender(name);
    }

    @Override
    public boolean isAttached(Appender<Event> appender) {
        return appenders.isAttached(appender);
    }

    @Override
    public void detachAndStopAllAppenders() {
        appenders.detachAndStopAllAppenders();
    }

    @Override
    public boolean detachAppender(Appender<Event> appender) {
        return appenders.detachAppender(appender);
    }

    @Override
    public boolean detachAppender(String name) {
        return appenders.detachAppender(name);
    }

}
