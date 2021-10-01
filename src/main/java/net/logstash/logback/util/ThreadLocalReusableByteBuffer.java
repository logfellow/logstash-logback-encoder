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
 * @author brenuart
 */
public class ThreadLocalReusableByteBuffer extends ThreadLocalHolder<ReusableByteBuffer> {
    
    /**
     * Create a new instance with an initial capacity of {@code initialSize} bytes.
     *
     * @param initialCapacity the initial capacity of buffers
     */
    public ThreadLocalReusableByteBuffer(int initialCapacity) {
        super(() -> new ReusableByteBuffer(initialCapacity));
        
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("initialCapacity must be greater than 0");
        }
    }
    
    /**
     * Create a new instance with an initial capacity set to {@value ReusableByteBuffer#DEFAULT_INITIAL_CAPACITY}
     */
    public ThreadLocalReusableByteBuffer() {
        this(ReusableByteBuffer.DEFAULT_INITIAL_CAPACITY);
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
}
