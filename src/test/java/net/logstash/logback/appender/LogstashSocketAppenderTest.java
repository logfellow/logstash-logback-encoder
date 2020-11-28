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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LogstashSocketAppenderTest {

    @Mock
    private Layout<ILoggingEvent> layout;

    @Mock
    private Context context;

    @Mock
    private StatusManager statusManager;
    
    @Test
    public void testNoNullPointerWithNoCustomFields() throws Exception {
        // The JSON Parser has been throwing a NPE if no custom field value is specified
        LogstashSocketAppender appender = new LogstashSocketAppender();
        appender.setHost("foo.com");
        appender.buildLayout();
    }
    
    @Test
    public void testNoNullPointerWithCustomFields() throws Exception {
        LogstashSocketAppender appender = new LogstashSocketAppender();
        appender.setHost("foo.com");
        appender.setCustomFields("");
        appender.buildLayout();
    }

    @Test
    public void testSetLayout() {
        when(context.getStatusManager()).thenReturn(statusManager);
        LogstashSocketAppender appender = new LogstashSocketAppender();
        appender.setContext(context);
        appender.setLayout(layout);
        assertThat(appender.getLayout()).isNotEqualTo(layout);
        verify(statusManager).add(any(Status.class));
    }
}