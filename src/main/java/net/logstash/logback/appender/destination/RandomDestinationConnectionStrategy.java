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

import java.util.Random;

/**
 * This strategy attempts connections to the destination in a random order.
 * If a connection fails, the next random destination is attempted.
 *
 * The connectionTTL can be set to gracefully close connections after a specific duration.
 * This will force the the appender to reattempt to connect to the next random destination.
 */
public class RandomDestinationConnectionStrategy extends DestinationConnectionStrategyWithTtl {

    private final ThreadLocal<Random> threadLocalRandom = new ThreadLocal<Random>() {
        protected Random initialValue() {
            return new Random();
        }
    };

    @Override
    public int selectNextDestinationIndex(int previousDestinationIndex, int numDestinations) {
        return getRandom().nextInt(numDestinations);
    }

    public Random getRandom() {
        return threadLocalRandom.get();
    }
}
