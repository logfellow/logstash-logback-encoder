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
package net.logstash.logback.pattern;

import java.util.function.Function;

/**
 * Computes a value given an event.
 *
 * @param <T> - type of the computed value
 * @param <Event> - type of the event (ILoggingEvent, IAccessEvent)
 *
 * @author <a href="mailto:dimas@dataart.com">Dmitry Andrianov</a>
 */
public interface ValueGetter<T, Event> {
    /**
     * Get the result of applying the ValueGetter to the event
     * 
     * @param event the event to apply this ValueGetter
     * @return the result of applying this ValueGetter on the event
     */
    T getValue(Event event);
    
    
    /**
     * Returns a composed ValueGetter that first applies this ValueGetter to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *            composed ValueGetter
     * @param after the function to apply after this ValueGetter is applied
     * @return a composed ValueGetter that first applies this ValueGetter and then
     *         applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default <V> ValueGetter<V, Event> andThen(Function<T, V> after) {
        return event -> {
            T value = getValue(event);
            return after.apply(value);
        };
    }
}
