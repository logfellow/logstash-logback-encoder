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


/**
 * A pool of {@link ReusableByteBuffer}.
 * 
 * <p>The pool is technically unbounded but will never hold more buffers than the number of concurrent
 * threads accessing it. Buffers are kept in the pool using soft references so they can be garbage
 * collected by the JVM when running low in memory.
 * 
 * @author brenuart
 */
public class ReusableByteBufferPool extends ObjectPool<ReusableByteBuffer> {
    
    /**
     * Create a new buffer pool holding buffers with an initial capacity of {@code initialSize} bytes.
     *
     * @param initialCapacity the initial capacity of buffers created by this pool.
     */
    private ReusableByteBufferPool(int initialCapacity) {
        super(() -> new ReusableByteBuffer(initialCapacity));

    }
    
    /**
     * Return a buffer to the pool after usage.
     * 
     * @param buffer the buffer to return to the pool.
     */
    @Override
    protected boolean recycleInstance(ReusableByteBuffer buffer) {
        buffer.reset();
        return true;
    }
    
    
    /**
     * Create a new buffer pool holding buffers with an initial capacity of {@code initialSize} bytes.
     *
     * @param initialCapacity the initial capacity of buffers created by this pool.
     * @return a new {@link ReusableByteBufferPool}
     */
    public static ReusableByteBufferPool create(int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("initialCapacity must be greater than 0");
        }
        return new ReusableByteBufferPool(initialCapacity);
    }
}
