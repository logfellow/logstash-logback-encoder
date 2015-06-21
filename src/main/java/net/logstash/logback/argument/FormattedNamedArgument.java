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
package net.logstash.logback.argument;

import java.text.MessageFormat;

import org.apache.commons.lang.Validate;

/**
 * Formatted implementation of the {@link NamedArgument} interface.
 * {@link #toString()} formats the key (first args) and the value (second args) using a {@link MessageFormat}. <br/>
 * Note: the value is first formatted using {@link NamedArguments#toString}
 *
 */
public class FormattedNamedArgument implements NamedArgument {

    private final String key;
    private final Object value;
    private final String format;

    /**
     *
     * @param key cannot be null
     * @param value
     * @param format cannot be null
     */
    public FormattedNamedArgument(String key, Object value, String format) {
        Validate.notNull(key);
        Validate.notNull(format);
        this.key = key;
        this.value = value;
        this.format = format;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return MessageFormat.format(format , key , NamedArguments.toString(value));
    }
}
