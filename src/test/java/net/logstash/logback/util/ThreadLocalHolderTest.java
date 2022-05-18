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
package net.logstash.logback.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import net.logstash.logback.util.ThreadLocalHolder.HolderRef;
import net.logstash.logback.util.ThreadLocalHolder.Lifecycle;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author brenuart
 *
 */
public class ThreadLocalHolderTest {

    private ThreadLocalHolder<PooledObject> pool = new ThreadLocalHolder<>(this::createInstance);

    private ExecutorService executor = new TestExecutorService();


    @AfterEach
    public void teardown() {
        this.executor.shutdown();
    }


    /*
     * Assert the same value is returned every time
     */
    @Test
    public void testAcquire_sameValueEveryTime() {
        PooledObject obj1 = pool.acquire();
        assertThat(obj1).isNotNull();

        // Release and acquire again - must be same value
        pool.release();
        assertThat(pool.acquire()).isSameAs(obj1);
    }


    /*
     * Assert different threads receive different values
     */
    @Test
    public void testAcquire_threadsReceiveDifferentValues() throws Exception {
        PooledObject obj1 = executor.submit(() -> acquire()).get();
        PooledObject obj2 = executor.submit(() -> acquire()).get();

        assertThat(obj1).isNotSameAs(obj2);
    }

    private PooledObject acquire() {
        System.out.println("acquire from thread " + Thread.currentThread());
        return pool.acquire();
    }

    /*
     * Assert an exception is thrown when value is not released
     */
    @Test
    public void testNotReleased() {
        // acquire the value
        assertThat(pool.acquire()).isNotNull();

        // acquire a second time without a release
        assertThatThrownBy(() -> pool.acquire()).isInstanceOf(IllegalStateException.class);
    }


    /*
     * Assert Lifecyle#recycle() is invoked when value is released
     */
    @Test
    public void testRecycle() {
        PooledObject obj1 = pool.acquire();
        pool.release();

        verify(obj1, times(1)).recycle();
    }


    /*
     * Assert value is disposed when Lifecycle#recycle() returns false
     */
    @Test
    public void testNotRecyclable() {
        PooledObject obj1 = pool.acquire();
        when(obj1.recycle()).thenReturn(false);

        pool.release();

        verify(obj1, times(1)).recycle();
        verify(obj1, times(1)).dispose();

        assertThat(pool.acquire()).isNotSameAs(obj1);
    }


    /*
     * Assert values owned by dead threads are disposed, even if not yet released
     */
    @Test
    public void testValueDisposedOnThreadDeath() throws Exception {
        // Get value from a separate thread
        // The thread is now dead and its reference should be enqueued
        PooledObject unreleasedObj = executor.submit(() -> pool.acquire()).get();
        PooledObject releasedObj = executor.submit(() -> acquireAndRelease()).get();

        // Cleanup of dead threads happens when calling "release()".
        // Lets acquire and release another value from the main thread to trigger
        // the house keeping stuff.
        pool.acquire();
        pool.release();

        verify(releasedObj, times(1)).dispose();
        verify(unreleasedObj, times(1)).dispose();
        assertThat(pool.threadValues).hasSize(1);
    }


    /*
     * Assert values are disposed when calling #close()
     */
    @Test
    public void testClose() {
        PooledObject obj1 = pool.acquire();
        pool.release();

        pool.close();
        verify(obj1, times(1)).dispose();
        assertThat(pool.threadValues).isEmpty();
    }


    /*
     * Assert that acquire is till possible after the ThreadLocalHolder is closed but values
     * are immediately disposed when released instead of reused.
     */
    @Test
    public void testClose_subsequentAcquiredValuesDisposed() {
        pool.close();

        PooledObject obj1 = pool.acquire();
        pool.release();

        verify(obj1, times(1)).dispose();
        assertThat(pool.acquire()).isNotSameAs(obj1);
    }


    /*
     * Close while a thread is still running and acquired a value
     */
    @Test
    public void testClose_withRunningThread() {
        CyclicBarrier barrier = new CyclicBarrier(2);

        // Acquire a value on separate thread and stay alive until latch is released
        AtomicReference<PooledObject> objRef = new AtomicReference<>();
        executor.submit(() -> {
            objRef.set(pool.acquire());
            await(barrier);

            await(barrier);
            pool.release();

            awaitUntilInterrupted(); // stay alive until test case is complete
        });


        // Wait until the thread acquired the value
        await(barrier);
        assertThat(pool.threadValues).hasSize(1);

        // Close -
        pool.close();
        verify(objRef.get(), never()).dispose();

        // Signal the thread to release its value
        await(barrier);
        verify(objRef.get(), timeout(100_000).times(1)).dispose();
    }


    /*
     * NullPointer exception thrown if factory returns null
     */
    @Test
    public void testFactoryReturnsNull() {
        pool = new ThreadLocalHolder<PooledObject>(() -> null);
        assertThatThrownBy(() -> pool.acquire()).isInstanceOf(NullPointerException.class);
    }


    /*
     * Exception thrown by the factory is propagated to ObjectPool#acquire()
     */
    @Test
    public void testFactoryThrowsException() {
        RuntimeException e = new RuntimeException();

        pool = new ThreadLocalHolder<PooledObject>(() -> {
            throw e;
        });

        assertThatThrownBy(() -> pool.acquire()).isSameAs(e);
    }


    /*
     * Exception thrown by Lifecycle#recycle() -> survive and do not recycle
     */
    @Test
    public void testRecycleThrowsException() {
       PooledObject obj1 = spy(pool.acquire());
       when(obj1.recycle()).thenThrow(new RuntimeException());

       assertThatCode(() -> pool.release()).doesNotThrowAnyException();
       assertThat(pool.acquire()).isNotSameAs(obj1);
    }


    /*
     * Exception thrown by Lifecycle#dispose() -> survive and do not recycle
     */
    @Test
    public void testDisposeThrowsException() {
        PooledObject obj1 = spy(pool.acquire());
        doThrow(new RuntimeException()).when(obj1).dispose();

        assertThatCode(() -> pool.release()).doesNotThrowAnyException();
        assertThat(pool.acquire()).isNotSameAs(obj1);
    }

    /**
     * Test to verify that memory leak explained in
     * https://github.com/logfellow/logstash-logback-encoder/issues/722 is not happening again.
     */
    @Test
    public void testThreadValues_expectsAtMaximumOneValuePerThreadInThreadValues() {
        final ForkJoinPool commonPool = ForkJoinPool.commonPool();
        final int poolSize = ForkJoinPool.getCommonPoolParallelism();

        for (int i = 0; i < 10_000; i++) {
            commonPool
                    .execute(
                            () -> {
                                final PooledObject acquire = acquireAndRelease();

                                assertThat(acquire.threadId).isEqualTo(Thread.currentThread().getId());
                            });
        }
        // try to wait for some time so that it executes the commonPool tasks
        commonPool.awaitQuiescence(5, TimeUnit.SECONDS);
        // it should have at maximum the number of threads plus 1 which indicates the main thread which ForkJoinPool
        // might end up using for optimization
        assertThat(pool.threadValues.size()).isLessThanOrEqualTo(poolSize + 1);
    }


    // --------------------------------------------------------------------------------------------

    private PooledObject createInstance() {
        return spy(new PooledObject(Thread.currentThread().getId()));
    }

    public static class PooledObject implements ThreadLocalHolder.Lifecycle {
        private long threadId;

        public PooledObject(long threadId) {
            this.threadId = threadId;
        }

        @Override
        public boolean recycle() {
            return Lifecycle.super.recycle();
        }

        @Override
        public void dispose() {
            Lifecycle.super.dispose();
        }
    }

    /*
     * Utility method to acquire and release a value in one go
     */
    private PooledObject acquireAndRelease() {
        PooledObject obj = pool.acquire();
        pool.release();
        return obj;
    }

    /*
     * Await until the current thread is interrupted
     */
    private void awaitUntilInterrupted() {
        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /*
     * Await for the CyclicBarrier ignoring any exception
     */
    private void await(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * An {@link ExecutorService} that executes each submitted task on a new thread and takes care
     * of notifying the {@link ThreadLocalHolder} when a thread is about to die.
     *
     * This implementation guarantees that the {@link ThreadLocalHolder} is notified about the death
     * of the thread *before* any Future is unblocked.
     */
    private class TestExecutorService extends AbstractExecutorService {
        private final List<Thread> runningThreads = new CopyOnWriteArrayList<>();

        @Override
        @SuppressWarnings("unchecked")
        protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
            /*
             * Wrap the Callable and notify about the thread death at the end of its execution.
             */
            @SuppressWarnings("rawtypes")
            Callable wrappedCallable = new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    try {
                        return callable.call();
                    } finally {
                        notifyThreadDeath(Thread.currentThread());
                    }
                }
            };

            return super.newTaskFor(wrappedCallable);
        }

        @Override
        protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
            return newTaskFor(Executors.callable(runnable, value));
        }

        @Override
        public void execute(Runnable command) {
            Thread t = new Thread(command) {
                @Override
                public void run() {
                    try {
                        super.run();
                    } finally {
                        runningThreads.remove(this);
                    }
                }
            };
            t.start();
            runningThreads.add(t);
        }

        @SuppressWarnings("rawtypes")
        private void notifyThreadDeath(Thread thread) {
            for (HolderRef ref: pool.threadValues.values()) {
                if (ref.get() == thread) {
                    ref.enqueue();
                }
            }
        }

        @Override
        public void shutdown() {
            for (Thread t: runningThreads) {
                if (t.isAlive()) {
                    t.interrupt();
                }
            }
        }

        @Override
        public List<Runnable> shutdownNow() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isShutdown() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isTerminated() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            throw new UnsupportedOperationException();
        }
    }
}
