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

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;

import net.logstash.logback.composite.CompositeJsonFormatter;
import net.logstash.logback.composite.CompositeJsonFormatter.JsonFormatter;

import ch.qos.logback.core.spi.DeferredProcessingAware;

/**
 * Pool of {@link ReusableJsonFormatter} that can be safely reused multiple times.
 * A {@link ReusableJsonFormatter} is made of an internal {@link ReusableByteBuffer} and a
 * {@link CompositeJsonFormatter.JsonFormatter} bound to it.
 * 
 * <p>Instances must be returned to the pool after use by calling {@link ReusableJsonFormatter#close()}
 * or {@link #release(net.logstash.logback.util.ReusableJsonFormatterPool.ReusableJsonFormatter)
 * release(ReusableJsonFormatter)}.
 * 
 * Instances are not recycled (and therefore not returned to the pool) after their internal
 * {@link CompositeJsonFormatter.JsonFormatter} threw an exception. This is to prevent reusing an
 * instance whose internal components are potentially in an unpredictable state.
 * 
 * <p>The internal byte buffer is created with an initial size of {@link #minBufferSize}.
 * The buffer automatically grows above the {@code #minBufferSize} when needed to
 * accommodate with larger events. However, only the first {@code minBufferSize} bytes
 * will be reused by subsequent invocations. It is therefore strongly advised to set
 * the minimum size at least equal to the average size of the encoded events to reduce
 * unnecessary memory allocations and reduce pressure on the garbage collector.
 * 
 * <p>The pool is technically unbounded but will never hold more entries than the number of concurrent
 * threads accessing it. Entries are kept in the pool using soft references so they can be garbage
 * collected by the JVM when running low in memory.
 * 
 * @author brenuart
 */
public class ReusableJsonFormatterPool<Event extends DeferredProcessingAware> {

    /**
     * The minimum size of the byte buffer used when encoding events.
     */
    private final int minBufferSize;
    
    /**
     * The factory used to create {@link JsonFormatter} instances
     */
    private final CompositeJsonFormatter<Event> formatterFactory;
    
    /**
     * The pool of reusable JsonFormatter instances.
     * May be cleared by the GC when running low in memory.
     * 
     * Note:
     *   JsonFormatters are not explicitly disposed when the GC clears the SoftReference. This means that
     *   the underlying Jackson JsonGenerator is not explicitly closed and the associated memory buffers
     *   are not returned to Jackson's internal memory pools.
     *   This behavior is desired and makes the associated memory immediately reclaimable - which is what
     *   we need since we are "running low in memory".
     */
    private volatile SoftReference<Deque<ReusableJsonFormatter>> formatters = new SoftReference<>(null);
    

    public ReusableJsonFormatterPool(CompositeJsonFormatter<Event> formatterFactory, int minBufferSize) {
        this.formatterFactory = Objects.requireNonNull(formatterFactory);
        this.minBufferSize = minBufferSize;
    }

    /**
     * A reusable JsonFormatter holding a JsonFormatter writing inside a dedicated {@link ReusableByteBuffer}.
     * Closing the instance returns it to the pool and makes it available for subsequent usage, unless the
     * underlying {@link CompositeJsonFormatter.JsonFormatter} threw an exception during its use.
     * 
     * <p>Note: usage is not thread-safe.
     */
    public class ReusableJsonFormatter implements Closeable {
        private ReusableByteBuffer buffer;
        private CompositeJsonFormatter<Event>.JsonFormatter formatter;
        private boolean recyclable = true;
        
        ReusableJsonFormatter(ReusableByteBuffer buffer, CompositeJsonFormatter<Event>.JsonFormatter formatter) {
            this.buffer = Objects.requireNonNull(buffer);
            this.formatter = Objects.requireNonNull(formatter);
        }

        /**
         * Return the underlying buffer into which the JsonFormatter is writing.
         * 
         * @return the underlying byte buffer
         */
        public ReusableByteBuffer getBuffer() {
            assertNotDisposed();
            return buffer;
        }
        
        /**
         * Write the Event in JSON format into the enclosed buffer using the enclosed JsonFormatter.
         * 
         * @param event the event to write
         * @throws IOException thrown when the JsonFormatter has problem to convert the Event into JSON format
         */
        public void write(Event event) throws IOException {
            assertNotDisposed();
            
            try {
                this.formatter.writeEvent(event);
                
            } catch (IOException e) {
                // Do not recycle the instance after an exception is thrown: the underlying
                // JsonGenerator may not be in a safe state.
                this.recyclable = false;
                throw e;
            }
        }
        
        /**
         * Close the JsonFormatter, release associated resources and return it to the pool.
         */
        @Override
        public void close() throws IOException {
            release(this);
        }
        
        /**
         * Dispose associated resources
         */
        protected void dispose() {
            try {
                this.formatter.close();
            } catch (IOException e) {
                // ignore and proceed
            }
            
            this.formatter = null;
            this.buffer = null;
        }
        
        protected boolean isDisposed() {
            return buffer == null;
        }
        
        protected void assertNotDisposed() {
            if (isDisposed()) {
                throw new IllegalStateException("Instance has been disposed and cannot be used anymore. Did you keep a reference to it after it is closed?");
            }
        }
    }
    
    
    /**
     * Get a {@link ReusableJsonFormatter} out of the pool, creating a new one if needed.
     * The instance must be closed after use to return it to the pool.
     * 
     * @return a {@link ReusableJsonFormatter}
     * @throws IOException thrown when unable to create a new instance
     */
    public ReusableJsonFormatter acquire() throws IOException {
        ReusableJsonFormatter reusableFormatter = null;

        Deque<ReusableJsonFormatter> cachedFormatters = formatters.get();
        if (cachedFormatters != null) {
            reusableFormatter = cachedFormatters.poll();
        }
        
        if (reusableFormatter == null) {
            reusableFormatter = createJsonFormatter();
        }
        
        return reusableFormatter;
    }
    
    
    /**
     * Return an instance to the pool.
     * An alternative is to call {@link ReusableJsonFormatter#close()}.
     * 
     * @param reusableFormatter the instance to return to the pool
     */
    public void release(ReusableJsonFormatter reusableFormatter) {
        if (reusableFormatter == null) {
            return;
        }
        
        /*
         * Dispose the formatter instead of returning to the pool when marked not recyclable
         */
        if (!reusableFormatter.recyclable) {
            reusableFormatter.dispose();
            return;
        }
        
        Deque<ReusableJsonFormatter> cachedFormatters = this.formatters.get();
        if (cachedFormatters == null) {
            cachedFormatters = new ConcurrentLinkedDeque<>();
            this.formatters = new SoftReference<>(cachedFormatters);
        }

        /*
         * Reset the internal buffer and return the cached JsonFormatter to the pool.
         */
        reusableFormatter.getBuffer().reset();
        cachedFormatters.addFirst(reusableFormatter); // try to reuse the same as much as we can -> add it first
    }

    
    /**
     * Create a new {@link ReusableJsonFormatter} instance by allocating a new {@link ReusableByteBuffer}
     * and a {@link CompositeJsonFormatter.JsonFormatter} bound to it.
     * 
     * @return a new {@link ReusableJsonFormatter}
     * @throws IOException thrown when the {@link CompositeJsonFormatter} is unable to create a new instance
     *                     of {@link CompositeJsonFormatter.JsonFormatter}.
     */
    protected ReusableJsonFormatter createJsonFormatter() throws IOException {
        ReusableByteBuffer buffer = new ReusableByteBuffer(this.minBufferSize);
        CompositeJsonFormatter<Event>.JsonFormatter jsonFormatter = this.formatterFactory.createJsonFormatter(buffer);
        return new ReusableJsonFormatter(buffer, jsonFormatter);
    }
}
