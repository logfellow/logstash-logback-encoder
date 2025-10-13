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
package net.logstash.logback.composite.loggingevent.mdc;

import tools.jackson.core.JsonGenerator;

/**
 * Writes long values (instead of String values) for any MDC values that can be parsed as a long (radix 10).
 */
public class LongMdcEntryWriter implements MdcEntryWriter {

    @Override
    public boolean writeMdcEntry(JsonGenerator generator, String fieldName, String mdcKey, String mdcValue) {
        if (shouldParse(mdcValue)) {
            try {
                long parsedValue = Long.parseLong(mdcValue);
                generator.writeNumberProperty(fieldName, parsedValue);
                return true;
            } catch (NumberFormatException ignore) {
            }
        }

        return false;
    }

    /**
     * Returns true if an attempt at parsing the given value should be made.
     * When true is returned, we can be reasonably confident that {@link Long#parseLong(String)}
     * will succeed.  However, it is not guaranteed to succeed.
     * This is mainly to avoid throwing/catching {@link NumberFormatException}
     * in as many cases as possible.
     */
    private boolean shouldParse(String value) {
        if (value == null || value.isEmpty() || value.length() > 20) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '+' || c == '-') {
                if (i != 0 || value.length() == 1) {
                    return false;
                }
            }
            else if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

}
