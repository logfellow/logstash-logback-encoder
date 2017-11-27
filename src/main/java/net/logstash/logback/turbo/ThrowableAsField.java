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

import org.slf4j.MDC;
import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Adds two fields to MDC which contain (cause) exception class names.
 */
public class ThrowableAsField extends TurboFilter {

    static final String FIELD_NAME = "throwable_class";
    static final String FIELD_NAME_CAUSE = "throwable_cause_class";

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        // performance optimization
        if (t == null && (params == null || params.length == 0) || MDC.get(FIELD_NAME) != null) {
            return FilterReply.NEUTRAL;
        }

        Throwable throwable = t != null ? t : extractThrowableFromParams(params);
        if (throwable == null) {
            return FilterReply.NEUTRAL;
        }

        MDC.put(FIELD_NAME, throwable.getClass().getSimpleName());
        try {

            if (throwable.getCause() != null) {
                MDC.put(FIELD_NAME_CAUSE, getCause(throwable).getClass().getSimpleName());
            }

            logger.log(marker, logger.getName(), Level.toLocationAwareLoggerInteger(level), format, params, t);
            return FilterReply.DENY;

        } finally {
            MDC.remove(FIELD_NAME);
            MDC.remove(FIELD_NAME_CAUSE);
        }
    }

    /** @return null if no throwable in params */
    private static Throwable extractThrowableFromParams(Object[] params) {
        for (Object param : params) {
            if (param instanceof Throwable) {
                return (Throwable) param;
            }
        }
        return null;
    }

    /**
     * @return given throwable if t does not contain any cause
     */
    private static Throwable getCause(Throwable t) {
        return t.getCause() != null ? getCause(t.getCause()) : t;
    }
}
