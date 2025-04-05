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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
        String str = "a,b";
        assertThat(commaDelimitedListToStringArray(str))
            .isEqualTo(delimitedListToStringArray(str, ","));
    }
    

    
    @ParameterizedTest
    @ValueSource(strings = {",", "--"})
    public void delimitedListToStringArrayTest(String delimiter) {
        // Null input -> empty array
        validate(delimiter, null, new String[] {});
        
        // Empty string -> empty array
        validate(delimiter, "", new String[] {});
        
        // Blank string -> empty array
        validate(delimiter, " ", new String[] {});
        
        
        // Single char delimiter
        validate(delimiter, "a,b", new String[] {"a", "b"});
        
        // Consecutive delimiters
        validate(delimiter, "a,,b", new String[] {"a", "b"});
        
        // Delimiter at end
        validate(delimiter, "a,", new String[] {"a"});
        
        // Trim individual values
        validate(delimiter, " a, b\t , c \n", new String[] {"a", "b", "c"});
        
        // Escape
        validate(delimiter, "a\\,b,c", new String[] {"a,b", "c"});
        validate(delimiter, "a,b\\,c", new String[] {"a", "b,c"});
        
        validate(delimiter, "a\\,\\,b\\,,c",  new String[] {"a,,b,", "c"});
        validate(delimiter, "a\\ ,\\,b\\,,c", new String[] {"a\\", ",b,", "c"});
    }
    
    private static void validate(String delimiter, String delimitedList, String[] expected) {
        String str = (delimiter == null || delimitedList == null) ? delimitedList : delimitedList.replace(",", delimiter);
        
        String[] e = new String[expected.length];
        if (delimiter == null) {
            e = expected;
        }
        else {
            for (int i = 0; i < expected.length; i++) {
                e[i] = expected[i].replace(",", delimiter);
            }
        }
        assertThat(delimitedListToStringArray(str, delimiter)).isEqualTo(e);
    }
    
    
    @Test
    public void delimitedListToStringArrayTest_edgecases() {
        
        // Null input -> empty array
        assertThat(delimitedListToStringArray(null, "-")).isEqualTo(new String[] {});
        
        // Empty input -> empty array
        assertThat(delimitedListToStringArray("", "-")).isEqualTo(new String[] {});
        
        // Blank input -> empty array
        assertThat(delimitedListToStringArray(" ", "-")).isEqualTo(new String[] {});
        
        
        // Null delimiter
        assertThat(delimitedListToStringArray("a,b", null)).isEqualTo(new String[] {"a,b"});

        // Empty delimiter
        assertThat(delimitedListToStringArray("a,b", "")).isEqualTo(new String[] {"a", ",", "b"});
    }
}
