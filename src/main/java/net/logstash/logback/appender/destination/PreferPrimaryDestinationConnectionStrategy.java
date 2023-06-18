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

import ch.qos.logback.core.util.Duration;

/**
 * The first destination is considered the "primary" destination.
 * The remaining destinations are considered "secondary" destinations.
 * <p>
 * Prefer to connect to the primary destination.
 * If the primary destination is down, try other destinations.
 * 
 * <p>
 * Connections are attempted to each destination in order until a connection succeeds.
 * Logs will be sent to the first destination for which the connection succeeds
 * until the connection breaks or (if the connection is to a secondary destination)
 * the {@link #secondaryConnectionTTL} elapses.
 */
public class PreferPrimaryDestinationConnectionStrategy implements DestinationConnectionStrategy {
    
    /**
     * The "primary" destination index;
     */
    private static final int PRIMARY_DESTINATION_INDEX = 0;

    /**
     * Time period for connections to secondary destinations to be used
     * before attempting to reconnect to primary destination.
     * 
     * When the value is null (the default), the feature is disabled:
     * the appender will stay on the current destination until an error occurs.
     */
    private Duration secondaryConnectionTTL;

    /**
     * The minimum amount of time that a connection must remain open
     * before the primary is retried on the next reopen attempt.
     *
     * <p>This is used to prevent a connection storm against the primary if the primary
     * accepts connections and then immediately closes them.</p>
     *
     * <p>When null, the primary will always be retried first,
     * regardless of how long the previous connection remained open.</p>
     */
    private Duration minConnectionTimeBeforePrimary = Duration.buildBySeconds(10);

    /**
     * The destinationIndex to be returned on the next call to {@link #selectNextDestinationIndex(int, int)}.
     */
    private volatile int nextDestinationIndex = PRIMARY_DESTINATION_INDEX;

    /**
     * Time at which the current connection should be automatically closed
     * to force an attempt to reconnect to the primary server
     */
    private volatile long secondaryConnectionExpirationTime = Long.MAX_VALUE;

    private volatile long lastSuccessfulConnectTime;
    
    @Override
    public int selectNextDestinationIndex(int previousDestinationIndex, int numDestinations) {
        int candidateIndex = this.nextDestinationIndex; // volatile read
        if (candidateIndex == PRIMARY_DESTINATION_INDEX
                && minConnectionTimeBeforePrimary != null
                && (System.currentTimeMillis() - lastSuccessfulConnectTime) < minConnectionTimeBeforePrimary.getMilliseconds()) {
            /*
             * If the connection didn't remain open for at least the minConnectionTimeBeforePrimary,
             * then keep trying destinations in round-robin.
             *
             * This prevents a connection storm against the primary if the primary
             * accepts connections and then immediately closes them.
             */

            return (previousDestinationIndex + 1) % numDestinations;
        }
        return nextDestinationIndex;
    }

    @Override
    public void connectSuccess(long connectionStartTimeInMillis, int connectedDestinationIndex, int numDestinations) {
        /*
         * If connected to a secondary, remember when the connection should be closed to
         * force attempt to reconnect to primary
         */
        if (secondaryConnectionTTL != null && connectedDestinationIndex != PRIMARY_DESTINATION_INDEX) {
            secondaryConnectionExpirationTime = connectionStartTimeInMillis + secondaryConnectionTTL.getMilliseconds();
        } else {
            secondaryConnectionExpirationTime = Long.MAX_VALUE;
        }

        lastSuccessfulConnectTime = connectionStartTimeInMillis;
        nextDestinationIndex = PRIMARY_DESTINATION_INDEX;
    }

    @Override
    public void connectFailed(long connectionStartTimeInMillis, int failedDestinationIndex, int numDestinations) {
        nextDestinationIndex = (failedDestinationIndex + 1) % numDestinations;
    }

    @Override
    public boolean shouldReconnect(long currentTimeInMillis, int currentDestinationIndex, int numDestinations) {
        return secondaryConnectionExpirationTime <= currentTimeInMillis;
    }
    
    public Duration getSecondaryConnectionTTL() {
        return secondaryConnectionTTL;
    }
    
    /**
     * Time period for connections to secondary destinations to be used
     * before attempting to reconnect to primary destination.
     * 
     * When the value is null (the default), the feature is disabled:
     * the appender will stay on the current destination until an error occurs.
     * 
     * @param secondaryConnectionTTL time to stay connected to a secondary connection
     *                               before attempting to reconnect to the primary
     */
    public void setSecondaryConnectionTTL(Duration secondaryConnectionTTL) {
        if (secondaryConnectionTTL != null && secondaryConnectionTTL.getMilliseconds() <= 0) {
            throw new IllegalArgumentException("secondaryConnectionTTL must be > 0");
        }
        this.secondaryConnectionTTL = secondaryConnectionTTL;
    }

    public Duration getMinConnectionTimeBeforePrimary() {
        return minConnectionTimeBeforePrimary;
    }

    /**
     * The minimum amount of time that a connection must remain open
     * before the primary is retried on the next reopen attempt.
     *
     * <p>This is used to prevent a connection storm against the primary if the primary
     * accepts connections and then immediately closes them.</p>
     *
     * <p>When null, the primary will always be retried first,
     * regardless of how long the previous connection remained open.</p>
     *
     * @param minConnectionTimeBeforePrimary The minimum amount of time that a connection must remain open
     *                                       before the primary is retried on the next reopen attempt.
     */
    public void setMinConnectionTimeBeforePrimary(Duration minConnectionTimeBeforePrimary) {
        this.minConnectionTimeBeforePrimary = minConnectionTimeBeforePrimary;
    }
}
