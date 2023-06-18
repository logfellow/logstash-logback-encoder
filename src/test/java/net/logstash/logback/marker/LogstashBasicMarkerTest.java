/*
 * Copyright 2013-2023 the original author or authors.
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
package net.logstash.logback.marker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.slf4j.Marker;

/**
 * @author brenuart
 *
 */
public class LogstashBasicMarkerTest {

    @Test
    public void name() {
        assertThat(new LogstashBasicMarker("m1").getName()).isEqualTo("m1");
    }
    
    @Test
    public void name_notNull() {
        assertThatThrownBy(() -> new LogstashBasicMarker(null)).isInstanceOf(NullPointerException.class);
    }
    
    @Test
    public void equalsAndHashCode() {
        LogstashBasicMarker m1 = new LogstashBasicMarker("same");
        LogstashBasicMarker m2 = new LogstashBasicMarker("same");
        LogstashBasicMarker m3 = new LogstashBasicMarker("another");
        
        assertThat(m1).isEqualTo(m2)
                      .hasSameHashCodeAs(m2)
                      .isNotEqualTo(m3)
                      .doesNotHaveSameHashCodeAs(m3);
        
        
        Set<Marker> set = new HashSet<>();
        set.add(m1);
        set.add(m2);
        set.add(m3);
        
        assertThat(set).containsExactly(m1, m3);
    }
    
    
    @Test
    public void contains() {
        LogstashBasicMarker m1 = new LogstashBasicMarker("m1");
        LogstashBasicMarker m2 = new LogstashBasicMarker("m2");
        
        // contains itself
        assertThat(m1.contains(m1)).isTrue();
        assertThat(m1.contains("m1")).isTrue();
        
        
        // contains when empty
        assertThat(m1.contains(m2)).isFalse();
        assertThat(m1.contains("m2")).isFalse();
        
        
        // contains after adding a ref
        m1.add(m2);
        assertThat(m1.contains(m2)).isTrue();
        assertThat(m1.contains("m2")).isTrue();
    }
    
    
    @Test
    public void contains_null() {
        LogstashBasicMarker m1 = new LogstashBasicMarker("m1");
        assertThat(m1.contains((Marker) null)).isFalse();
        assertThat(m1.contains((String) null)).isFalse();
    }
    
    
    @Test
    public void add_self() {
        LogstashBasicMarker m1 = new LogstashBasicMarker("m1");

        m1.add(m1);
        assertThat(m1.hasReferences()).isFalse();
    }
    
    
    @Test
    public void add_duplicate() {
        LogstashBasicMarker m1 = new LogstashBasicMarker("m1");
        LogstashBasicMarker m2 = new LogstashBasicMarker("m2");
        
        m1.add(m2);
        m1.add(m2);
        assertThat(m1.iterator()).toIterable().containsExactly(m2);
    }
    
    
    @Test
    public void add_recursion() {
        LogstashBasicMarker m1 = new LogstashBasicMarker("m1");
        LogstashBasicMarker m2 = new LogstashBasicMarker("m2");
        m2.add(m1);
        
        m1.add(m2);
        assertThat(m1.hasReferences()).isFalse();

        
        // create a cycle
        LogstashBasicMarker m3 = new LogstashBasicMarker("m3");
        m3.add(m2);
        m1.add(m3);
        
        assertThat(m1.hasReferences()).isFalse();
    }

    
    @Test
    public void iterator() {
        LogstashBasicMarker m1 = new LogstashBasicMarker("m1");
        LogstashBasicMarker m2 = new LogstashBasicMarker("m2");
        LogstashBasicMarker m3 = new LogstashBasicMarker("m3");
        
        m1.add(m2);
        m1.add(m3);
        assertThat(m1.iterator()).toIterable().containsExactly(m2, m3);
    }
    
    
    @Test
    public void remove() {
        LogstashBasicMarker m1 = new LogstashBasicMarker("m1");
        LogstashBasicMarker m2 = new LogstashBasicMarker("m2");
        LogstashBasicMarker m3 = new LogstashBasicMarker("m3");
        
        
        // remove when empty
        assertThat(m1.remove(m2)).isFalse();
        
        // remove existing ref
        m1.add(m2);
        assertThat(m1.remove(m2)).isTrue();
        assertThat(m1.iterator()).toIterable().isEmpty();
        
        // remove unexisting ref
        assertThat(m1.remove(m3)).isFalse();
    }
}
