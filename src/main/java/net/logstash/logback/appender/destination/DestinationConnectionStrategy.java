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
package net.logstash.logback.appender.destination;


/**
 * Strategy used to determine to which destination to connect, and when to reconnect.
 */
public interface DestinationConnectionStrategy {

    /**
     * Returns the index of the destination to which to connect next.
     *
     * @param previousDestinationIndex The previous destination index to which a connection was attempted (either success or failure)
     * @param numDestinations The total number of destinations available.
     * @return the index of the destination to which to connect next.
     */
    int selectNextDestinationIndex(int previousDestinationIndex, int numDestinations);

    /**
     * Returns whether the connection should be reestablished.
     *
     * @param currentTimeInMillis The time in millis for which to reevaluate whether the connection should be reestablished.
     * @param currentDestinationIndex The index of the destination which is currently connected
     * @param numDestinations The total number of destinations available.
     * @return true if the connection should be reestablished (to the destination returned by the next call to {@link #selectNextDestinationIndex(int, int)}, false otherwise.
     */
    boolean shouldReconnect(long currentTimeInMillis, int currentDestinationIndex, int numDestinations);

    /**
     * Called when a connection was successful to the given connectedDestinationIndex.
     *
     * @param connectionStartTimeInMillis The time in millis at which the connection was initiated (not completed).
     * @param connectedDestinationIndex The index of the destination which was successfully connected.
     * @param numDestinations The total number of destinations available.
     */
    void connectSuccess(long connectionStartTimeInMillis, int connectedDestinationIndex, int numDestinations);

    /**
     * Called when a connection fails to the given failedDestinationIndex.
     *
     * @param connectionStartTimeInMillis The time in millis at which the connection was initiated (not completed).
     * @param failedDestinationIndex The index of the destination which failed to connect.
     * @param numDestinations The total number of destinations available.
     */
    void connectFailed(long connectionStartTimeInMillis, int failedDestinationIndex, int numDestinations);

}
