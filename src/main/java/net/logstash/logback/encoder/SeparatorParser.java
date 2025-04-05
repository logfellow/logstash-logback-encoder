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
package net.logstash.logback.encoder;

public class SeparatorParser {
    
    private SeparatorParser() {
        // utility class
    }
    
    /**
     * Parses the given separator string.
     * 
     * The following values have special meaning:
     * <ul>
     * <li>{@code null} or empty string = no separator.</li>
     * <li>"{@code SYSTEM}}" = operating system new line.</li>
     * <li>"{@code UNIX}" = unix line ending ({@code \n}).</li>
     * <li>"{@code WINDOWS}" = windows line ending ({@code \r\n}).</li>
     * </ul>
     * <p>
     * Any other value will be returned as-is.
     * 
     * @param separator the separator format
     * @return the actual separator string after parsing
     */
    public static String parseSeparator(String separator) {
        if (separator == null || separator.isEmpty()) {
            return null;
        }
        if (separator.equalsIgnoreCase("SYSTEM")) {
            return System.lineSeparator();
        }
        if (separator.equalsIgnoreCase("UNIX")) {
            return "\n";
        }
        if (separator.equalsIgnoreCase("WINDOWS")) {
            return "\r\n";
        }
        return separator;
    }

}
