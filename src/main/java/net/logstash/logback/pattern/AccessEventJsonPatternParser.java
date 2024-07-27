/*
 * Copyright 2013-2023 the original author or authors.
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

import ch.qos.logback.access.common.PatternLayout;
import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import com.fasterxml.jackson.core.JsonFactory;

/**
 * @author <a href="mailto:dimas@dataart.com">Dmitry Andrianov</a>
 */
public class AccessEventJsonPatternParser extends AbstractJsonPatternParser<IAccessEvent> {

    public AccessEventJsonPatternParser(final Context context, final JsonFactory jsonFactory) {
        super(context, jsonFactory);
        
        addOperation("nullNA", new NullNaValueOperation());
    }
    
    protected class NullNaValueOperation implements Operation<String> {
        @Override
        public String apply(final String value) {
            return "-".equals(value) ? null : value;
        }
    }

    
    @Override
    protected PatternLayoutBase<IAccessEvent> createLayout() {
        return new PatternLayout();
    }
}
