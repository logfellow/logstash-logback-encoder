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
package net.logstash.logback.encoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * Util to wrap encoder output that should be compatible with a Beats input.
 * Can be used with Logstash input or Graylog's Beats input.
 *
 * See <a href="https://github.com/logstash-plugins/logstash-input-beats/blob/master/PROTOCOL.md">Logstash's documentation</a>
 * to read more about this protocol.
 */
public class BeatsPayloadWrapper {

    private static final int INT_SIZE_IN_BYTES = 4;

    private static final byte PROTOCOL_VERSION = '2';
    private static final byte PAYLOAD_JSON_TYPE = 'J';

    private BeatsPayloadWrapper() {
        // util
    }

    public static byte[] wrapAsBeatsOrReturnEmpty(byte[] encoded, AtomicInteger counter, BiConsumer<String, IOException> exceptionConsumer) {
        try {
            return wrapAsBeats(encoded, counter);
        } catch (IOException e) {
            exceptionConsumer.accept("Cannot wrap encoded event as Beats", e);
        }
        return new byte[0];
    }

    public static byte[] wrapAsBeats(byte[] encoded, AtomicInteger counter) throws IOException {
        byte[] payloadLength = intToBytes(encoded.length);
        byte[] sequenceNumber = intToBytes(counter.getAndIncrement());

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(
                2 + encoded.length + payloadLength.length + sequenceNumber.length
        )) {
            outputStream.write(PROTOCOL_VERSION);
            outputStream.write(PAYLOAD_JSON_TYPE);
            outputStream.write(sequenceNumber);
            outputStream.write(payloadLength);
            outputStream.write(encoded);

            return outputStream.toByteArray();
        }
    }

    private static byte[] intToBytes(int value) {
        byte[] result = new byte[INT_SIZE_IN_BYTES];
        byte[] unpadded = BigInteger.valueOf(value).toByteArray();
        for (int i = unpadded.length - 1, j = result.length - 1; i >= 0 && j >= 0; i++, j++) {
            result[j] = unpadded[i];
        }
        return result;
    }
}
