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
package net.logstash.logback.decorate.mask;

import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.node.NullNode;

/**
 * Masks JSON string and number values within a JSON stream.
 *
 * <p>Invoked by {@link MaskingJsonGenerator} before a number or string value is written
 * to determine if the value should be masked.</p>
 *
 * <h3>Comparison with {@link FieldMasker}</h3>
 *
 * <ul>
 *     <li>{@link FieldMasker}s are more efficient than {@link ValueMasker}s, since {@link FieldMasker}s do not inspect values.</li>
 *     <li>{@link FieldMasker}s can mask any type of JSON field (string, number, boolean, array, object), whereas a {@link ValueMasker} can only mask string and number values.</li>
 *     <li>{@link ValueMasker}s can mask element values within an array.  {@link FieldMasker}s can only mask field values.
 * </ul>
 */
@FunctionalInterface
public interface ValueMasker {

    /**
     * If the given value at the JSON stream context's current path should be masked,
     * then returns the masked value to write as the value..
     * The {@link MaskingJsonGenerator} will write the returned masked value
     * as the value (instead of the original value).
     *
     * <p>If the given value at the JSON stream context's current path should NOT be masked, returns null.</p>
     *
     * @param context the current JSON stream context, which can be used to determine the path within the JSON output.
     *                (could be at a field value path or an array element value path)
     * @param value the number or string scalar value to potentially mask (could be a field value or an array element value).
     * @return A non-null masked value to write if given value at the JSON stream context's current path should be masked.
     *         Otherwise null if the given value at the JSON stream context's current path should NOT be masked.
     *         To write a JSON null value as the masked value, return {@link NullNode#instance}.
     *         To write {@value net.logstash.logback.decorate.mask.MaskingJsonGenerator#MASK}, the return {@link MaskingJsonGenerator#MASK MaskingJsonGenerator.MASK}
     */
    Object mask(JsonStreamContext context, Object value);
}
