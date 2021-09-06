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

import ch.qos.logback.core.spi.DeferredProcessingAware;

/**
 * @author brenuart
 *
 */
public class ReusableJsonFormatterPool<Event extends DeferredProcessingAware> {

    private final int minBufferSize;
    
    private final CompositeJsonFormatter<Event> formatterFactory;
    
    private SoftReference<Deque<ReusableJsonFormatter>> formatters = new SoftReference<>(null);
    

    public ReusableJsonFormatterPool(CompositeJsonFormatter<Event> formatterFactory, int minBufferSize) {
        this.formatterFactory = Objects.requireNonNull(formatterFactory);
        this.minBufferSize = minBufferSize;
    }

    public class ReusableJsonFormatter implements Closeable {
        private final ReusableByteBuffer buffer;
        private final CompositeJsonFormatter<Event>.JsonFormatter formatter;
        
        ReusableJsonFormatter(ReusableByteBuffer buffer, CompositeJsonFormatter<Event>.JsonFormatter formatter) {
            this.buffer = buffer;
            this.formatter = formatter;
        }
        
        public ReusableByteBuffer getBuffer() {
            return buffer;
        }
        
        public void write(Event event) throws IOException {
            this.formatter.writeEvent(event);
        }
        
        @Override
        protected void finalize() throws Throwable {
            dispose();
        }
        
        public void dispose() throws IOException {
            this.formatter.dispose();
            this.buffer.reset();
        }
        
        public void reset() {
            this.buffer.reset();
        }
        
        @Override
        public void close() throws IOException {
            release(this);
        }
    }
    
    public ReusableJsonFormatter acquire() throws IOException {
        ReusableJsonFormatter cachedFormatter = null;

        Deque<ReusableJsonFormatter> cachedFormatters = formatters.get();
        if (cachedFormatters != null) {
            cachedFormatter = cachedFormatters.poll();
        }
        
        if (cachedFormatter == null) {
            cachedFormatter = createJsonFormatter();
        }
        
        return cachedFormatter;
    }
    
    protected ReusableJsonFormatter createJsonFormatter() throws IOException {
        ReusableByteBuffer buffer = new ReusableByteBuffer(this.minBufferSize);
        CompositeJsonFormatter<Event>.JsonFormatter jsonFormatter = formatterFactory.createJsonFormatter(buffer);
        return new ReusableJsonFormatter(buffer, jsonFormatter);
    }
    
    public void release(ReusableJsonFormatter cachedFormatter) {
        if (cachedFormatter == null) {
            return;
        }
        
//        if (isStarted()) {
            /*
             * Create a new Deque if the SoftReference has been cleared.
             * The new instance may be lost if another thread executes this code concurrently and the
             * released JsonFormatter won't be reused. This behavior is expected and is far cheaper than
             * implementing a more robust concurrent solution.
             */
            Deque<ReusableJsonFormatter> cachedFormatters = formatters.get();
            if (cachedFormatters == null) {
                cachedFormatters = new ConcurrentLinkedDeque<>();
                formatters = new SoftReference<>(cachedFormatters);
            }

            /*
             * Reset the internal buffer and return the cached JsonFormatter to the pool.
             */
            cachedFormatter.reset();
            cachedFormatters.addFirst(cachedFormatter); // try to reuse the same as much as we can -> add it first
            
//        } else {
//            // Encoder is stopped - dispose the JSON formatter instead of adding it
//            // back into the pool
//            try {
//                cachedFormatter.dispose();
//            } catch (IOException e) {
//                // ignore
//            }
//        }
    }
    
}
