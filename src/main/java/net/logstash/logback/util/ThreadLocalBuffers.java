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

import java.lang.ref.SoftReference;

import com.fasterxml.jackson.core.io.SegmentedStringWriter;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;

/**
 * Utility to maintain thread-bound buffers.
 */
public class ThreadLocalBuffers {

    /**
     * This <code>ThreadLocal</code> contains a {@link java.lang.ref.SoftReference}
     * to a {@link BufferRecycler} used to provide a low-cost
     * buffer recycling between writer instances.
     */
    private final ThreadLocal<SoftReference<BufferRecycler>> recycler = new ThreadLocal<SoftReference<BufferRecycler>>() {
        protected SoftReference<BufferRecycler> initialValue() {
            final BufferRecycler bufferRecycler = new BufferRecycler();
            return new SoftReference<BufferRecycler>(bufferRecycler);
        }
    };
    
    
    private BufferRecycler getBufferRecycler() {
        SoftReference<BufferRecycler> bufferRecyclerReference = recycler.get();
        BufferRecycler bufferRecycler = bufferRecyclerReference.get();
        if (bufferRecycler == null) {
            recycler.remove();
            return getBufferRecycler();
        }
        return bufferRecycler;
    }
    
    
    /**
     * Get a thread-bound {@link ByteArrayBuilder} with a default initial size.
     * 
     * <p>Do not forget to release the buffer by calling {@link ByteArrayBuilder#release()} 
     * when done otherwise the memory allocated by the buffer will not be recycled and new 
     * memory allocations will occur for every subsequent buffers.
     * 
     * @return a thread-bound {@link ByteArrayBuilder}
     */
    public ByteArrayBuilder getByteBuffer() {
        return new ByteArrayBuilder(getBufferRecycler());
    }

    /**
     * Get a thread-bound {@link ByteArrayBuilder} with the specified initial size.
     * A buffer of the default size is allocated with the initial size is less than the default.
     * 
     * <p>Do not forget to release the buffer by calling {@link ByteArrayBuilder#release()} 
     * when done otherwise the memory allocated by the buffer will not be recycled and new 
     * memory allocations will occur for every subsequent buffers.
     * 
     * @param initialSize the initial size of the buffer
     * @return a thread-bound {@link ByteArrayBuilder}
     */
    public ByteArrayBuilder getByteBuffer(int initialSize) {
        return new ByteArrayBuilder(getBufferRecycler(), initialSize);
    }
    
    /**
     * Get a thread-bound {@link SegmentedStringWriter}.
     * 
     * @return a thread-bound {@link SegmentedStringWriter}
     */
    public SegmentedStringWriter getStringWriter() {
        return new SegmentedStringWriter(getBufferRecycler());
    }
}
