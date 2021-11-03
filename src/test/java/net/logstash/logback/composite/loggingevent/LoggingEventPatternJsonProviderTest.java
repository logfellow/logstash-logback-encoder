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
package net.logstash.logback.composite.loggingevent;

import net.logstash.logback.composite.AbstractPatternJsonProvider;
import net.logstash.logback.composite.AbstractPatternJsonProviderTest;
import net.logstash.logback.pattern.AbstractJsonPatternParser;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonFactory;

/**
 * @author <a href="mailto:dimas@dataart.com">Dmitry Andrianov</a>
 */
public class LoggingEventPatternJsonProviderTest extends AbstractPatternJsonProviderTest<ILoggingEvent> {
    
    @Override
    protected AbstractPatternJsonProvider<ILoggingEvent> createProvider() {
        return new LoggingEventPatternJsonProvider() {
            @Override
            protected AbstractJsonPatternParser<ILoggingEvent> createParser(JsonFactory jsonFactory) {
                // The base class needs to hook into the parser for some assertions
                return decorateParser(super.createParser(jsonFactory));
            }
        };
    }
}
