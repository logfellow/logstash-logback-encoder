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

import java.util.regex.Pattern;

import tools.jackson.core.JsonGenerator;

/**
 * Writes double values (instead of String values) for any MDC values that can be parsed as a double,
 * except NaN and positive/negative Infinity.
 */
public class DoubleMdcEntryWriter implements MdcEntryWriter {

    private static final Pattern DOUBLE_PATTERN = doublePattern();

    @Override
    public boolean writeMdcEntry(JsonGenerator generator, String fieldName, String mdcKey, String mdcValue) {
        if (shouldParse(mdcValue)) {
            try {
                double parsedValue = Double.parseDouble(mdcValue);
                generator.writeNumberProperty(fieldName, parsedValue);
                return true;
            } catch (NumberFormatException ignore) {
            }
        }

        return false;
    }

    /**
     * Returns true if an attempt at parsing the given value should be made.
     * When true is returned, we can be reasonably confident that {@link Double#parseDouble(String)}
     * will succeed.  However, it is not guaranteed to succeed.
     * This is mainly to avoid throwing/catching {@link NumberFormatException}
     * in as many cases as possible.
     */
    private boolean shouldParse(String value) {
        return value != null && !value.isEmpty() && DOUBLE_PATTERN.matcher(value).matches();
    }

    /**
     * Returns a Pattern that matches strings that can be parsed by {@link Double#parseDouble(String)}.
     * This regex comes from the javadoc for {@link Double#valueOf(String)},
     * but with NaN and Infinity removed.
     */
    private static Pattern doublePattern() {
        final String Digits = "(\\p{Digit}+)";
        final String HexDigits = "(\\p{XDigit}+)";
        // an exponent is 'e' or 'E' followed by an optionally
        // signed decimal integer.
        final String Exp = "[eE][+-]?" + Digits;
        final String fpRegex =
                ("[\\x00-\\x20]*"  // Optional leading "whitespace"
                        + "[+-]?(" // Optional sign character

                        // A decimal floating-point string representing a finite positive
                        // number without a leading sign has at most five basic pieces:
                        // Digits . Digits ExponentPart FloatTypeSuffix
                        //
                        // Since this method allows integer-only strings as input
                        // in addition to strings of floating-point literals, the
                        // two sub-patterns below are simplifications of the grammar
                        // productions from section 3.10.2 of
                        // The Java Language Specification.

                        // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
                        + "(((" + Digits + "(\\.)?(" + Digits + "?)(" + Exp + ")?)|"

                        // . Digits ExponentPart_opt FloatTypeSuffix_opt
                        + "(\\.(" + Digits + ")(" + Exp + ")?)|"

                        // Hexadecimal strings
                        + "(("
                        // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
                        + "(0[xX]" + HexDigits + "(\\.)?)|"

                        // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
                        + "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")"

                        + ")[pP][+-]?" + Digits + "))"
                        + "[fFdD]?))"
                        + "[\\x00-\\x20]*"); // Optional trailing "whitespace"
        return Pattern.compile(fpRegex);
    }

}
