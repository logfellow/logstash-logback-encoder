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
package net.logstash.logback.appender.destination;

import ch.qos.logback.core.util.Duration;

/**
 * A {@link DestinationConnectionStrategy} that will force a connection to be reestablished after a length of time has elapsed.
 */
public abstract class DestinationConnectionStrategyWithTtl implements DestinationConnectionStrategy {

    /**
     * The desired max length of time for a connection to remain connected.
     * After this length of time, the connection will be reestablished.
     */
    private Duration connectionTTL;

    /**
     * Time at which the current connection should be automatically closed
     * to force an attempt to reconnect to the next destination.
     */
    private volatile long connectionExpirationTime = Long.MAX_VALUE;

    @Override
    public void connectSuccess(long connectionStartTimeInMillis, int connectedDestinationIndex, int numDestinations) {
        if (connectionTTL != null) {
            connectionExpirationTime = connectionStartTimeInMillis + connectionTTL.getMilliseconds();
        } else {
            connectionExpirationTime = Long.MAX_VALUE;
        }
    }

    @Override
    public void connectFailed(long connectionStartTimeInMillis, int failedDestinationIndex, int numDestinations) {
    }

    @Override
    public boolean shouldReconnect(long currentTimeInMillis, int currentDestinationIndex, int numDestinations) {
        return connectionExpirationTime <= currentTimeInMillis;
    }

    public Duration getConnectionTTL() {
        return connectionTTL;
    }

    public void setConnectionTTL(Duration connectionTTL) {
        if (connectionTTL != null && connectionTTL.getMilliseconds() <= 0) {
            throw new IllegalArgumentException("connectionTTL must be > 0");
        }
        this.connectionTTL = connectionTTL;
    }


}
