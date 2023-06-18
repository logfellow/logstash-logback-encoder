/*
 * Copyright 2013-2023 the original author or authors.
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
package net.logstash.logback.mask;

import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.node.NullNode;

/**
 * Masks JSON fields within a JSON stream.
 *
 * Invoked by {@link MaskingJsonGenerator} after a field name is written
 * (but before the field's value is known)
 * to determine if the field's value should be masked.
 *
 * <h2>Comparison with {@link ValueMasker}</h2>
 *
 * <ul>
 *     <li>{@link FieldMasker}s are more efficient than {@link ValueMasker}s, since {@link FieldMasker}s do not inspect values.</li>
 *     <li>{@link FieldMasker}s can mask any type of JSON field (string, number, boolean, array, object), whereas a {@link ValueMasker} can only mask string and number values.</li>
 *     <li>{@link ValueMasker}s can mask element values within an array.  {@link FieldMasker}s can only mask field values.
 * </ul>
 */
@FunctionalInterface
public interface FieldMasker {

    /**
     * If the field at the JSON stream context's current path should be masked,
     * then returns the masked value to write as the field's value.
     * The {@link MaskingJsonGenerator} will write the returned masked value
     * as the field's value (instead of the original field's value).
     *
     * <p>If the JSON stream context's current path should NOT be masked, returns null.</p>
     *
     * @param context the current JSON stream context, which can be used to determine the path within the JSON output.
     * @return A non-null masked value to write if the current field should be masked.
     *         Otherwise null if the current field should not be masked.
     *         To write a JSON null value as the masked value, return {@link NullNode#instance}.
     *         To write {@value MaskingJsonGenerator#MASK}, the return {@link MaskingJsonGenerator#MASK MaskingJsonGenerator.MASK}
     */
    Object mask(JsonStreamContext context);
}
