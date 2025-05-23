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
package net.logstash.logback.appender.destination;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.UnaryOperator;

/**
 * This strategy attempts connections to the destination in a random order.
 * If a connection fails, the next random destination is attempted.
 *
 * <p>The connectionTTL can be set to gracefully close connections after a specific duration.
 * This will force the the appender to reattempt to connect to the next random destination.
 */
public class RandomDestinationConnectionStrategy extends DestinationConnectionStrategyWithTtl {

    /**
     * Random number generator. Function argument is the maximum value allowed for the generated
     * random int number.
     */
    private final UnaryOperator<Integer> randomSupplier;
    
    
    public RandomDestinationConnectionStrategy() {
        this(bound -> ThreadLocalRandom.current().nextInt(bound));
    }
    
    public RandomDestinationConnectionStrategy(UnaryOperator<Integer> randomSupplier) {
        this.randomSupplier = Objects.requireNonNull(randomSupplier);
    }
    
    @Override
    public int selectNextDestinationIndex(int previousDestinationIndex, int numDestinations) {
        return randomSupplier.apply(numDestinations);
    }
}
