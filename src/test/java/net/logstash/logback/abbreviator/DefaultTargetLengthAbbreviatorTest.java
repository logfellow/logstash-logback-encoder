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
package net.logstash.logback.abbreviator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * @author brenuart
 *
 */
public class DefaultTargetLengthAbbreviatorTest {

    @Test
    public void test() {
        String txt = "com.foo.Bar";
        
        validate(-1, txt, txt);
        validate(0,  txt, "Bar");
        validate(3,  txt, "c.f.Bar");
        validate(10, txt, "c.foo.Bar");
    }
    
    @Test
    public void testNotStarted() {
        DefaultTargetLengthAbbreviator abbreviator = new DefaultTargetLengthAbbreviator();

        assertThatThrownBy(() -> abbreviator.abbreviate("foo")).isInstanceOf(IllegalStateException.class);
    }
    
    private void validate(int targetLength, String txt, String expected) {
        DefaultTargetLengthAbbreviator abbreviator = new DefaultTargetLengthAbbreviator();
        abbreviator.setTargetLength(targetLength);
        abbreviator.start();
        
        assertThat(abbreviator.abbreviate(txt)).isEqualTo(expected);
    }
}
