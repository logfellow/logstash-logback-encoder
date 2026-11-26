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
package net.logstash.logback.abbreviator;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ch.qos.logback.classic.pattern.Abbreviator;

/**
 * An {@link Abbreviator} that caches results from a {@link #delegate} abbreviator.
 * 
 * Logger names are typically reused constantly, so caching abbreviations
 * of class names helps performance. Results are divided among independent segments.
 * Each segment evicts names in insertion order, so hits do not update the eviction order.
 */
public class CachingAbbreviator implements Abbreviator {
    
    private final Abbreviator delegate;
    private final Segment[] segments;

    public CachingAbbreviator(Abbreviator delegate) {
        this(delegate, 16, 64);
    }

    CachingAbbreviator(Abbreviator delegate, int segmentCount, int segmentCapacity) {
        if (segmentCount < 1 || segmentCapacity < 1) {
            throw new IllegalArgumentException("segment count and capacity must be positive");
        }
        this.delegate = delegate;
        this.segments = new Segment[segmentCount];
        for (int i = 0; i < segments.length; i++) {
            segments[i] = new Segment(segmentCapacity);
        }
    }

    @Override
    public String abbreviate(String in) {
        int hash = in.hashCode();
        Segment segment = segments[Math.floorMod(hash ^ (hash >>> 16), segments.length)];
        String value = segment.cache.get(in);
        return value != null ? value : segment.cacheMiss(in, delegate);
    }

    /**
     * Clears the cache.
     */
    public void clear() {
        for (Segment segment : segments) {
            segment.clear();
        }
    }

    private static final class Segment {
        private final ConcurrentMap<String, String> cache = new ConcurrentHashMap<>();
        private final String[] insertionOrder;
        private int next;

        private Segment(int capacity) {
            this.insertionOrder = new String[capacity];
        }

        private synchronized String cacheMiss(String in, Abbreviator delegate) {
            String value = cache.get(in);
            if (value != null) {
                return value;
            }

            value = cache.computeIfAbsent(in, delegate::abbreviate);
            if (value != null) {
                // The map may briefly hold one extra entry between insertion and eviction.
                String oldest = insertionOrder[next];
                if (oldest != null) {
                    cache.remove(oldest);
                }
                insertionOrder[next] = in;
                next = (next + 1) % insertionOrder.length;
            }
            return value;
        }

        private synchronized void clear() {
            cache.clear();
            Arrays.fill(insertionOrder, null);
            next = 0;
        }
    }

}
