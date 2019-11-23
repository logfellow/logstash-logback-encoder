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

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.TimeZone;

import ch.qos.logback.core.spi.DeferredProcessingAware;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link TcpAppenderListener} that logs a warning message on the first
 * append/send/connect success after a series of consecutive failures.
 * The warning message includes the number of consecutive failures,
 * the timestamp of the first failure,
 * and the duration between the first and last failures.
 *
 * <p>The default logger to which the event is sent is the fully qualified name of {@link FailureSummaryLoggingAppenderListener},
 * and can be changed by calling {@link #setLoggerName(String)}.</p>
 */
public class FailureSummaryLoggingAppenderListener<Event extends DeferredProcessingAware> extends FailureSummaryAppenderListener<Event> {

    private volatile Logger logger = LoggerFactory.getLogger(FailureSummaryLoggingAppenderListener.class);

    /**
     * Logs a message with the details from the given {@link FailureSummary}
     * with the given callback type.
     *
     * @param failureSummary contains summary details of all the consecutive failures
     * @param callbackType the type of failure (append/send/connect)
     */
    @Override
    protected void handleFailureSummary(FailureSummary failureSummary, CallbackType callbackType) {
        if (logger.isWarnEnabled()) {
            if (failureSummary.getFirstFailure() != failureSummary.getMostRecentFailure()) {
                failureSummary.getMostRecentFailure().addSuppressed(failureSummary.getFirstFailure());
            }
            logger.warn("{} {} failures since {} for {}.",
                    StructuredArguments.value("failEventCount", failureSummary.getConsecutiveFailures()),
                    StructuredArguments.value("failType", callbackType.name().toLowerCase()),
                    StructuredArguments.value("failStartTime", DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(TimeZone.getDefault().toZoneId()).format(Instant.ofEpochMilli(failureSummary.getFirstFailureTime()))),
                    StructuredArguments.value("failDuration", Duration.ofMillis(System.currentTimeMillis() - failureSummary.getFirstFailureTime()).toString()),
                    failureSummary.getMostRecentFailure());
        }
    }

    public String getLoggerName() {
        return logger.getName();
    }

    /**
     * Sets the slf4j logger name to which to log.
     * Defaults to the fully qualified name of {@link FailureSummaryLoggingAppenderListener}.
     *
     * @param loggerName the name of the logger to which to log.
     */
    public void setLoggerName(String loggerName) {
        logger = LoggerFactory.getLogger(Objects.requireNonNull(loggerName));
    }
}
