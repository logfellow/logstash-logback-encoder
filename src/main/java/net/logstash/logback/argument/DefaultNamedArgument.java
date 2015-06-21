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

import org.apache.commons.lang.Validate;

/**
 * Default implementation of the {@link NamedArgument} interface.
 * {@link #toString()} formats {@link #getValue()} as a {@link String} using {@link NamedArguments#toString}
 *
 */
public class DefaultNamedArgument implements NamedArgument {

    private final String key;
    private final Object value;

    /**
     *
     * @param key cannot be null
     * @param value
     */
    public DefaultNamedArgument(String key, Object value) {
        Validate.notNull(key);
        this.key = key;
        this.value = value;
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
        return NamedArguments.toString(value);
    }
}
