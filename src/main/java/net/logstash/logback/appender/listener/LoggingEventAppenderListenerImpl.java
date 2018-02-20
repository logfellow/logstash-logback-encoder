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
package net.logstash.logback.appender.listener;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

/**
 * Implementation of {@link AppenderListener} for {@link ILoggingEvent}s that does nothing.
 */
public class LoggingEventAppenderListenerImpl implements AppenderListener<ILoggingEvent> {

    @Override
    public void appenderStarted(Appender<ILoggingEvent> appender) {
    }

    @Override
    public void appenderStopped(Appender<ILoggingEvent> appender) {
    }

    @Override
    public void eventAppended(Appender<ILoggingEvent> appender, ILoggingEvent event, long durationInNanos) {
    }

    @Override
    public void eventAppendFailed(Appender<ILoggingEvent> appender, ILoggingEvent event, Throwable reason) {
    }
}
