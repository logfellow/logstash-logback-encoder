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
package net.logstash.logback.mask;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import tools.jackson.core.TokenStreamContext;

/**
 * Masks values of specific JSON field names within a JSON stream.
 */
public class FieldNameBasedFieldMasker implements FieldMasker {

    private final Set<String> fieldNamesToMask;
    private final Object mask;

    /**
     * @param fieldNamesToMask the names of fields in the JSON stream to mask
     * @param mask the value to write for fields in the fieldNamesToMask
     */
    public FieldNameBasedFieldMasker(Set<String> fieldNamesToMask, Object mask) {
        this.fieldNamesToMask = new HashSet<>(Objects.requireNonNull(fieldNamesToMask, "fieldNamesToMask must not be null"));
        this.mask = Objects.requireNonNull(mask, "mask must not be null");
    }

    @Override
    public Object mask(TokenStreamContext context) {
        return context.hasCurrentName() && fieldNamesToMask.contains(context.currentName())
                ? mask
                : null;
    }

}
