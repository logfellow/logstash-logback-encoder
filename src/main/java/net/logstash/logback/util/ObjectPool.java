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
package net.logstash.logback.util;

import java.lang.ref.SoftReference;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;


/**
 * Pool of reusable object instances.
 * 
 * <p>Instances are obtained from the pool by calling {@link #acquire()} and must be returned after use
 * by calling {@link #release(Object)}. If not, the instance is simply reclaimed by the garbage collector.
 * 
 * <p>Instance may also implement the optional {@link ObjectPool.Lifecycle} interface if they wish to be
 * notified when they are recycled or disposed.
 * 
 * <p>The pool is technically unbounded but will never hold more entries than the number of concurrent
 * threads accessing it. Entries are kept in the pool using soft references so they can be garbage
 * collected by the JVM when running low in memory.
 * 
 * <p>The pool can be cleared at any time by calling {@link #clear()} in which case instances currently
 * in the pool will be disposed.
 * 
 * @param <T> type of pooled instances.
 *
 * @author brenuart
 */
public class ObjectPool<T> {

    /**
     * The factory used to create new instances
     */
    private final Supplier<T> factory;
    
    /**
     * Instances pool
     */
    private volatile SoftReference<Deque<T>> poolRef = new SoftReference<>(null);

    
    /**
     * Create a new instance of the pool.
     * 
     * @param factory the factory used to create new instances.
     */
    public ObjectPool(Supplier<T> factory) {
        this.factory = Objects.requireNonNull(factory);
    }
    
    /**
     * Get an instance out of the pool, creating a new one if needed.
     * The instance must be returned to the pool by calling {@link #release(Object)}. If not the
     * instance is disposed by the garbage collector.
     * 
     * @return a pooled instance or a new one if none is available
     */
    public final T acquire() {
        T instance = null;

        Deque<T> pool = this.poolRef.get();
        if (pool != null) {
            instance = pool.poll();
        }
        
        if (instance == null) {
            instance = Objects.requireNonNull(createNewInstance());
        }
        
        return instance;
    }
    
    
    /**
     * Return an instance to the pool or dispose it if it cannot be recycled.
     * 
     * @param instance the instance to return to the pool
     */
    public final void release(T instance) {
        if (instance == null) {
            return;
        }
        if (!recycleInstance(instance)) {
            disposeInstance(instance);
            return;
        }

        Deque<T> pool = this.poolRef.get();
        if (pool == null) {
            pool = new ConcurrentLinkedDeque<>();
            this.poolRef = new SoftReference<>(pool);
        }

        pool.addFirst(instance); // try to reuse the same as much as we can -> add it first
    }
    
    
    /**
     * Clear the object pool and dispose instances it may contain
     */
    public void clear() {
        Deque<T> pool = this.poolRef.get();
        if (pool != null) {
            while (!pool.isEmpty()) {
                disposeInstance(pool.poll());
            }
        }
    }
    
    
    /**
     * Get the number of instances currently in the pool
     * 
     * @return the number of instances in the pool
     */
    public int size() {
        Deque<T> pool = this.poolRef.get();
        if (pool != null) {
            return pool.size();
        } else {
            return 0;
        }
    }
    
    
    /**
     * Create a new object instance.
     * 
     * @return a new object instance
     */
    protected T createNewInstance() {
        return this.factory.get();
    }
    
    
    /**
     * Dispose the object instance by calling its life cycle methods.
     * 
     * @param instance the instance to dispose
     */
    protected void disposeInstance(T instance) {
        if (instance instanceof Lifecycle) {
            ((Lifecycle) instance).dispose();
        }
    }
    
    
    /**
     * Recycle the instance before returning it to the pool.
     * 
     * @param instance the instance to recycle
     * @return {@code true} if the instance can be recycled and returned to the pool, {@code false} if not.
     */
    protected boolean recycleInstance(T instance) {
        if (instance instanceof Lifecycle) {
            return ((Lifecycle) instance).recycle();
        } else {
            return true;
        }
    }
    
    
    /**
     * Optional interface that pooled instances may implement if they wish to be notified of
     * life cycle events.
     */
    public interface Lifecycle {
        /**
         * Indicate whether the instance can be recycled and returned to the pool and perform
         * the necessary recycling tasks.
         * 
         * @return {@code true} if the instance can be returned to the pool, {@code false} if
         *         it must be disposed instead.
         */
        default boolean recycle() {
            return true;
        }
        
        /**
         * Dispose the instance and free allocated resources.
         */
        default void dispose() {
            // noop
        }
    }
}
