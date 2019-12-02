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
package net.logstash.logback.decorate.mask;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.TokenStreamContext;

/**
 * Masks values based on a regular expression.
 */
public class RegexValueMasker implements ValueMasker {

    private final Pattern pattern;
    private final Object mask;

    /**
     * @param regex the regex used to identify values to mask
     * @param mask the value to write for values that match the regex (can contain back references to capture groups in the regex)
     */
    public RegexValueMasker(String regex, Object mask) {
        this(Pattern.compile(regex), mask);
    }

    /**
     * @param pattern the pattern used to identify values to mask
     * @param mask the value to write for values that match the regex (can contain back references to capture groups in the regex)
     */
    public RegexValueMasker(Pattern pattern, Object mask) {
        this.pattern = Objects.requireNonNull(pattern, "pattern must not be null");
        this.mask = Objects.requireNonNull(mask, "rmask must not be null");
    }

    @Override
    public Object mask(TokenStreamContext context, Object o) {
        if (o instanceof CharSequence) {
            Matcher matcher = pattern.matcher((CharSequence) o);
            if (matcher.matches()) {
                return mask instanceof String
                        ? matcher.replaceAll((String) mask)
                        : mask;
            }
        }
        return null;
    }
}
