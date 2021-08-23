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

import java.math.BigInteger;

public class ByteUtil {

    private static final int INT_SIZE_IN_BYTES = 4;

    private ByteUtil() {
        // util
    }

    public static byte[] intToBytes(int value) {
        byte[] result = new byte[INT_SIZE_IN_BYTES];
        byte[] unpadded = BigInteger.valueOf(value).toByteArray();
        for (int i = unpadded.length - 1, j = result.length - 1; i >= 0 && j >= 0; i--, j--) {
            result[j] = unpadded[i];
        }
        return result;
    }

    public static int bytesToInt(byte[] value) {
        return new BigInteger(1, value).intValueExact();
    }
}
