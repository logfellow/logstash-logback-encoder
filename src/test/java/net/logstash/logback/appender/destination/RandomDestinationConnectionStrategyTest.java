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
package net.logstash.logback.appender.destination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.function.UnaryOperator;

import ch.qos.logback.core.util.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RandomDestinationConnectionStrategyTest {
    
    @Mock
    private UnaryOperator<Integer> randomGenerator;

    private RandomDestinationConnectionStrategy strategy;
    
    
    @BeforeEach
    public void setup() {
        this.strategy = new RandomDestinationConnectionStrategy(randomGenerator);
    }
    
    @Test
    public void testNoConnectionTtl_success() {
        
        when(randomGenerator.apply(3))
            .thenReturn(0)
            .thenReturn(1);
        
        assertThat(strategy.selectNextDestinationIndex(0, 3)).isEqualTo(0);
        
        strategy.connectSuccess(0, 0, 3);
        
        assertThat(strategy.shouldReconnect(5000, 0, 3)).isEqualTo(false);
        
        assertThat(strategy.selectNextDestinationIndex(0, 3)).isEqualTo(1);
    }

    @Test
    public void testNoConnectionTtl_failed() {
        
        when(randomGenerator.apply(3))
            .thenReturn(0)
            .thenReturn(2)
            .thenReturn(1);
        
        assertThat(strategy.selectNextDestinationIndex(0, 3)).isEqualTo(0);
        
        strategy.connectFailed(0, 0, 3);
        
        assertThat(strategy.shouldReconnect(5000, 0, 3)).isEqualTo(false);
        
        assertThat(strategy.selectNextDestinationIndex(0, 3)).isEqualTo(2);
        
        strategy.connectFailed(0, 1, 3);
        assertThat(strategy.selectNextDestinationIndex(0, 3)).isEqualTo(1);
        
        strategy.connectFailed(0, 2, 3);
        assertThat(strategy.selectNextDestinationIndex(0, 3)).isEqualTo(1);
        
    }

    @Test
    public void testConnectionTtl_success() {
        
        when(randomGenerator.apply(3))
            .thenReturn(0)
            .thenReturn(1);

        strategy.setConnectionTTL(Duration.buildByMilliseconds(1000));
        
        assertThat(strategy.selectNextDestinationIndex(0, 3)).isEqualTo(0);
        
        strategy.connectSuccess(0, 0, 3);
        
        assertThat(strategy.shouldReconnect(5000, 0, 3)).isEqualTo(true);
        
        assertThat(strategy.selectNextDestinationIndex(0, 3)).isEqualTo(1);
    }

    @Test
    public void testConnectionTtl_failed() {
        
        when(randomGenerator.apply(3))
            .thenReturn(0)
            .thenReturn(2)
            .thenReturn(1);

        strategy.setConnectionTTL(Duration.buildByMilliseconds(1000));
        
        assertThat(strategy.selectNextDestinationIndex(0, 3)).isEqualTo(0);
        
        strategy.connectFailed(0, 0, 3);
        
        
        assertThat(strategy.selectNextDestinationIndex(0, 3)).isEqualTo(2);
        
        strategy.connectSuccess(1000, 1, 3);
        
        assertThat(strategy.shouldReconnect(5000, 0, 3)).isEqualTo(true);
        
        assertThat(strategy.selectNextDestinationIndex(0, 3)).isEqualTo(1);
        
    }

}
