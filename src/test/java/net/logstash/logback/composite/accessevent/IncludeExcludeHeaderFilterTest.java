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
package net.logstash.logback.composite.accessevent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

public class IncludeExcludeHeaderFilterTest {
    
    private IncludeExcludeHeaderFilter filter = new IncludeExcludeHeaderFilter();
    
    @Test
    public void testEmpty() {
        assertThat(filter.includeHeader("HeaderA", "1")).isTrue();
        assertThat(filter.includeHeader("Headera", "2")).isTrue();
        assertThat(filter.includeHeader("HeaderB", "3")).isTrue();
        assertThat(filter.includeHeader("Headerb", "4")).isTrue();
    }
    @Test
    public void testIncludeOnly() {
        filter.addInclude("headera");
        assertThat(filter.includeHeader("HeaderA", "1")).isTrue();
        assertThat(filter.includeHeader("Headera", "2")).isTrue();
        assertThat(filter.includeHeader("HeaderB", "3")).isFalse();
        assertThat(filter.includeHeader("Headerb", "3")).isFalse();
    }

    @Test
    public void testExcludeOnly() {
        filter.addExclude("headera");
        assertThat(filter.includeHeader("HeaderA", "1")).isFalse();
        assertThat(filter.includeHeader("Headera", "2")).isFalse();
        assertThat(filter.includeHeader("HeaderB", "3")).isTrue();
        assertThat(filter.includeHeader("Headerb", "4")).isTrue();
    }

    @Test
    public void testBoth() {
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
            filter.addInclude("headera");
            filter.addExclude("headerb");
            filter.includeHeader("HeaderA", "1");
        });
    }

}
