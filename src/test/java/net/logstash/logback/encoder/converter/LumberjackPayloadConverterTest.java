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
package net.logstash.logback.encoder.converter;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LumberjackPayloadConverterTest {
    private final LumberjackPayloadConverter payloadConverter = new LumberjackPayloadConverter();

    @Test
    void testPayloadProperlyConverted() {
        String payload = "{\"message\":\"Log message\"}";
        byte[] encoded = payload.getBytes(StandardCharsets.UTF_8);

        byte[] wrapped = assertDoesNotThrow(() -> payloadConverter.convert(encoded));

        ByteBuffer buffer = ByteBuffer.wrap(wrapped);

        // version
        assertEquals('2', buffer.get());
        // payload type
        assertEquals('J', buffer.get());
        // sequence number
        assertEquals(1, buffer.getInt());
        // payload length
        int payloadLength = buffer.getInt();
        assertEquals(encoded.length, payloadLength);
        // actual payload length
        int position = buffer.position();
        int leftOverByteLength = wrapped.length - position;
        assertEquals(encoded.length, leftOverByteLength);
        // payload
        byte[] encodedPayload = new byte[leftOverByteLength];
        System.arraycopy(buffer.array(), position, encodedPayload, 0, encodedPayload.length);
        assertEquals(payload, new String(encodedPayload));
    }

    @Test
    void testSequenceNumberIncremented() {
        String payload = "{\"message\":\"Log message\"}";
        byte[] encoded = payload.getBytes(StandardCharsets.UTF_8);

        for (int i = 1; i <= 5; i++) {
            byte[] wrapped = assertDoesNotThrow(() -> payloadConverter.convert(encoded));
            assertSequenceNumberIs(wrapped, i);
        }
    }

    private void assertSequenceNumberIs(byte[] wrapped, int number) {
        ByteBuffer buffer = ByteBuffer.wrap(wrapped);
        buffer.get();
        buffer.get();
        assertEquals(number, buffer.getInt());
    }
}