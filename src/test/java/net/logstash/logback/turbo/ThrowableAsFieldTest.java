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
package net.logstash.logback.turbo;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import org.junit.*;
import org.slf4j.MDC;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;

public class ThrowableAsFieldTest {
    private final TurboFilter filter = new ThrowableAsField();
    private final LoggerContext context = new LoggerContext();

    private FilterReply filter(Throwable t) {
        return filter.decide(null, context.getLogger("loggerName"), Level.INFO, "message", null, t);
    }

    /** if new message is logged with fields is untestable because Logger is not mockable */
    @Test
    public void addExceptionClassToMdc() {
        assertThat(filter(new IllegalStateException()), equalTo(FilterReply.DENY));
    }

    /** if new message is logged with fields is untestable because Logger is not mockable */
    @Test
    public void addExceptionCauseClassToMdc() {
        assertThat(filter(new IllegalStateException(new IllegalArgumentException())), equalTo(FilterReply.DENY));
    }

    @Test
    public void removeExceptionClassesFromMdc() {
        assertThat(filter(new IllegalStateException()), equalTo(FilterReply.DENY));
        assertTrue(MDC.getCopyOfContextMap().isEmpty());
    }

    @Test
    public void ignoreIfNoException() {
        assertThat(filter(null), equalTo(FilterReply.NEUTRAL));
    }

    @Test
    public void ignoreIfAlreadyContainsExceptionClass() {
        MDC.put(ThrowableAsField.FIELD_NAME, "name");
        try {
            assertThat(filter(null), equalTo(FilterReply.NEUTRAL));
        } finally {
            MDC.remove(ThrowableAsField.FIELD_NAME);
        }
    }

    @Test
    public void logIfExceptionIsParameter() {
        assertThat(filter.decide(null, context.getLogger("loggerName"), Level.INFO, "message",
                new Object[] { new IllegalArgumentException() }, null), equalTo(FilterReply.DENY));
    }
}