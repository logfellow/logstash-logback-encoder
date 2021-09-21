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
     * <p>The string can be a valid time zone ID. For instance, the time zone ID for the
     * U.S. Pacific Time zone is "America/Los_Angeles".
     * 
     * <p>If the time zone you want is not represented by one of the
     * supported IDs, then a custom time zone ID can be specified to
     * produce a TimeZone. The syntax of a custom time zone ID is:
     *
     * <blockquote><pre>
     * <i>CustomID:</i>
     *         <code>GMT</code> <i>Sign</i> <i>Hours</i> <code>:</code> <i>Minutes</i>
     *         <code>GMT</code> <i>Sign</i> <i>Hours</i> <i>Minutes</i>
     *         <code>GMT</code> <i>Sign</i> <i>Hours</i>
     * <i>Sign:</i> one of
     *         <code>+ -</code>
     * <i>Hours:</i>
     *         <i>Digit</i>
     *         <i>Digit</i> <i>Digit</i>
     * <i>Minutes:</i>
     *         <i>Digit</i> <i>Digit</i>
     * <i>Digit:</i> one of
     *         <code>0 1 2 3 4 5 6 7 8 9</code>
     * </pre></blockquote>
     *
     * <i>Hours</i> must be between 0 to 23 and <i>Minutes</i> must be
     * between 00 to 59.  For example, "GMT+10" and "GMT+0010" mean ten
     * hours and ten minutes ahead of GMT, respectively.
     * 
     * @param str the string to parse into a valid {@link TimeZone}.
     * @return the {@link TimeZone} corresponding to the input string
     * @throws IllegalArgumentException thrown when the string is not a valid TimeZone textual
     *                                  representation.
     */
    public static TimeZone parse(String str) {
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
