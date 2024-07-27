/*
 * Copyright 2013-2023 the original author or authors.
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

import java.net.InetSocketAddress;
import java.net.Socket;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.Appender;

/**
 * Implementation of {@link TcpAppenderListener} for {@link IAccessEvent}s that does nothing.
 * 
 * @deprecated Replaced by default methods in interface.
 */
@Deprecated
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
