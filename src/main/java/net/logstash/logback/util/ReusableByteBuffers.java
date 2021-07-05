/**
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

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A pool of {@link ReusableByteBuffer}.
 * 
 * <p>The pool is unbounded and can hold as many buffers as needed. Buffers are kept in the pool
 * using weak references so they can be garbage collected by the JVM when running low in memory. 
 * 
 * @author brenuart
 */
public class ReusableByteBuffers {

    /**
     * Pool of reusable buffers.
     */
    private final Deque<Reference<ReusableByteBuffer>> buffers = new ConcurrentLinkedDeque<>();
    
    /**
     * The size (in bytes) of the initial buffer that is reused across consecutive usages.  
     */
    private final int initialSize;
    
    /**
     * Create a new buffer pool holding buffers with an initial capacity of {@code initialSize} bytes.
     *  
     * @param initialSize the initial capacity of buffers created by this pool.
     */
    public ReusableByteBuffers(int initialSize) {
        this.initialSize = initialSize;
    }
    
    /**
     * Create a new buffer pool holding buffers with a default initial capacity.
     */
    public ReusableByteBuffers() {
        this(ReusableByteBuffer.INITIAL_SIZE);
    }

    /**
     * Create a new buffer with an initial size of {@link #initialSize} bytes.
     * 
     * @return a new buffer instance
     */
    private ReusableByteBuffer createBuffer() {
        return new ReusableByteBuffer(initialSize);
    }
    
    /**
     * Get a buffer from the pool or create a new one if none is available.
     * The buffer must be returned to the pool after usage by a call to {@link #releaseBuffer(ReusableByteBuffer)}.
     * 
     * @return a reusable byte buffer
     */
    public ReusableByteBuffer getBuffer() {
        ReusableByteBuffer buffer=null;
        
        while(buffer==null) {
            Reference<ReusableByteBuffer> ref = buffers.poll();
            if (ref==null) {
                break;
            }
            buffer = ref.get();
        }
        
        if (buffer==null) {
            buffer = createBuffer();
        }
        
        return buffer;
    }
    
    /**
     * Return a buffer to the pool after usage. 
     * 
     * @param buffer the buffer to return to the pool.
     */
    public void releaseBuffer(ReusableByteBuffer buffer) {
        buffer.reset();
        this.buffers.add(new SoftReference<>(buffer));
    }
}