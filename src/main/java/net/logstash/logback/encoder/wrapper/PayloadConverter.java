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

import java.io.IOException;

/**
 * Converts encoded payload (with prefix and suffix).
 * Can be used to convert plain bytes to a given format.
 */
public interface PayloadConverter {
    default void start() { }
    default void stop() { }
    default boolean isStarted() {
        return true;
    }

    byte[] convert(byte[] encoded) throws IOException;
}
