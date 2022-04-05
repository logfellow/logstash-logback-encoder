/*
 * Copyright 2013-2022 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.Test;

class RegexValueMaskerTest {

    @Test
    void regexMatchesFullString() {
        RegexValueMasker masker = new RegexValueMasker("^test$", "****");
        assertThat(masker.mask(null, "test")).isEqualTo("****");
        assertThat(masker.mask(null, "testtest")).isNull();
    }

    @Test
    void regexMatchesPartialString() {
        RegexValueMasker masker = new RegexValueMasker("test", "****");
        assertThat(masker.mask(null, "test")).isEqualTo("****");
        assertThat(masker.mask(null, "test-test")).isEqualTo("****-****");
        assertThat(masker.mask(null, "foo")).isNull();
    }

    @Test
    void nullMask() {
        RegexValueMasker masker = new RegexValueMasker("test", NullNode.instance);
        assertThat(masker.mask(null, "test")).isEqualTo(NullNode.instance);
        assertThat(masker.mask(null, "test-test")).isNull();
    }

    @Test
    void nonString() {
        RegexValueMasker masker = new RegexValueMasker("test", "****");
        assertThat(masker.mask(null, 1)).isNull();
    }
}
