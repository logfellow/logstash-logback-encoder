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
package net.logstash.logback.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * A speedy alternative to {@link java.io.ByteArrayOutputStream}.
 *
 * <p>Unlike {@link java.io.ByteArrayOutputStream}, this implementation is backed by an {@link ArrayList}
 * of {@code byte[]} instead of 1 constantly resizing {@code byte[]} array. It does not copy buffers when
 * it gets expanded.
 *
 * <p>The initial buffer is only created when the stream is first written.
 * There is also no copying of the internal buffer if its contents is extracted with the
 * {@link #writeTo(OutputStream)} method.
 *
 * <p>The {@link #reset()} method clears the content and resets the buffer to its initial state.
 * Buffers are disposed except the initial buffer which is reused by subsequent usage.
 *
 * <p>This class is *not* thread-safe!
 * 
 * <p>Note: This class is for internal use only and subject to backward incompatible change
 * at any time.
 * 
 * @author brenuart
 */
public class ReusableByteBuffer extends OutputStream {

    /**
     * The default size of the initial buffer
     */
    static final int DEFAULT_INITIAL_CAPACITY = 1024;

    /**
     * Constant with an empty byte array
     */
    private static final byte[] EMPTY_BYTES = new byte[0];

    /**
     * The buffers used to store the content bytes
     */
    private final List<byte[]> buffers = new ArrayList<>();

    /**
     * The number of bytes already written in previous buffers (other than tail).
     */
    private int alreadyBufferedSize = 0;

    /**
     * The write index in the tail buffer
     */
    private int tailWriteIndex = 0;

    /**
     * Is the stream closed?
     */
    private boolean closed = false;


    /**
     * Create a new {@link ReusableByteBuffer} with the default initial capacity of 1024 bytes.
     */
    public ReusableByteBuffer() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Create a new {@link ReusableByteBuffer} with the specified initial capacity.
     *
     * @param initialCapacity the initial buffer size in bytes
     */
    public ReusableByteBuffer(int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("initialCapacity must be greater than 0");
        }
        this.buffers.add(new byte[initialCapacity]);
    }


    @Override
    public void write(int datum) throws IOException {
        if (this.closed) {
            throw new IOException("Stream closed");
        }

        growIfNeeded();
        getTailBuffer()[this.tailWriteIndex++] = (byte) datum;
    }


    @Override
    public void write(byte[] data, int offset, int length) throws IOException {
        Objects.requireNonNull(data, "data must not be null");
        if (offset < 0 || offset + length > data.length || length < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (this.closed) {
            throw new IOException("Stream closed");
        }


        while (length > 0) {
            byte[] buffer = getTailBuffer();
            int freeSpace = buffer.length - this.tailWriteIndex;

            if (freeSpace > 0) {
                int toCopy = Math.min(freeSpace, length);
                System.arraycopy(data, offset, buffer, this.tailWriteIndex, toCopy);
                offset += toCopy;
                this.tailWriteIndex += toCopy;
                length -= toCopy;
            }

            if (length > 0) {
                growIfNeeded();
            }
        }
    }


    @Override
    public void close() {
        this.closed = true;
    }


    /**
     * Return the current size of the buffer.
     * 
     * @return the current size of the buffer.
     */
    public int size() {
        return this.alreadyBufferedSize + this.tailWriteIndex;
    }


    /**
     * Reset the contents of this {@link ReusableByteBuffer}.
     * <p>All currently accumulated output in the output stream is discarded.
     * The output stream can be used again.
     */
    public void reset() {
        // Clear allocated buffers but keep the first one
        if (buffers.size() > 1) {
            byte[] initialBuffer = this.buffers.get(0);
            this.buffers.clear();
            this.buffers.add(initialBuffer);
        }
        
        this.closed = false;
        this.tailWriteIndex = 0;
        this.alreadyBufferedSize = 0;
    }


    /**
     * Write the buffers content to the given OutputStream.
     *
     * @param out the OutputStream to write to
     * @throws IOException in case of problems writing into the output stream
     */
    public void writeTo(OutputStream out) throws IOException {
        Iterator<byte[]> it = this.buffers.iterator();
        while (it.hasNext()) {
            byte[] buffer = it.next();
            if (it.hasNext()) {
                out.write(buffer, 0, buffer.length);
            } else {
                out.write(buffer, 0, this.tailWriteIndex);
            }
        }
    }


    /**
     * Creates a newly allocated byte array.
     * 
     * <p>Its size is the current size of this output stream and the valid contents
     * of the buffer have been copied into it.</p>
     *
     * @return the current contents of this output stream, as a byte array.
     * @see #size()
     */
    public byte[] toByteArray() {
        int totalSize = size();
        if (totalSize == 0) {
            return EMPTY_BYTES;
        }

        byte[] result = new byte[totalSize];

        int offset = 0;
        Iterator<byte[]> it = this.buffers.iterator();
        while (it.hasNext()) {
            byte[] buffer = it.next();
            if (it.hasNext()) {
                System.arraycopy(buffer, 0, result, offset, buffer.length);
                offset += buffer.length;
            } else {
                System.arraycopy(buffer, 0, result, offset, this.tailWriteIndex);
            }
        }

        return result;
    }


    /**
     * Allocate a new chunk if needed
     */
    private void growIfNeeded() {
        if (getTailBuffer().length == this.tailWriteIndex) {
            this.alreadyBufferedSize += this.tailWriteIndex;
            this.buffers.add(new byte[this.tailWriteIndex * 2]); // block size doubles each time
            this.tailWriteIndex = 0;
        }
    }

    /**
     * Convenience method to get the tail buffer (the one to write into)
     *
     * @return the tail buffer
     */
    private byte[] getTailBuffer() {
        return this.buffers.get(this.buffers.size() - 1);
    }
}
