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
package net.logstash.logback.layout.parser;

import ch.qos.logback.access.PatternLayout;
import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import ch.qos.logback.core.spi.ContextAware;

/**
 * @author <a href="mailto:dimas@dataart.com">Dmitry Andrianov</a>
 */
public class AccessEventJsonPatternParser extends AbstractJsonPatternParser<IAccessEvent> {

    public AccessEventJsonPatternParser(final ContextAware contextAware) {
        super(contextAware);
    }

    static class NullNaValueTransformer<E> implements ValueGetter<String, E> {

        private final ValueGetter<String, E> generator;

        NullNaValueTransformer(final ValueGetter<String, E> generator) {
            this.generator = generator;
        }

        @Override
        public String getValue(final E event) {
            final String value = generator.getValue(event);
            return "-".equals(value) ? null : value;
        }
    }

    @Override
    protected ValueGetter<?, IAccessEvent> operation(final String operation, final String data) {
        if ("nullNA".equals(operation) && data != null) {
            return new NullNaValueTransformer<IAccessEvent>(makeLayoutValueGetter(data));
        } else {
            return super.operation(operation, data);
        }
    }

    @Override
    protected PatternLayoutBase<IAccessEvent> createLayout() {
        return new PatternLayout();
    }

}
