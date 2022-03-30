/*
 * Copyright 2013-2021 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonStreamContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AnnotatedFieldMaskerTest {

    private static final String SECRET_DATE = "secretDate";
    private static final String STREET_ADDRESS = "streetAddress";
    private static final String MASKED = "REDACTED";
    private static final String NONE_OF_YOUR_BUSINESS = "NONE OF YOUR BUSINESS";

    private JsonStreamContext streamContext;

    private AnnotatedFieldMasker candidate;

    @BeforeEach
    void setup() {
        streamContext = Mockito.mock(JsonStreamContext.class);
        candidate = new AnnotatedFieldMasker();
    }

    @Test
    void assertMasksFound() {
        assertEquals(2, candidate.fieldMasks.size());
        assertTrue(candidate.fieldMasks.containsKey(SECRET_DATE));
        assertTrue(candidate.fieldMasks.containsKey(STREET_ADDRESS));

        assertEquals(NONE_OF_YOUR_BUSINESS, candidate.fieldMasks.get(SECRET_DATE));
        assertEquals(MASKED, candidate.fieldMasks.get(STREET_ADDRESS));
    }

    @Test
    void assertNoMaskNoName() {
        when(streamContext.hasCurrentName()).thenReturn(Boolean.FALSE);
        assertNull(candidate.mask(streamContext));
    }

    @Test
    void assertNoMaskCity() {
        when(streamContext.hasCurrentName()).thenReturn(Boolean.TRUE);
        when(streamContext.getCurrentName()).thenReturn("city");
        assertNull(candidate.mask(streamContext));
    }

    @Test
    void assertDefaultMaskStreetAddress() {
        when(streamContext.hasCurrentName()).thenReturn(Boolean.TRUE);
        when(streamContext.getCurrentName()).thenReturn(STREET_ADDRESS);
        assertEquals(MASKED, candidate.mask(streamContext));
    }

    @Test
    void assertMaskSecretDate() {
        when(streamContext.hasCurrentName()).thenReturn(Boolean.TRUE);
        when(streamContext.getCurrentName()).thenReturn(SECRET_DATE);
        assertEquals(NONE_OF_YOUR_BUSINESS, candidate.mask(streamContext));
    }

    @Test
    void assertNoMaskBirthDate() {
        when(streamContext.hasCurrentName()).thenReturn(Boolean.TRUE);
        when(streamContext.getCurrentName()).thenReturn("birthDate");
        assertNull(candidate.mask(streamContext));
    }
}
