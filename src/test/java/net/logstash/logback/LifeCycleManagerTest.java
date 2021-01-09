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
package net.logstash.logback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import ch.qos.logback.core.spi.LifeCycle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LifeCycleManagerTest {

    @Mock
    private LifeCycle lifeCycle;

    @Test
    void original_not_started() {

        LifeCycleManager lifecycleManager = new LifeCycleManager();
        lifecycleManager.start(lifeCycle);

        verify(lifeCycle).start();

        when(lifeCycle.isStarted()).thenReturn(true);

        lifecycleManager.stop(lifeCycle);

        verify(lifeCycle).stop();

    }

    @Test
    void original_started() {
        when(lifeCycle.isStarted()).thenReturn(true);

        LifeCycleManager lifecycleManager = new LifeCycleManager();
        lifecycleManager.start(lifeCycle);

        verify(lifeCycle, never()).start();

        lifecycleManager.stop(lifeCycle);

        verify(lifeCycle, never()).stop();

    }

    @Test
    void stopped_manually() {

        LifeCycleManager lifecycleManager = new LifeCycleManager();
        lifecycleManager.start(lifeCycle);

        verify(lifeCycle).start();

        when(lifeCycle.isStarted()).thenReturn(false);

        lifecycleManager.stop(lifeCycle);

        verify(lifeCycle, never()).stop();

    }

    @Test
    void unknown_stopped() {

        LifeCycleManager lifecycleManager = new LifeCycleManager();

        when(lifeCycle.isStarted()).thenReturn(true);

        lifecycleManager.stop(lifeCycle);

        verify(lifeCycle, never()).stop();

    }

    @Test
    void stopAll() {

        LifeCycle lifeCycle1 = mock(LifeCycle.class);
        LifeCycle lifeCycle2 = mock(LifeCycle.class);
        LifeCycle lifeCycle3 = mock(LifeCycle.class);

        when(lifeCycle2.isStarted()).thenReturn(true);

        LifeCycleManager lifecycleManager = new LifeCycleManager();
        lifecycleManager.start(lifeCycle1);
        lifecycleManager.start(lifeCycle2);
        lifecycleManager.start(lifeCycle3);

        verify(lifeCycle1).start();
        verify(lifeCycle2, never()).start();
        verify(lifeCycle3).start();

        when(lifeCycle1.isStarted()).thenReturn(true);

        Set<LifeCycle> stopped = lifecycleManager.stopAll();

        assertThat(stopped).containsExactly(lifeCycle1);

        verify(lifeCycle1).stop();
        verify(lifeCycle2, never()).stop();
        verify(lifeCycle3, never()).stop();

    }


}