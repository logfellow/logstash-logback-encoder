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

import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * @author brenuart
 *
 */
public class ThreadLocalReusableByteBufferTest {

    /*
     * Assert ReusableByteBuffer are properly recycled when released
     */
    @Test
    public void testBufferRecycled() throws IOException {
        ThreadLocalReusableByteBuffer pool = new ThreadLocalReusableByteBuffer(1024);

        // Acquire a buffer and write some content to it
        ReusableByteBuffer buffer = pool.acquire();
        buffer.write("hello".getBytes());
        assertThat(buffer.size()).isNotZero();

        // Release the buffer
        pool.release();

        // Ask again for a buffer - the previous should be returned and have a size()==0
        ReusableByteBuffer secondBuffer = pool.acquire();
        assertThat(secondBuffer).isSameAs(buffer);
        assertThat(secondBuffer.size()).isZero();
    }
}
