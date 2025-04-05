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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * @author brenuart
 *
 */
@SuppressWarnings("resource")
public class ReusableBufferTest {

    private final byte[] helloBytes = "0123456789".getBytes(StandardCharsets.UTF_8);

    
    @Test
    public void invalidInitialSize() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new ReusableByteBuffer(0));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new ReusableByteBuffer(-1));
    }
    
    
    @Test
    public void size() throws IOException {
        ReusableByteBuffer buffer = new ReusableByteBuffer();
        
        buffer.write(helloBytes);
        assertThat(buffer.size()).isEqualTo(helloBytes.length);
    }
    
    
    @Test
    public void autoGrow() throws IOException {
        ReusableByteBuffer buffer = new ReusableByteBuffer(1);
        
        for (int i = 0; i < 10; i++) {
            buffer.write(1);
        }
        
        assertThat(buffer.size()).isEqualTo(10);
        assertThat(buffer.toByteArray()).containsExactly(new byte[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1});
    }
    
    
    @Test
    public void reset() throws IOException {
        ReusableByteBuffer buffer = new ReusableByteBuffer();

        buffer.write(helloBytes);
        assertThat(buffer.toByteArray()).containsExactly(helloBytes);

        buffer.reset();
        assertThat(buffer.size()).isZero();
        
        buffer.write(helloBytes);
        assertThat(buffer.toByteArray()).containsExactly(helloBytes);
    }
    
    
    @Test
    public void close() {
        ReusableByteBuffer buffer = new ReusableByteBuffer();

        buffer.close();
        assertThatIOException().isThrownBy(() -> buffer.write(this.helloBytes));
        
        buffer.reset();
        assertThatCode(() -> buffer.write(this.helloBytes)).doesNotThrowAnyException();
    }
    
    
    @Test
    public void writeTo() throws IOException {
        ReusableByteBuffer buffer = new ReusableByteBuffer();

        buffer.write(this.helloBytes);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        buffer.writeTo(baos);
        assertThat(baos.toByteArray()).isEqualTo(this.helloBytes);
    }
}
