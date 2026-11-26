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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.qos.logback.classic.pattern.Abbreviator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CachingAbbreviatorTest {
    
    @Mock
    private Abbreviator delegate;
    
    @Test
    public void test() {
        when(delegate.abbreviate("full")).thenReturn("abbreviation");
        
        CachingAbbreviator abbreviator = new CachingAbbreviator(delegate);
        
        assertThat(abbreviator.abbreviate("full")).isEqualTo("abbreviation");
        assertThat(abbreviator.abbreviate("full")).isEqualTo("abbreviation");
        
        verify(delegate, times(1)).abbreviate("full");
        
        abbreviator.clear();
        
        assertThat(abbreviator.abbreviate("full")).isEqualTo("abbreviation");
        
        verify(delegate, times(2)).abbreviate("full");
    }

    @Test
    void boundsTheDefaultCache() {
        AtomicBoolean populate = new AtomicBoolean(true);
        CachingAbbreviator abbreviator = new CachingAbbreviator(name -> populate.get() ? name : null);
        for (int i = 0; i < 4096; i++) {
            String name = "logger" + i;
            assertThat(abbreviator.abbreviate(name)).isEqualTo(name);
        }

        // Returning null for misses measures retention without changing the cache.
        populate.set(false);
        int retained = 0;
        for (int i = 0; i < 4096; i++) {
            if (abbreviator.abbreviate("logger" + i) != null) {
                retained++;
            }
        }
        assertThat(retained).isPositive().isLessThanOrEqualTo(1024);
    }

    @Test
    void evictsInInsertionOrderAndIgnoresUnsuccessfulResults() {
        when(delegate.abbreviate("a")).thenReturn("A", "A2");
        when(delegate.abbreviate("b")).thenReturn("B");
        when(delegate.abbreviate("c")).thenReturn("C");
        when(delegate.abbreviate("missing")).thenReturn(null, "found");
        when(delegate.abbreviate("bad")).thenThrow(new IllegalStateException("bad"));
        CachingAbbreviator abbreviator = new CachingAbbreviator(delegate, 1, 2);

        assertThat(abbreviator.abbreviate("a")).isEqualTo("A");
        assertThat(abbreviator.abbreviate("b")).isEqualTo("B");
        assertThat(abbreviator.abbreviate("missing")).isNull();
        assertThatThrownBy(() -> abbreviator.abbreviate("bad")).isInstanceOf(IllegalStateException.class);
        for (int i = 0; i < 10; i++) {
            assertThat(abbreviator.abbreviate("a")).isEqualTo("A");
        }

        assertThat(abbreviator.abbreviate("c")).isEqualTo("C");
        assertThat(abbreviator.abbreviate("b")).isEqualTo("B");
        verify(delegate).abbreviate("b");
        assertThat(abbreviator.abbreviate("a")).isEqualTo("A2");
        assertThat(abbreviator.abbreviate("missing")).isEqualTo("found");
    }

    @Test
    void computesMissesInDifferentSegmentsConcurrently() throws Exception {
        CountDownLatch entered = new CountDownLatch(2);
        CachingAbbreviator abbreviator = new CachingAbbreviator(name -> {
            entered.countDown();
            try {
                assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError(interrupted);
            }
            return name;
        });

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = pool.submit(() -> abbreviator.abbreviate("a"));
            Future<String> second = pool.submit(() -> abbreviator.abbreviate("b"));
            assertThat(first.get(15, TimeUnit.SECONDS)).isEqualTo("a");
            assertThat(second.get(15, TimeUnit.SECONDS)).isEqualTo("b");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentInsertionsAndClearLeaveABoundedCache() throws Exception {
        int capacity = 3;
        int writers = 3;
        int namesPerWriter = 64;
        AtomicBoolean populate = new AtomicBoolean(true);
        CachingAbbreviator abbreviator = new CachingAbbreviator(name -> populate.get() ? name : null, 1, capacity);
        ExecutorService pool = Executors.newFixedThreadPool(writers);
        CyclicBarrier start = new CyclicBarrier(writers);
        List<Future<Void>> tasks = new ArrayList<>();
        try {
            for (int writer = 0; writer < writers; writer++) {
                int first = writer * namesPerWriter;
                tasks.add(pool.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    for (int i = 0; i < namesPerWriter; i++) {
                        if (first == 0 && i == namesPerWriter / 2) {
                            abbreviator.clear();
                        }
                        String name = "logger" + (first + i);
                        assertThat(abbreviator.abbreviate(name)).isEqualTo(name);
                    }
                    return null;
                }));
            }
            for (Future<Void> task : tasks) {
                task.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        // A null delegate result lets us probe misses without evicting a resident key.
        populate.set(false);
        int retained = 0;
        for (int i = 0; i < writers * namesPerWriter; i++) {
            if (abbreviator.abbreviate("logger" + i) != null) {
                retained++;
            }
        }
        assertThat(retained).isEqualTo(capacity);
    }

}
