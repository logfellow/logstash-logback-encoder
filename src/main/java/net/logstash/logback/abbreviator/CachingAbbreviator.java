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
package net.logstash.logback.abbreviator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ch.qos.logback.classic.pattern.Abbreviator;

/**
 * An {@link Abbreviator} that caches results from a {@link #delegate} abbreviator.
 * 
 * Logger names are typically reused constantly, so caching abbreviations
 * of class names helps performance.
 */
public class CachingAbbreviator implements Abbreviator {
    
    private final Abbreviator delegate;
    
    private final ConcurrentMap<String, String> cache = new ConcurrentHashMap<>();
    
    public CachingAbbreviator(Abbreviator delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public String abbreviate(String in) {
        return cache.computeIfAbsent(in, delegate::abbreviate);
    }
    
    /**
     * Clears the cache.
     */
    public void clear() {
        cache.clear();
    }

}
