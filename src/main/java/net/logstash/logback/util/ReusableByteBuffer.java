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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
 * @author brenuart
 *
 */
public class ReusableByteBuffer extends OutputStream {

    /**
     * The default size of the initial buffer
     */
    public static final int INITIAL_SIZE = 1024;

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
    private int index = 0;

    /**
     * Is the stream closed?
     */
    private boolean closed = false;


    /**
     * Create a new {@link ReusableByteBuffer}
     * with the default initial capacity of 1024 bytes.
     */
    public ReusableByteBuffer() {
        this(INITIAL_SIZE);
    }

    /**
     * Create a new {@link ReusableByteBuffer}
     * with the specified initial capacity.
     *
     * @param initialBlockSize the initial buffer size in bytes
     */
    public ReusableByteBuffer(int initialBlockSize) {
        if (initialBlockSize <= 0) {
            throw new IllegalArgumentException("Initial block size must be greater than 0");
        }
        this.buffers.add(new byte[initialBlockSize]);
    }


    @Override
    public void write(int datum) throws IOException {
        if (this.closed) {
            throw new IOException("Stream closed");
        }

        growIfNeeded();
        getTailBuffer()[this.index++] = (byte) datum;
    }


    @Override
    public void write(byte[] data, int offset, int length) throws IOException {
        if (data == null) {
            throw new NullPointerException();
        }
        if (offset < 0 || offset + length > data.length || length < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (this.closed) {
            throw new IOException("Stream closed");
        }


        while (length > 0) {
            byte[] buffer = getTailBuffer();
            int freeSpace = buffer.length - this.index;

            if (freeSpace > 0) {
                int toCopy = Math.min(freeSpace, length);
                System.arraycopy(data, offset, buffer, this.index, toCopy);
                offset += toCopy;
                this.index += toCopy;
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
     * Return the number of bytes stored in this {@link ReusableByteBuffer}.
     */
    public int size() {
        return (this.alreadyBufferedSize + this.index);
    }


    /**
     * Reset the contents of this {@link ReusableByteBuffer}.
     * <p>All currently accumulated output in the output stream is discarded.
     * The output stream can be used again.
     */
    public void reset() {
        // Clear allocated buffers but keep the first one
        byte[] initialBuffer = this.buffers.get(0);
        this.buffers.clear();
        this.buffers.add(initialBuffer);

        //this.nextBlockSize = this.initialBlockSize;
        this.closed = false;
        this.index = 0;
        this.alreadyBufferedSize = 0;
    }


    /**
     * Write the buffers content to the given OutputStream.
     *
     * @param out the OutputStream to write to
     */
    public void writeTo(OutputStream out) throws IOException {
        Iterator<byte[]> it = this.buffers.iterator();
        while (it.hasNext()) {
            byte[] buffer = it.next();
            if (it.hasNext()) {
                out.write(buffer, 0, buffer.length);
            } else {
                out.write(buffer, 0, this.index);
            }
        }
    }


    /**
     * Creates a newly allocated byte array.
     * <p>Its size is the current
     * size of this output stream and the valid contents of the buffer
     * have been copied into it.</p>
     *
     * @return the current contents of this output stream, as a byte array.
     * @see #size()
     * @see #toByteArrayUnsafe()
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
                System.arraycopy(buffer, 0, result, offset, this.index);
            }
        }

        return result;
    }


    /**
     * Allocate a new chunk if needed
     */
    private void growIfNeeded() {
        if (getTailBuffer().length == this.index) {
            this.alreadyBufferedSize += this.index;
            this.buffers.add(new byte[this.index * 2]); // block size doubles each time
            this.index = 0;
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
