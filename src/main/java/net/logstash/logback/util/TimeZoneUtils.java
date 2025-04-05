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
package net.logstash.logback.util;

import java.util.Objects;
import java.util.TimeZone;

public class TimeZoneUtils {

    private TimeZoneUtils() {
        // utility class
    }
    
    
    /**
     * Parse a string into the corresponding {@link TimeZone} using the format described by
     * {@link TimeZone#getTimeZone(String)}.
     * 
     * <p>The value of the {@code timeZone} can be any string accepted by java's {@link TimeZone#getTimeZone(String)}
     * method. For example "America/Los_Angeles" or "GMT+10".
     * 
     * @param str the string to parse into a valid {@link TimeZone}.
     * @return the {@link TimeZone} corresponding to the input string
     * @throws IllegalArgumentException thrown when the string is not a valid TimeZone textual
     *                                  representation.
     */
    public static TimeZone parseTimeZone(String str) {
        TimeZone tz = TimeZone.getTimeZone(Objects.requireNonNull(str));
        
        /*
         * Instead of throwing an exception when it fails to parse the string into a valid
         * TimeZone, getTimeZone() returns a TimeZone with id "GMT".
         * 
         * If the returned TimeZone is GMT but the input string is not, then the input string
         * was not a valid time zone representation.
         */
        if ("GMT".equals(tz.getID()) && !"GMT".equals(str)) {
            throw new IllegalArgumentException("Invalid TimeZone value (was '" + str + "')");
        }
        
        return tz;
    }
}
