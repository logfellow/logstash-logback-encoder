/*
 * Copyright 2013-2025 the original author or authors.
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

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.logstash.logback.appender.listener.AppenderListener;

import ch.qos.logback.core.Layout;
import ch.qos.logback.core.net.SyslogAppenderBase;
import ch.qos.logback.core.net.SyslogOutputStream;
import ch.qos.logback.core.spi.DeferredProcessingAware;

/**
 * An appender that sends events over UDP using a custom {@link Layout}.
 */
public class AbstractLogstashUdpSocketAppender<Event extends DeferredProcessingAware> extends SyslogAppenderBase<Event> {

    private Layout<Event> layout;

    /**
     * These listeners will be notified when certain events occur on this appender.
     */
    private final List<AppenderListener<Event>> listeners = new ArrayList<>();

    private SyslogOutputStream syslogOutputStream;

    public AbstractLogstashUdpSocketAppender() {
        setFacility("NEWS"); // NOTE: this value is never used
    }
    
    @Override
    public void start() {
        if (layout == null) {
            addError("No layout was configured. Use <layout> to specify the fully qualified class name of the layout to use");
            return;
        }

        super.start();
        if (isStarted()) {
            getLayout().start();
            fireAppenderStarted();
        }
    }
    
    @Override
    public void stop() {
        super.stop();
        getLayout().stop();
        fireAppenderStopped();
    }

    @Override
    public Layout<Event> buildLayout() {
        layout.setContext(getContext());
        return layout;
    }

    @Override
    protected void append(Event eventObject) {
        if (!isStarted()) {
            return;
        }
        long startTime = System.nanoTime();
        try {
            /*
             * This is a cut-and-paste of SyslogAppenderBase.append(E)
             * so that we can fire the appropriate event.
             */
            String msg = getLayout().doLayout(eventObject);
            if (msg == null) {
                return;
            }
            if (msg.length() > getMaxMessageSize()) {
                msg = msg.substring(0, getMaxMessageSize());
            }
            syslogOutputStream.write(msg.getBytes(getCharset()));
            syslogOutputStream.flush();
            postProcess(eventObject, syslogOutputStream);
            
            fireEventAppended(eventObject, System.nanoTime() - startTime);
        } catch (IOException ioe) {
            addError("Failed to send diagram to " + getSyslogHost(), ioe);

            fireEventAppendFailed(eventObject, ioe);
        }
    }
    
    protected void fireAppenderStarted() {
        for (AppenderListener<Event> listener : listeners) {
            listener.appenderStarted(this);
        }
    }
    
    protected void fireAppenderStopped() {
        for (AppenderListener<Event> listener : listeners) {
            listener.appenderStopped(this);
        }
    }
    
    protected void fireEventAppended(Event event, long durationInNanos) {
        for (AppenderListener<Event> listener : listeners) {
            listener.eventAppended(this, event, durationInNanos);
        }
    }
    
    protected void fireEventAppendFailed(Event event, Throwable reason) {
        for (AppenderListener<Event> listener : listeners) {
            listener.eventAppendFailed(this, event, reason);
        }
    }
    
    @Override
    public int getSeverityForEvent(Object eventObject) {
        return 0; // NOTE: this value is never used
    }
    
    public String getHost() {
        return getSyslogHost();
    }
    
    /**
     * Just an alias for syslogHost (since that name doesn't make much sense here)
     * 
     * @param host the name of the the host where log output should go
     */
    public void setHost(String host) {
        setSyslogHost(host);
    }

    @Override
    public Layout<Event> getLayout() {
        return layout;
    }

    @Override
    public void setLayout(Layout<Event> layout) {
        this.layout = Objects.requireNonNull(layout, "layout must not be null");
    }

    public void addListener(AppenderListener<Event> listener) {
        this.listeners.add(listener);
    }
    public void removeListener(AppenderListener<Event> listener) {
        this.listeners.remove(listener);
    }

    @Override
    public SyslogOutputStream createOutputStream() throws UnknownHostException, SocketException {
        this.syslogOutputStream = new SyslogOutputStream(this.getHost(), this.getPort());
        return this.syslogOutputStream;
    }
}
