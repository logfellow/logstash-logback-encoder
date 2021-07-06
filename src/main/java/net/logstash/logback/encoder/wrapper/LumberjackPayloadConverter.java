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
package net.logstash.logback.encoder.wrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Wraps payload so it should be compatible with a Beats input (Lumberjack protocol).
 * Can be used with Logstash input or Graylog's Beats input.
 *
 * See <a href="https://github.com/logstash-plugins/logstash-input-beats/blob/master/PROTOCOL.md">Logstash's documentation</a>
 * to read more about this protocol.
 */
public class LumberjackPayloadConverter implements PayloadConverter {

    private static final int INT_SIZE_IN_BYTES = 4;

    private static final byte PROTOCOL_VERSION = '2';
    private static final byte PAYLOAD_JSON_TYPE = 'J';

    private int counter;

    public LumberjackPayloadConverter() {
        this.counter = 0;
    }

    @Override
    public byte[] convert(byte[] encoded) throws IOException {
        byte[] payloadLength = intToBytes(encoded.length);
        byte[] sequenceNumber = intToBytes(counter++);

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
        for (int i = unpadded.length - 1, j = result.length - 1; i >= 0 && j >= 0; i--, j--) {
            result[j] = unpadded[i];
        }
        return result;
    }
}
