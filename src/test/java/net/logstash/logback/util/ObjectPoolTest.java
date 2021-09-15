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
package net.logstash.logback.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.logstash.logback.util.ObjectPool.Lifecycle;

import org.junit.jupiter.api.Test;

/**
 * @author brenuart
 *
 */
public class ObjectPoolTest {

    private ObjectPool<PooledObject> pool = new ObjectPool<>(this::createInstance);

    
    @Test
    public void testReuse() {
        PooledObject obj1 = pool.acquire();
        assertThat(obj1).isNotNull();
        
        // Acquire a second instance and make sure not same as first
        PooledObject obj2 = pool.acquire();
        assertThat(obj2).isNotNull();
        assertThat(obj1).isNotSameAs(obj2);
        
        // Release second and re-acquire - should be the same
        pool.release(obj2);
        PooledObject obj3 = pool.acquire();
        assertThat(obj3).isSameAs(obj2);
    }
    
    
    /*
     * Assert Lifecyle#recycle() is invoked when instance is returned to the pool
     */
    @Test
    public void testRecycle() {
        PooledObject obj1 = pool.acquire();
        pool.release(obj1);
        
        verify(obj1, times(1)).recycle();
    }
    
    
    /*
     * Assert instance is disposed and not returned to the pool when Lifecycle#recycle()
     * returns false
     */
    @Test
    public void testNotRecyclable() {
        PooledObject obj1 = pool.acquire();
        when(obj1.recycle()).thenReturn(false);
        
        pool.release(obj1);
        
        verify(obj1, times(1)).recycle();
        verify(obj1, times(1)).dispose();
        
        assertThat(pool.acquire()).isNotSameAs(obj1);
    }
    
    
    /*
     * Assert pooled instance are disposed when calling #clear()
     */
    @Test
    public void testClear() {
        PooledObject obj1 = pool.acquire();
        pool.release(obj1);
        assertThat(pool.size()).isEqualTo(1);
        
        pool.clear();
        verify(obj1, times(1)).dispose();
        
        assertThat(pool.size()).isZero();
    }
    
    
    /*
     * Releasing a "null" instance should not throw any exception
     */
    @Test
    public void testReleaseNull() {
        assertThatCode(() -> pool.release(null)).doesNotThrowAnyException();
    }
    
    
    /*
     * NullPointer exception thrown if factory returns null
     */
    @Test
    public void testFactoryReturnsNull() {
        pool = new ObjectPool<PooledObject>(() -> null);
        assertThatThrownBy(() -> pool.acquire()).isInstanceOf(NullPointerException.class);
    }
    
    
    /*
     * Exception thrown by the factory is propagated to ObjectPool#acquire()
     */
    @Test
    public void testFactoryThrowsException() {
        RuntimeException e = new RuntimeException();
        
        pool = new ObjectPool<PooledObject>(() -> {
            throw e;
        });

        assertThatThrownBy(() -> pool.acquire()).isSameAs(e);
    }
    
    
    private PooledObject createInstance() {
        return spy(new PooledObject());
    }
    
    public static class PooledObject implements ObjectPool.Lifecycle {
        @Override
        public boolean recycle() {
            return Lifecycle.super.recycle();
        }
        
        @Override
        public void dispose() {
            Lifecycle.super.dispose();
        }
    }
}
