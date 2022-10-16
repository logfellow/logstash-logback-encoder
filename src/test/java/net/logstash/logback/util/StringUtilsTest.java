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
import static net.logstash.logback.util.StringUtils.isBlank;
import static net.logstash.logback.util.StringUtils.isEmpty;
import static net.logstash.logback.util.StringUtils.length;
import static net.logstash.logback.util.StringUtils.trim;
import static net.logstash.logback.util.StringUtils.trimToEmpty;
import static net.logstash.logback.util.StringUtils.trimToNull;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author brenuart
 *
 */
public class StringUtilsTest {

    @Test
    public void isBlankTest() {
        assertThat(isBlank(null)).isTrue();
        assertThat(isBlank("")).isTrue();
        assertThat(isBlank(" ")).isTrue();
        assertThat(isBlank("\t")).isTrue();
        assertThat(isBlank("\n")).isTrue();
        assertThat(isBlank("\r")).isTrue();
        assertThat(isBlank(" \t\n\r")).isTrue();
        
        assertThat(isBlank(" a ")).isFalse();
    }
    
    
    @Test
    public void isEmptyTest() {
        assertThat(isEmpty(null)).isTrue();
        assertThat(isEmpty("")).isTrue();
        assertThat(isEmpty(" ")).isFalse();
        assertThat(isEmpty("\t")).isFalse();
        assertThat(isEmpty("\n")).isFalse();
        
        assertThat(isEmpty(" a ")).isFalse();
    }
    
    
    @Test
    public void trimTest() {
        assertThat(trim(null)).isNull();
        assertThat(trim("")).isEqualTo("");
        assertThat(trim(" ")).isEqualTo("");
        assertThat(trim(" foo ")).isEqualTo("foo");
    }

    
    @Test
    public void trimToEmptyTest() {
        assertThat(trimToEmpty(null)).isEqualTo("");
        assertThat(trimToEmpty("")).isEqualTo("");
        assertThat(trimToEmpty(" ")).isEqualTo("");
        assertThat(trimToEmpty(" foo ")).isEqualTo("foo");
    }

    
    @Test
    public void trimToNullTest() {
        assertThat(trimToNull(null)).isNull();
        assertThat(trimToNull("")).isNull();
        assertThat(trimToNull(" ")).isNull();
        assertThat(trimToNull(" foo ")).isEqualTo("foo");
    }
    
    
    @Test
    public void lengthTest() {
        assertThat(length(null)).isZero();
        assertThat(length("")).isZero();
        assertThat(length(" ")).isOne();
    }
    
    
    @Test
    public void commaDelimitedListToStringArrayTest() {
        // Null input -> empty array
        assertThat(commaDelimitedListToStringArray(null)).isEqualTo(new String[] {});
        
        // Empty string -> empty array
        assertThat(commaDelimitedListToStringArray("")).isEqualTo(new String[] {});
        
        // Blank string -> empty array
        assertThat(commaDelimitedListToStringArray(" ")).isEqualTo(new String[] {});
        
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
