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
package net.logstash.logback.composite.accessevent;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IncludeExcludeHeaderFilterTest {
    
    private IncludeExcludeHeaderFilter filter = new IncludeExcludeHeaderFilter();
    
    @Test
    public void testEmpty() {
        Assertions.assertTrue(filter.includeHeader("HeaderA", "1"));
        Assertions.assertTrue(filter.includeHeader("Headera", "2"));
        Assertions.assertTrue(filter.includeHeader("HeaderB", "3"));
        Assertions.assertTrue(filter.includeHeader("Headerb", "4"));
    }
    @Test
    public void testIncludeOnly() {
        filter.addInclude("headera");
        Assertions.assertTrue(filter.includeHeader("HeaderA", "1"));
        Assertions.assertTrue(filter.includeHeader("Headera", "2"));
        Assertions.assertFalse(filter.includeHeader("HeaderB", "3"));
        Assertions.assertFalse(filter.includeHeader("Headerb", "3"));
    }

    @Test
    public void testExcludeOnly() {
        filter.addExclude("headera");
        Assertions.assertFalse(filter.includeHeader("HeaderA", "1"));
        Assertions.assertFalse(filter.includeHeader("Headera", "2"));
        Assertions.assertTrue(filter.includeHeader("HeaderB", "3"));
        Assertions.assertTrue(filter.includeHeader("Headerb", "4"));
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
