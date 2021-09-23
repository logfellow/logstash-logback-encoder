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

import net.logstash.logback.composite.UuidJsonProvider;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Outputs random UUID as field value.
 * Handy when you want to provide unique identifier for log lines.
 * 
 * @Deprecated use {@link UuidJsonProvider} instead.
 */
@Deprecated
public class UuidProvider extends UuidJsonProvider<ILoggingEvent> {

    public static final String FIELD_UUID = UuidJsonProvider.FIELD_UUID;

    /**
     * Type 4 UUID.
     */
    public static final String STRATEGY_RANDOM = UuidJsonProvider.STRATEGY_RANDOM;

    /**
     * Type 1 time based UUID.
     *
     * When the time strategy is used, then
     * {@link #ethernet} can be set to either 'interface' (to automatically pick a MAC address from a network interface)
     * or a MAC address string.
     */
    public static final String STRATEGY_TIME = UuidJsonProvider.STRATEGY_TIME;
}
