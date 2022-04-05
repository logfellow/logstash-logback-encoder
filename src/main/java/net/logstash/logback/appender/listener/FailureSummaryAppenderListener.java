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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.DeferredProcessingAware;

/**
 * A {@link TcpAppenderListener} that invokes different callbacks for
 * the first successful append/send/connect after a series of failures.
 * The callback includes a summary of the failures that occurred.
 *
 * <p>Subclasses implement {@link #handleFailureSummary(FailureSummary, CallbackType)}
 * to define behavior when the first successful append/send/connect
 * occurs after a series of consecutive failures.</p>
 */
public abstract class FailureSummaryAppenderListener<Event extends DeferredProcessingAware> implements TcpAppenderListener<Event> {

    /**
     * The current state of appending events to the appender.
     */
    private final AtomicReference<State> appendState = new AtomicReference<>(SUCCEEDING_STATE);

    /**
     * The current state of sending events to the TCP destination.
     */
    private final AtomicReference<State> sendState = new AtomicReference<>(SUCCEEDING_STATE);

    /**
     * The current state of opening TCP connections.
     */
    private final AtomicReference<State> connectState = new AtomicReference<>(SUCCEEDING_STATE);

    /**
     * The type of listener callback.
     */
    public enum CallbackType {
        /**
         * Callback for appending events to the appender.
         */
        APPEND,
        /**
         * Callback for sending events from a TCP appender.
         */
        SEND,
        /**
         * Callback for creating TCP connections for a TCP appender.
         */
        CONNECT
    }

    /**
     * Summary details of consecutive failures
     */
    public interface FailureSummary {
        /**
         * Millisecond value of the first failue.
         * @return Millisecond value of the first failue.
         */
        long getFirstFailureTime();

        /**
         * The first failure that occurred.
         * @return The first failure that occurred.
         */
        Throwable getFirstFailure();

        /**
         * The most recent failure that occurred.
         * @return The most recent failure that occurred.
         */
        Throwable getMostRecentFailure();

        /**
         * The number of consecutive failures before a success.
         * @return number of consecutive failures before a success.
         */
        long getConsecutiveFailures();
    }

    private interface State {
        boolean isSucceeding();
    }

    /**
     * Constant representing a state where events are succeeding.
     */
    private static final State SUCCEEDING_STATE = () -> true;

    /**
     * A state where events are failing.
     */
    private static class FailingState implements State, FailureSummary {

        private final Throwable firstThrowable;
        private final long firstFailureTime;
        private final AtomicLong consecutiveFailures = new AtomicLong();
        private volatile Throwable mostRecentThrowable;

        private FailingState(Throwable firstThrowable) {
            this.firstThrowable = firstThrowable;
            this.firstFailureTime = System.currentTimeMillis();
            recordThrowable(firstThrowable);
        }

        @Override
        public boolean isSucceeding() {
            return false;
        }

        @Override
        public Throwable getFirstFailure() {
            return firstThrowable;
        }

        @Override
        public long getFirstFailureTime() {
            return firstFailureTime;
        }

        @Override
        public Throwable getMostRecentFailure() {
            return mostRecentThrowable;
        }

        @Override
        public long getConsecutiveFailures() {
            return consecutiveFailures.get();
        }

        private void recordThrowable(Throwable throwable) {
            consecutiveFailures.incrementAndGet();
            mostRecentThrowable = throwable;
        }
    }

    @Override
    public void eventAppended(Appender<Event> appender, Event event, long durationInNanos) {
        recordSuccess(this.appendState, CallbackType.APPEND);
    }

    @Override
    public void eventAppendFailed(Appender<Event> appender, Event event, Throwable reason) {
        recordFailure(this.appendState, reason);
    }

    @Override
    public void eventSent(Appender<Event> appender, Socket socket, Event event, long durationInNanos) {
        recordSuccess(this.sendState, CallbackType.SEND);
    }

    @Override
    public void eventSendFailure(Appender<Event> appender, Event event, Throwable reason) {
        recordFailure(this.sendState, reason);
    }

    @Override
    public void connectionOpened(Appender<Event> appender, Socket socket) {
        recordSuccess(this.connectState, CallbackType.CONNECT);
    }

    @Override
    public void connectionFailed(Appender<Event> appender, InetSocketAddress address, Throwable reason) {
        recordFailure(this.connectState, reason);
    }

    private void recordSuccess(AtomicReference<State> stateRef, CallbackType callbackType) {
        State currentState = stateRef.get();
        if (!currentState.isSucceeding() && stateRef.compareAndSet(currentState, SUCCEEDING_STATE)) {
            FailingState oldFailingState = (FailingState) currentState;
            handleFailureSummary(oldFailingState, callbackType);
        }
    }
    private void recordFailure(AtomicReference<State> stateRef, Throwable reason) {
        State currentState = stateRef.get();
        if (currentState.isSucceeding()) {
            if (!stateRef.compareAndSet(currentState, new FailingState(reason))) {
                recordFailure(stateRef, reason);
            }
        } else {
            ((FailingState) currentState).recordThrowable(reason);
        }
    }

    /**
     * Called after the first success after the a series of consecutive failures.
     *
     * @param failureSummary contains summary details of all the consecutive failures
     * @param callbackType the type of callback (append/send/connect)
     */
    protected abstract void handleFailureSummary(FailureSummary failureSummary, CallbackType callbackType);

}
