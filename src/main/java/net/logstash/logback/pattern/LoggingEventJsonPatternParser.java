/*
 * Copyright 2013-2025 the original author or authors.
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
package net.logstash.logback.pattern;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import com.fasterxml.jackson.core.JsonFactory;

/**
 * @author <a href="mailto:dimas@dataart.com">Dmitry Andrianov</a>
 */
public class LoggingEventJsonPatternParser extends AbstractJsonPatternParser<ILoggingEvent> {

    public LoggingEventJsonPatternParser(final Context context, final JsonFactory jsonFactory) {
        super(context, jsonFactory);
    }

    /**
     * Create a new {@link PatternLayout} and replace the default {@code %property} converter
     * with a {@link EnhancedPropertyConverter} to add support for default value in case the
     * property is not defined.
     */
    @Override
    protected PatternLayoutBase<ILoggingEvent> createLayout() {
        PatternLayoutBase<ILoggingEvent> layout = new PatternLayout();
        PatternLayoutCompatibilityUtil.registerConverter(
                layout, "property", EnhancedPropertyConverter.class, EnhancedPropertyConverter::new);
        return layout;
    }

}
