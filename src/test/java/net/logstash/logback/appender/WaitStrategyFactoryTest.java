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
package net.logstash.logback.appender;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.LiteBlockingWaitStrategy;
import com.lmax.disruptor.PhasedBackoffWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.TimeoutBlockingWaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;

public class WaitStrategyFactoryTest {
    
    @Test
    public void testCreateNull() {
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(null)).isNull();
    }

    @Test
    public void testCreateEmpty() {
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" ")).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnknown() {
        WaitStrategyFactory.createWaitStrategyFromString("foo");
    }

    @Test
    public void testCreateBlocking() {
        assertThat(WaitStrategyFactory.createWaitStrategyFromString("blocking")).isInstanceOf(BlockingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" blocking ")).isInstanceOf(BlockingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" Blocking ")).isInstanceOf(BlockingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" BLOCKING ")).isInstanceOf(BlockingWaitStrategy.class);
    }

    @Test
    public void testCreateBusySpin() {
        assertThat(WaitStrategyFactory.createWaitStrategyFromString("busySpin")).isInstanceOf(BusySpinWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" busySpin ")).isInstanceOf(BusySpinWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" BusySpin ")).isInstanceOf(BusySpinWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" BUSYSPIN ")).isInstanceOf(BusySpinWaitStrategy.class);
    }

    @Test
    public void testCreateLiteBlocking() {
        assertThat(WaitStrategyFactory.createWaitStrategyFromString("liteBlocking")).isInstanceOf(LiteBlockingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" liteBlocking ")).isInstanceOf(LiteBlockingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" LiteBlocking ")).isInstanceOf(LiteBlockingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" LITEBLOCKING ")).isInstanceOf(LiteBlockingWaitStrategy.class);
    }

    @Test
    public void testCreateSleeping() {
        assertThat(WaitStrategyFactory.createWaitStrategyFromString("sleeping")).isInstanceOf(SleepingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" sleeping ")).isInstanceOf(SleepingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" Sleeping ")).isInstanceOf(SleepingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" SLEEPING ")).isInstanceOf(SleepingWaitStrategy.class);
    }

    @Test
    public void testCreateYielding() {
        assertThat(WaitStrategyFactory.createWaitStrategyFromString("yielding")).isInstanceOf(YieldingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" yielding ")).isInstanceOf(YieldingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" Yielding ")).isInstanceOf(YieldingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" YIELDING ")).isInstanceOf(YieldingWaitStrategy.class);
    }

    @Test
    public void testCreatePhasedBackoff() {
        assertThat(WaitStrategyFactory.createWaitStrategyFromString("phasedBackoff{1,2,SECONDS,blocking}")).isInstanceOf(PhasedBackoffWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" phasedBackoff{1,2,seconds,blocking} ")).isInstanceOf(PhasedBackoffWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" PhasedBackoff { 1, 2, SECONDS , blocking } ")).isInstanceOf(PhasedBackoffWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" PHASEDBACKOFF { 1, 2, SECONDS , blocking } ")).isInstanceOf(PhasedBackoffWaitStrategy.class);
        
        PhasedBackoffWaitStrategy waitStrategy = (PhasedBackoffWaitStrategy) WaitStrategyFactory.createWaitStrategyFromString(" PHASEDBACKOFF { 1, 2, SECONDS , blocking } ");
        
        assertThat(Whitebox.getInternalState(waitStrategy, "spinTimeoutNanos")).isEqualTo(TimeUnit.SECONDS.toNanos(1));
        assertThat(Whitebox.getInternalState(waitStrategy, "yieldTimeoutNanos")).isEqualTo(TimeUnit.SECONDS.toNanos(1 + 2));
        assertThat(Whitebox.getInternalState(waitStrategy, "fallbackStrategy")).isInstanceOf(BlockingWaitStrategy.class);
        
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreatePhasedBackoff_noParams() {
        WaitStrategyFactory.createWaitStrategyFromString("phasedBackoff");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreatePhasedBackoff_notEnoughParams() {
        WaitStrategyFactory.createWaitStrategyFromString("phasedBackoff{1,1,SECONDS}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreatePhasedBackoff_noEndParamDelimiter() {
        WaitStrategyFactory.createWaitStrategyFromString("phasedBackoff{1,1,SECONDS,blocking");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreatePhasedBackoff_unparsableParam() {
        WaitStrategyFactory.createWaitStrategyFromString("phasedBackoff{hello,1,SECONDS,blocking}");
    }

    public void testCreatePhasedBackoff_nested() {
        PhasedBackoffWaitStrategy waitStrategy = (PhasedBackoffWaitStrategy) WaitStrategyFactory.createWaitStrategyFromString("phasedBackoff{1,2,SECONDS,phasedBackoff{1,2,SECONDS,blocking}}");
        assertThat(Whitebox.getInternalState(waitStrategy, "fallbackStrategy")).isInstanceOf(PhasedBackoffWaitStrategy.class);
    }

    @Test
    public void testCreateTimeoutBlocking() {
        assertThat(WaitStrategyFactory.createWaitStrategyFromString("timeoutBlocking{1,SECONDS}")).isInstanceOf(TimeoutBlockingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" timeoutBlocking{1,seconds} ")).isInstanceOf(TimeoutBlockingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" TimeoutBlocking { 1, SECONDS } ")).isInstanceOf(TimeoutBlockingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" TIMEOUTBLOCKING { 1, SECONDS } ")).isInstanceOf(TimeoutBlockingWaitStrategy.class);
        
        TimeoutBlockingWaitStrategy waitStrategy = (TimeoutBlockingWaitStrategy) WaitStrategyFactory.createWaitStrategyFromString(" TIMEOUTBLOCKING { 1, SECONDS } ");
        
        assertThat(Whitebox.getInternalState(waitStrategy, "timeoutInNanos")).isEqualTo(TimeUnit.SECONDS.toNanos(1));
        
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTimeoutBlocking_noParams() {
        WaitStrategyFactory.createWaitStrategyFromString("timeoutBlocking");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTimeoutBlocking_notEnoughParams() {
        WaitStrategyFactory.createWaitStrategyFromString("timeoutBlocking{1}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTimeoutBlocking_noEndParamDelimiter() {
        WaitStrategyFactory.createWaitStrategyFromString("timeoutBlocking{1,SECONDS");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTimeoutBlocking_unparsableParam() {
        WaitStrategyFactory.createWaitStrategyFromString("timeoutBlocking{hello,SECONDS}");
    }
}
