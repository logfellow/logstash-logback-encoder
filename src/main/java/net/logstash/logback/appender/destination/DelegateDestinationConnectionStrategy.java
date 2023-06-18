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
package net.logstash.logback.appender.destination;

import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;

/**
 * A convenience class to make setting a {@link DestinationConnectionStrategy} cleaner in logback's xml configuration
 * when using the strategies provided by logstash-logback-encoder.
 *
 * <p>
 * For example, instead of:
 * {@code
 *     <appender name="tcp" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
 *         <connectionStrategy class="net.logstash.logback.appender.destination.RoundRobinDestinationConnectionStrategy">
 *             <connectionTTL>10 minutes</connectionTTL>
 *         </connectionStrategy>
 * }
 * you can use:
 * {@code
 *     <appender name="tcp" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
 *         <connectionStrategy>
 *             <roundRobin>
 *                 <connectionTTL>10 minutes</connectionTTL>
 *             </roundRobin>
 *         </connectionStrategy>
 * }
 *
 *
 */
public class DelegateDestinationConnectionStrategy extends ContextAwareBase implements DestinationConnectionStrategy, LifeCycle {

    private DestinationConnectionStrategy delegate;

    private volatile boolean started;

    @Override
    public int selectNextDestinationIndex(int previousDestinationIndex, int numDestinations) {
        return delegate.selectNextDestinationIndex(previousDestinationIndex, numDestinations);
    }

    @Override
    public void connectSuccess(long connectionStartTimeInMillis, int connectedDestinationIndex, int numDestinations) {
        delegate.connectSuccess(connectionStartTimeInMillis, connectedDestinationIndex, numDestinations);
    }

    @Override
    public void connectFailed(long connectionStartTimeInMillis, int failedDestinationIndex, int numDestinations) {
        delegate.connectFailed(connectionStartTimeInMillis, failedDestinationIndex, numDestinations);
    }

    @Override
    public boolean shouldReconnect(long currentTimeInMillis, int currentDestinationIndex, int numDestinations) {
        return delegate.shouldReconnect(currentTimeInMillis, currentDestinationIndex, numDestinations);
    }

    @Override
    public void start() {
        this.started = true;
        if (this.delegate == null) {
            throw new IllegalStateException("No destinationConnectionStrategy configured.");
        }
    }

    @Override
    public void stop() {
        this.started = false;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    public void setPreferPrimary(PreferPrimaryDestinationConnectionStrategy strategy) {
        setDelegate(strategy);
    }

    public void setRandom(RandomDestinationConnectionStrategy strategy) {
        setDelegate(strategy);
    }

    public void setRoundRobin(RoundRobinDestinationConnectionStrategy strategy) {
        setDelegate(strategy);
    }

    private void setDelegate(DestinationConnectionStrategy delegate) {
        if (this.delegate != null) {
            throw new IllegalStateException(String.format("Attempted to set two destination connection strategies: %s %s", this.delegate, delegate));
        }
        this.delegate = delegate;
    }

}
