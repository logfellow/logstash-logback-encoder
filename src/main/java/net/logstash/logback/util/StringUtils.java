/*
 * Copyright 2013-2022 the original author or authors.
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

import static ch.qos.logback.core.CoreConstants.EMPTY_STRING;
import static ch.qos.logback.core.CoreConstants.EMPTY_STRING_ARRAY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Operations on {@link java.lang.String} that are
 * {@code null} safe.
 * 
 * <p>Largely inspired by Apache Commons Lang3.
 * 
 * <p>Note: This class is for internal use only and subject to backward incompatible change
 * at any time.
 *
 * @author brenuart
 */
public class StringUtils {

    private StringUtils() {
        // utility class - no instantiation
    }
    
    /**
     * <p>Removes control characters (char &lt;= 32) from both
     * ends of this String, handling {@code null} by returning
     * {@code null}.</p>
     *
     * <p>The String is trimmed using {@link String#trim()}.
     * Trim removes start and end characters &lt;= 32.</p>
     *
     * <pre>
     * StringUtils.trim(null)          = null
     * StringUtils.trim("")            = ""
     * StringUtils.trim("     ")       = ""
     * StringUtils.trim("abc")         = "abc"
     * StringUtils.trim("    abc    ") = "abc"
     * </pre>
     *
     * @param str the String to be trimmed, may be null
     * @return the trimmed string, {@code null} if null String input
     */
    public static String trim(final String str) {
        return str == null ? null : str.trim();
    }

    /**
     * <p>Removes control characters (char &lt;= 32) from both
     * ends of this String returning an empty String ("") if the String
     * is empty ("") after the trim or if it is {@code null}.
     *
     * <p>The String is trimmed using {@link String#trim()}.
     * Trim removes start and end characters &lt;= 32.</p>
     *
     * <pre>
     * StringUtils.trimToEmpty(null)          = ""
     * StringUtils.trimToEmpty("")            = ""
     * StringUtils.trimToEmpty("     ")       = ""
     * StringUtils.trimToEmpty("abc")         = "abc"
     * StringUtils.trimToEmpty("    abc    ") = "abc"
     * </pre>
     *
     * @param str the String to be trimmed, may be null
     * @return the trimmed String, or an empty String if {@code null} input
     */
    public static String trimToEmpty(final String str) {
        return str == null ? EMPTY_STRING : str.trim();
    }

    /**
     * <p>Removes control characters (char &lt;= 32) from both
     * ends of this String returning {@code null} if the String is
     * empty ("") after the trim or if it is {@code null}.
     *
     * <p>The String is trimmed using {@link String#trim()}.
     * Trim removes start and end characters &lt;= 32.</p>
     *
     * <pre>
     * StringUtils.trimToNull(null)          = null
     * StringUtils.trimToNull("")            = null
     * StringUtils.trimToNull("     ")       = null
     * StringUtils.trimToNull("abc")         = "abc"
     * StringUtils.trimToNull("    abc    ") = "abc"
     * </pre>
     *
     * @param str the String to be trimmed, may be null
     * @return the trimmed String, {@code null} if only chars &lt;= 32, empty
     *         or null String input
     */
    public static String trimToNull(final String str) {
        final String ts = trim(str);
        return isEmpty(ts) ? null : ts;
    }
    
    /**
     * <p>Checks if a CharSequence is empty ("") or null.</p>
     *
     * <pre>
     * StringUtils.isEmpty(null)      = true
     * StringUtils.isEmpty("")        = true
     * StringUtils.isEmpty(" ")       = false
     * StringUtils.isEmpty("bob")     = false
     * StringUtils.isEmpty("  bob  ") = false
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     */
    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }
    
    /**
     * <p>Checks if a CharSequence is empty (""), null or whitespace only.</p>
     *
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     *
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("bob")     = false
     * StringUtils.isBlank("  bob  ") = false
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null, empty or whitespace only
     */
    public static boolean isBlank(final CharSequence cs) {
        final int strLen = length(cs);
        if (strLen == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Gets a CharSequence length or {@code 0} if the CharSequence is
     * {@code null}.
     *
     * @param cs a CharSequence or {@code null}
     * @return CharSequence length or {@code 0} if the CharSequence is
     *         {@code null}.
     */
    public static int length(final CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }
    
    /**
     * Convert a comma delimited list into an array of strings.
     * 
     * @param str the input {@code String} (potentially {@code null} or empty)
     * @return an array of strings, or the empty array in case of empty input
     * @see #delimitedListToStringArray
     */
    public static String[] commaDelimitedListToStringArray(String str) {
        return delimitedListToStringArray(str, ",");
    }
    
    /**
     * Take a {@code String} that is a delimited list and convert it into
     * a {@code String} array.
     * 
     * <p>A single {@code delimiter} may consist of more than one character,
     * but it will still be considered as a single delimiter string, rather
     * than as a bunch of potential delimiter characters.
     * Delimiter can be escaped by prefixing it with a backslash ({@code \}).
     * 
     * <p>Values are trimmed, and are added to the resulting array only if not blank.
     * Therefore two consecutive delimiters are treated as a single delimiter.
     * 
     * 
     * <p>A {@code null} delimiter is treated as no delimiter and returns an array with
     * the original {@code str} string as single element.
     * An empty delimiter splits the input string at each character.
     * 
     * <p>A {@code null} input returns an empty array.
     * 
     * @param str the input {@code String} (potentially {@code null} or empty)
     * @param delimiter the delimiter between elements
     * @return an array of the tokens in the list
     */
    public static String[] delimitedListToStringArray(String str, String delimiter) {

        if (str == null || str.isEmpty()) {
            return EMPTY_STRING_ARRAY;
        }
        if (delimiter == null) {
            return new String[] {str};
        }

        List<String> result = new ArrayList<>();
        if (delimiter.isEmpty()) {
            for (int i = 0; i < str.length(); i++) {
                result.add(str.substring(i, i + 1));
            }
        }
        else {
            int pos = 0;
            int searchPos = 0;
            int nextPos;
            boolean escaping = false;

            while ((nextPos = str.indexOf(delimiter, searchPos)) != -1) {
                if (nextPos > 0 && str.charAt(nextPos - 1) == '\\') {
                    /*
                     *  The delimiter is escaped -> continue search after the escaped
                     *  delimiter we just found
                     */
                    searchPos = nextPos + delimiter.length();
                    escaping = true;
                }
                else {
                    addToResult(result, str.substring(pos, nextPos), escaping, delimiter);
                    escaping = false;
                    pos = nextPos + delimiter.length();
                    searchPos = pos;
                }
            }
            
            
            if (pos <= str.length()) {
                addToResult(result, str.substring(pos), escaping, delimiter);
            }
        }
        return result.toArray(EMPTY_STRING_ARRAY);
    }

    /**
     * Add a string to the collection after unescaping the delimiter and trimming the result.
     * The resulting string is actually added to the collection only if not blank.
     *
     * @param result the collection to add the string to
     * @param str the string to eventually add to the collection
     * @param unescape indicate whether delimiter should be "un-escaped" ({@code true}) or not ({@code false}).
     * @param delimiter the delimiter
     */
    private static void addToResult(Collection<String> result, String str, boolean unescape, String delimiter) {
        if (unescape) {
            str = str.replace("\\" + delimiter, delimiter);
        }
        str = trim(str);
        if (!isBlank(str)) {
            result.add(str);
        }
    }
}
