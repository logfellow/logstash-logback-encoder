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
package net.logstash.logback.appender;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.logstash.logback.CachingAbbreviator;

import ch.qos.logback.classic.pattern.Abbreviator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CachingAbbreviatorTest {
    
    @Mock
    private Abbreviator delegate;
    
    @Test
    public void test() {
        when(delegate.abbreviate("full")).thenReturn("abbreviation");
        
        CachingAbbreviator abbreviator = new CachingAbbreviator(delegate);
        
        Assertions.assertEquals("abbreviation", abbreviator.abbreviate("full"));
        Assertions.assertEquals("abbreviation", abbreviator.abbreviate("full"));
        
        verify(delegate, times(1)).abbreviate("full");
        
        abbreviator.clear();
        
        Assertions.assertEquals("abbreviation", abbreviator.abbreviate("full"));
        
        verify(delegate, times(2)).abbreviate("full");
    }

}
