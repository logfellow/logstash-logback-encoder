/*
 * Copyright 2013-2021 the original author or authors.
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
package net.logstash.logback.composite.loggingevent;

import java.io.IOException;

import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.JsonWritingUtils;
import ch.qos.logback.classic.spi.ILoggingEvent;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;
import com.fasterxml.uuid.impl.TimeBasedGenerator;

/**
 * Outputs random UUID as field value.
 * Handy when you want to provide unique identifier for log lines.
 */
public class UuidProvider extends AbstractFieldJsonProvider<ILoggingEvent> {

    public static final String FIELD_UUID = "uuid";

    /**
     * Type 4 UUID.
     */
    public static final String STRATEGY_RANDOM = "random";

    /**
     * Type 1 time based UUID.
     *
     * When the time strategy is used, then
     * {@link #ethernet} can be set to either 'interface' (to automatically pick a MAC address from a network interface)
     * or a MAC address string.
     */
    public static final String STRATEGY_TIME = "time";

    private NoArgGenerator uuids = Generators.randomBasedGenerator();

    /**
     * One of {@value #STRATEGY_RANDOM} or {@value #STRATEGY_TIME}.
     */
    private String strategy = STRATEGY_RANDOM;

    /**
     * For {@link UuidStrategy#time} strategy only,
     * 'interface' or ethernet MAC address.
     */
    private String ethernet;

    public UuidProvider() {
        setFieldName(FIELD_UUID);
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent iLoggingEvent) throws IOException {
        JsonWritingUtils.writeStringField(generator, getFieldName(), uuids.generate().toString());
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;

        uuids = newUuidStrategy(strategy, ethernet);
    }

    public String getEthernet() {
        return ethernet;
    }

    public void setEthernet(String ethernet) {
        this.ethernet = ethernet;

        uuids = newUuidStrategy(this.strategy, this.ethernet);
    }

    private NoArgGenerator newUuidStrategy(String strategy, String ethernet) {

        if (STRATEGY_TIME.equalsIgnoreCase(strategy)) {
            return newTimeBasedGenerator(ethernet);
        }
        if (STRATEGY_RANDOM.equalsIgnoreCase(strategy)) {
            return Generators.randomBasedGenerator();
        }
        throw new IllegalArgumentException("Unknown strategy: " + strategy);
    }

    private TimeBasedGenerator newTimeBasedGenerator(String ethernet) {
        if (ethernet == null) {
            return Generators.timeBasedGenerator();
        }

        if ("interface".equalsIgnoreCase(ethernet)) {
            return Generators.timeBasedGenerator(EthernetAddress.fromInterface());
        }

        return Generators.timeBasedGenerator(EthernetAddress.valueOf(ethernet));
    }
}
