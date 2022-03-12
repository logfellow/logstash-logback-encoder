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
package net.logstash.logback.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.status.WarnStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LevelFilteringStatusListenerTest {

    private static final InfoStatus INFO_STATUS = new InfoStatus("foo", null);
    private static final WarnStatus WARN_STATUS = new WarnStatus("foo", null);
    private static final ErrorStatus ERROR_STATUS = new ErrorStatus("foo", null);

    @Mock
    private StatusListener simpleStatusListener;

    @Mock
    private OnConsoleStatusListener onConsoleStatusListener;

    @Mock
    private Context context;

    @Test
    public void simpleStatusListener_info() {
        LevelFilteringStatusListener statusListener = new LevelFilteringStatusListener();
        statusListener.setDelegate(simpleStatusListener);
        statusListener.setContext(context);
        statusListener.start();

        assertThat(statusListener.isStarted()).isTrue();

        statusListener.addStatusEvent(INFO_STATUS);
        verify(simpleStatusListener).addStatusEvent(INFO_STATUS);

        statusListener.addStatusEvent(WARN_STATUS);
        verify(simpleStatusListener).addStatusEvent(WARN_STATUS);

        statusListener.addStatusEvent(ERROR_STATUS);
        verify(simpleStatusListener).addStatusEvent(ERROR_STATUS);

        statusListener.stop();
        assertThat(statusListener.isStarted()).isFalse();
    }

    @Test
    public void onConsoleStatusListener_warn() {
        LevelFilteringStatusListener statusListener = new LevelFilteringStatusListener();
        statusListener.setLevelValue(Status.WARN);
        statusListener.setContext(context);

        statusListener.setDelegate(onConsoleStatusListener);
        verify(onConsoleStatusListener).setRetrospective(0L);

        statusListener.start();
        verify(onConsoleStatusListener).setContext(context);

        verify(onConsoleStatusListener).start();

        assertThat(statusListener.isStarted()).isTrue();

        statusListener.addStatusEvent(INFO_STATUS);
        verify(onConsoleStatusListener, never()).addStatusEvent(INFO_STATUS);

        statusListener.addStatusEvent(WARN_STATUS);
        verify(onConsoleStatusListener).addStatusEvent(WARN_STATUS);

        statusListener.addStatusEvent(ERROR_STATUS);
        verify(onConsoleStatusListener).addStatusEvent(ERROR_STATUS);

        statusListener.stop();
        verify(onConsoleStatusListener).stop();
        assertThat(statusListener.isStarted()).isFalse();
    }

    @Test
    public void setLevel() {
        LevelFilteringStatusListener statusListener = new LevelFilteringStatusListener();
        statusListener.setContext(context);
        assertThat(statusListener.getLevelValue()).isEqualTo(Status.INFO);

        statusListener.setLevel("wArN");
        assertThat(statusListener.getLevelValue()).isEqualTo(Status.WARN);

        statusListener.setLevel("error");
        assertThat(statusListener.getLevelValue()).isEqualTo(Status.ERROR);

        statusListener.setLevel("INFO");
        assertThat(statusListener.getLevelValue()).isEqualTo(Status.INFO);

        assertThatThrownBy(() -> statusListener.setLevel("bogus")).isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    public void setLevelValue() {
        LevelFilteringStatusListener statusListener = new LevelFilteringStatusListener();
        statusListener.setContext(context);
        assertThat(statusListener.getLevelValue()).isEqualTo(Status.INFO);

        statusListener.setLevelValue(Status.WARN);
        assertThat(statusListener.getLevelValue()).isEqualTo(Status.WARN);

        statusListener.setLevelValue(Status.ERROR);
        assertThat(statusListener.getLevelValue()).isEqualTo(Status.ERROR);

        statusListener.setLevelValue(Status.INFO);
        assertThat(statusListener.getLevelValue()).isEqualTo(Status.INFO);

        assertThatThrownBy(() -> statusListener.setLevelValue(400)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> statusListener.setLevelValue(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
