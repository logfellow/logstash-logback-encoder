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
package net.logstash.logback.util;

import static net.logstash.logback.util.StringUtils.commaDelimitedListToStringArray;
import static net.logstash.logback.util.StringUtils.delimitedListToStringArray;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author brenuart
 *
 */
public class StringUtilsTest {

    @Test
    public void commaDelimitedListToStringArrayTest() {
        // Null input -> empty array
        assertThat(commaDelimitedListToStringArray(null)).isEqualTo(new String[] {});
        
        // Empty string -> empty array
        assertThat(commaDelimitedListToStringArray("")).isEqualTo(new String[] {});
        
        // Trim individual values
        assertThat(commaDelimitedListToStringArray("a,b,c")).isEqualTo(new String[] {"a", "b", "c"});
        assertThat(commaDelimitedListToStringArray("a, b , c \n")).isEqualTo(new String[] {"a", "b", "c"});
        
        // Consecutive delimiters
        assertThat(commaDelimitedListToStringArray("a,,b")).isEqualTo(new String[] {"a", "b"});

        // Delimiter at end
        assertThat(commaDelimitedListToStringArray("a,,")).isEqualTo(new String[] {"a"});
    }
    
    
    @Test
    public void delimitedListToStringArrayTest() {
        // Single char delimiter
        assertThat(delimitedListToStringArray("a|b", "|")).isEqualTo(new String[] {"a", "b"});
        
        // Two char delimiter
        assertThat(delimitedListToStringArray("a|b", "||")).isEqualTo(new String[] {"a|b"});
        assertThat(delimitedListToStringArray("a||b", "||")).isEqualTo(new String[] {"a", "b"});
        
        // Empty delimiter
        assertThat(delimitedListToStringArray("a,b", "")).isEqualTo(new String[] {"a", ",", "b"});
        
        // Null delimiter
        assertThat(delimitedListToStringArray("a,b", null)).isEqualTo(new String[] {"a,b"});
    }
}
