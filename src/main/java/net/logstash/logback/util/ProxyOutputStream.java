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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/**
 * A Proxy stream which acts as expected, that is it passes the method calls on
 * to the proxied stream and doesn't change which methods are being called (unlike
 * JDK {@link FilterOutputStream}).
 * 
 * <p>Note: This class is for internal use only and subject to backward incompatible change
 * at any time.
 * 
 * @author brenuart
 */
public class ProxyOutputStream extends OutputStream {

    protected OutputStream delegate;
    
    /**
     * Constructs a new ProxyOutputStream.
     *
     * @param delegate the OutputStream to delegate to
     */
    public ProxyOutputStream(final OutputStream delegate) {
        this.delegate = delegate;
    }

    /**
     * Invokes the delegate's <code>write(int)</code> method.
     * 
     * @param b the byte to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(final int b) throws IOException {
        try {
            assertStreamConnected().write(b);
        } catch (final IOException e) {
            handleIOException(e);
        }
    }

    /**
     * Invokes the delegate's <code>write(byte[])</code> method.
     * 
     * @param b the bytes to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(final byte[] b) throws IOException {
        try {
            assertStreamConnected().write(b);
        } catch (final IOException e) {
            handleIOException(e);
        }
    }

    /**
     * Invokes the delegate's <code>write(byte[])</code> method.
     * 
     * @param b the bytes to write
     * @param off  The start offset
     * @param len The number of bytes to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        try {
            assertStreamConnected().write(b, off, len);
        } catch (final IOException e) {
            handleIOException(e);
        }
    }

    /**
     * Invokes the delegate's <code>flush()</code> method.
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void flush() throws IOException {
        try {
            assertStreamConnected().flush();
        } catch (final IOException e) {
            handleIOException(e);
        }
    }

    /**
     * Invokes the delegate's <code>close()</code> method.
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        try {
            assertStreamConnected().close();
        } catch (final IOException e) {
            handleIOException(e);
        }
    }
    
    /**
     * Handle any IOExceptions thrown.
     * <p>
     * This method provides a point to implement custom exception handling. The
     * default behavior is to re-throw the exception.
     * 
     * @param e The IOException thrown
     * @throws IOException if an I/O error occurs
     */
    protected void handleIOException(final IOException e) throws IOException {
        throw e;
    }
    
    /**
     * Get the underlying OutputStream and assert it is connected.
     * 
     * @return the underlying OutputStream
     * @throws IOException thrown when the stream is not connected
     */
    protected OutputStream assertStreamConnected() throws IOException {
        if (this.delegate == null) {
            throw new IOException("Stream is not connected");
        }
        return this.delegate;
    }
}
