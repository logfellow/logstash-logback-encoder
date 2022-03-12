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

import java.net.InetSocketAddress;
import java.net.Socket;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.DeferredProcessingAware;

/**
 * Listens to a TCP appender.
 *
 * Methods will be invoked in the thread that is sending the events over the TCP connection.
 * Therefore, ensure that the methods complete quickly, so that future events are not delayed.
 */
public interface TcpAppenderListener<Event extends DeferredProcessingAware> extends AppenderListener<Event> {

    /**
     * Called after given appender successfully sent the given event over the TCP connection.
     *
     * @param appender the appender that sent the event
     * @param socket the socket over which the appender sent the event
     * @param event the event that was sent
     * @param durationInNanos the time (in nanoseconds) it took to send the event
     */
    default void eventSent(Appender<Event> appender, Socket socket, Event event, long durationInNanos) {
    }

    /**
     * Called when the given appender fails to send the given event over a TCP connection.
     *
     * @param appender the appender that attempted to send the event
     * @param event the event that failed to send
     * @param reason what caused the failure
     */
    default void eventSendFailure(Appender<Event> appender, Event event, Throwable reason) {
    }

    /**
     * Called after the given appender successfully opens the given socket
     *
     * @param appender the appender that opened the socket
     * @param socket the socket that was opened
     */
    default void connectionOpened(Appender<Event> appender, Socket socket) {
    }

    /**
     * Called after the given appender fails to open a socket
     *
     * @param appender the appender that attempted to open a socket
     * @param address the address to which the appender attempted to connect
     * @param reason what caused the failure
     */
    default void connectionFailed(Appender<Event> appender, InetSocketAddress address, Throwable reason) {
    }

    /**
     * Called after the given appender closes the given socket
     * (either due to a reconnect, or shutdown)
     *
     * @param appender the appender that closed the socket
     * @param socket the socket that was closed
     */
    default void connectionClosed(Appender<Event> appender, Socket socket) {
    }

}
