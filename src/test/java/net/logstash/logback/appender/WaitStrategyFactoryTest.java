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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.LiteBlockingWaitStrategy;
import com.lmax.disruptor.LiteTimeoutBlockingWaitStrategy;
import com.lmax.disruptor.PhasedBackoffWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.TimeoutBlockingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import org.junit.jupiter.api.Test;

public class WaitStrategyFactoryTest {
    
    @Test
    public void testCreateNull() {
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(null)).isNull();
    }

    @Test
    public void testCreateEmpty() {
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" ")).isNull();
    }

    public void testUnknown() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            WaitStrategyFactory.createWaitStrategyFromString("foo");
        });
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
    public void testCreateSleeping_noParams() {
        assertThat(WaitStrategyFactory.createWaitStrategyFromString("sleeping")).isInstanceOf(SleepingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" sleeping ")).isInstanceOf(SleepingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" Sleeping ")).isInstanceOf(SleepingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" SLEEPING ")).isInstanceOf(SleepingWaitStrategy.class);
    }

    @Test
    public void testCreateSleeping_parameterized() {
        assertThat(WaitStrategyFactory.createWaitStrategyFromString("sleeping{500,1000}")).isInstanceOf(SleepingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" sleeping{500,1000} ")).isInstanceOf(SleepingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" Sleeping { 500, 1000 } ")).isInstanceOf(SleepingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" SLEEPING { 500, 1000} ")).isInstanceOf(SleepingWaitStrategy.class);
        
        SleepingWaitStrategy waitStrategy = (SleepingWaitStrategy) WaitStrategyFactory.createWaitStrategyFromString(" SLEEPING { 500, 1000 } ");
        
        assertThat((int) getFieldValue(waitStrategy, SleepingWaitStrategy.class, "retries")).isEqualTo(500);
        assertThat((long) getFieldValue(waitStrategy, SleepingWaitStrategy.class, "sleepTimeNs")).isEqualTo(1000L);
    }

    @Test
    public void testCreateSleeping_notEnoughParams() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            WaitStrategyFactory.createWaitStrategyFromString("sleeping{500}");
        });
    }

    @Test
    public void testCreateSleeping_noEndParamDelimiter() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            WaitStrategyFactory.createWaitStrategyFromString("sleeping{500,1000");
        });
    }

    @Test
    public void testCreateSleeping_unparsableParam() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            WaitStrategyFactory.createWaitStrategyFromString("sleeping{hello,1000}");
        });
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
        
        assertThat((long) getFieldValue(waitStrategy, PhasedBackoffWaitStrategy.class, "spinTimeoutNanos")).isEqualTo(TimeUnit.SECONDS.toNanos(1));
        assertThat((long) getFieldValue(waitStrategy, PhasedBackoffWaitStrategy.class, "yieldTimeoutNanos")).isEqualTo(TimeUnit.SECONDS.toNanos(1 + 2));
        assertThat((WaitStrategy) getFieldValue(waitStrategy, PhasedBackoffWaitStrategy.class, "fallbackStrategy")).isInstanceOf(BlockingWaitStrategy.class);
        
    }

    @Test
    public void testCreatePhasedBackoff_noParams() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            WaitStrategyFactory.createWaitStrategyFromString("phasedBackoff");
        });
    }

    @Test
    public void testCreatePhasedBackoff_notEnoughParams() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            WaitStrategyFactory.createWaitStrategyFromString("phasedBackoff{1,1,SECONDS}");
        });
    }

    @Test
    public void testCreatePhasedBackoff_noEndParamDelimiter() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            WaitStrategyFactory.createWaitStrategyFromString("phasedBackoff{1,1,SECONDS,blocking");
        });
    }

    @Test
    public void testCreatePhasedBackoff_unparsableParam() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            WaitStrategyFactory.createWaitStrategyFromString("phasedBackoff{hello,1,SECONDS,blocking}");
        });
    }
    
    @Test
    public void testCreatePhasedBackoff_nested() {
        PhasedBackoffWaitStrategy waitStrategy = (PhasedBackoffWaitStrategy) WaitStrategyFactory.createWaitStrategyFromString("phasedBackoff{1,2,SECONDS,phasedBackoff{1,2,SECONDS,blocking}}");
        assertThat((WaitStrategy) getFieldValue(waitStrategy, PhasedBackoffWaitStrategy.class, "fallbackStrategy")).isInstanceOf(PhasedBackoffWaitStrategy.class);
    }

    @Test
    public void testCreatePhasedBackoff_nested_invalidStart() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            WaitStrategyFactory.createWaitStrategyFromString("phasedBackoff{1,2,SECONDS,phasedBackoff{1,2,SECONDS,blocking}");
        });
    }

    @Test
    public void testCreatePhasedBackoff_nested_invalidEnd() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            WaitStrategyFactory.createWaitStrategyFromString("phasedBackoff{1,2,SECONDS,phasedBackoff{1,2,SECONDS,blocking}}}");
        });
    }

    @Test
    public void testCreateTimeoutBlocking() {
        assertThat(WaitStrategyFactory.createWaitStrategyFromString("timeoutBlocking{1,SECONDS}")).isInstanceOf(TimeoutBlockingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" timeoutBlocking{1,seconds} ")).isInstanceOf(TimeoutBlockingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" TimeoutBlocking { 1, SECONDS } ")).isInstanceOf(TimeoutBlockingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" TIMEOUTBLOCKING { 1, SECONDS } ")).isInstanceOf(TimeoutBlockingWaitStrategy.class);
        
        TimeoutBlockingWaitStrategy waitStrategy = (TimeoutBlockingWaitStrategy) WaitStrategyFactory.createWaitStrategyFromString(" TIMEOUTBLOCKING { 1, SECONDS } ");
        
        assertThat((long) getFieldValue(waitStrategy, TimeoutBlockingWaitStrategy.class, "timeoutInNanos")).isEqualTo(TimeUnit.SECONDS.toNanos(1));
        
    }

    @Test
    public void testCreateTimeoutBlocking_noParams() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            WaitStrategyFactory.createWaitStrategyFromString("timeoutBlocking");
        });
    }

    @Test
    public void testCreateTimeoutBlocking_notEnoughParams() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            WaitStrategyFactory.createWaitStrategyFromString("timeoutBlocking{1}");
        });
    }

    @Test
    public void testCreateTimeoutBlocking_noEndParamDelimiter() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            WaitStrategyFactory.createWaitStrategyFromString("timeoutBlocking{1,SECONDS");
        });
    }

    @Test
    public void testCreateTimeoutBlocking_unparsableParam() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            WaitStrategyFactory.createWaitStrategyFromString("timeoutBlocking{hello,SECONDS}");
        });
    }
    
    @Test
    public void testCreateLiteTimeoutBlocking() {
        assertThat(WaitStrategyFactory.createWaitStrategyFromString("liteTimeoutBlocking{1,SECONDS}")).isInstanceOf(LiteTimeoutBlockingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" liteTimeoutBlocking{1,seconds} ")).isInstanceOf(LiteTimeoutBlockingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" LiteTimeoutBlocking { 1, SECONDS } ")).isInstanceOf(LiteTimeoutBlockingWaitStrategy.class);
        assertThat(WaitStrategyFactory.createWaitStrategyFromString(" LITETIMEOUTBLOCKING { 1, SECONDS } ")).isInstanceOf(LiteTimeoutBlockingWaitStrategy.class);
        
        LiteTimeoutBlockingWaitStrategy waitStrategy = (LiteTimeoutBlockingWaitStrategy) WaitStrategyFactory.createWaitStrategyFromString(" LITETIMEOUTBLOCKING { 1, SECONDS } ");
        
        assertThat((long) getFieldValue(waitStrategy, LiteTimeoutBlockingWaitStrategy.class, "timeoutInNanos")).isEqualTo(TimeUnit.SECONDS.toNanos(1));
        
    }

    @Test
    public void testCreateLiteTimeoutBlocking_noParams() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            WaitStrategyFactory.createWaitStrategyFromString("liteTimeoutBlocking");
        });
    }

    @Test
    public void testCreateLiteTimeoutBlocking_notEnoughParams() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            WaitStrategyFactory.createWaitStrategyFromString("liteTimeoutBlocking{1}");
        });
    }

    @Test
    public void testCreateLiteTimeoutBlocking_noEndParamDelimiter() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            WaitStrategyFactory.createWaitStrategyFromString("liteTimeoutBlocking{1,SECONDS");
        });
    }

    @Test
    public void testCreateLiteTimeoutBlocking_unparsableParam() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            WaitStrategyFactory.createWaitStrategyFromString("liteTimeoutBlocking{hello,SECONDS}");
        });
    }
    
    private <T> T getFieldValue(Object object, Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(object);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
