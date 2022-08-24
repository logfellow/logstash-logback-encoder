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
package net.logstash.logback.decorate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;

/**
 * A {@link JsonFactoryDecorator} that can be used to customize the {@link JsonFactory#setCharacterEscapes(CharacterEscapes)}.
 *
 * For example, you could change the escape sequence used for newline characters from '\n' to '\u2028'
 */
public class CharacterEscapesJsonFactoryDecorator implements JsonFactoryDecorator {

    /**
     * A {@link CharacterEscapes} implementation that has been created from the registered {@link CharacterEscapesJsonFactoryDecorator#escapes}
     */
    @SuppressWarnings("serial")
    private static class CustomizedCharacterEscapes extends CharacterEscapes {

        /**
         * See {@link CharacterEscapes#getEscapeCodesForAscii()}
         */
        private final int[] escapeCodesForAscii;

        /**
         * The characterCodes whose escapeSequences have been customized.
         * Parallel with escapeSequences.
         */
        private final int[] targetCharacterCodes;

        /**
         * The customized escape sequences for specific characters.
         * Parallel with targetCharacterCodes.
         */
        private final SerializedString[] escapeSequences;

        private CustomizedCharacterEscapes(boolean includeStandardAsciiEscapesForJSON, List<Escape> escapes) {
            if (includeStandardAsciiEscapesForJSON) {
                escapeCodesForAscii = standardAsciiEscapesForJSON();
            } else {
                escapeCodesForAscii = new int[128];
                Arrays.fill(escapeCodesForAscii, ESCAPE_NONE);
            }

            /*
             * Sort the escapes, so that binarySearch can be used by getEscapeSequence
             */
            List<Escape> sortedEscapes = new ArrayList<>(escapes);
            Collections.sort(sortedEscapes);

            targetCharacterCodes = new int[sortedEscapes.size()];
            escapeSequences = new SerializedString[sortedEscapes.size()];

            for (int i = 0; i < sortedEscapes.size(); i++) {
                Escape escape = sortedEscapes.get(i);
                if (escape.getTargetCharacterCode() < 128) {
                    escapeCodesForAscii[escape.getTargetCharacterCode()] = ESCAPE_CUSTOM;
                }
                /*
                 * Keep parallel arrays of these, so that a binary search can be performed against targetCharacterCodes
                 * in order to determine the escapeSequence to return from getEscapeSequence
                 */
                targetCharacterCodes[i] = escape.getTargetCharacterCode();
                escapeSequences[i] = escape.getEscapeSequence();
            }
        }

        @Override
        public SerializableString getEscapeSequence(int ch) {

            int index = Arrays.binarySearch(targetCharacterCodes, ch);
            if (index >= 0) {
                return escapeSequences[index];
            }
            // for `ch < 128` only registered characters will be passed.
            // for `ch >= 128` null value is ok (if we got not replacement)
            return null;
        }

        @Override
        public int[] getEscapeCodesForAscii() {
            return escapeCodesForAscii;
        }
    }

    /**
     * Defines how a character will be escaped whenever that character is attempted to be written by a JsonGenerator.
     */
    public static class Escape implements Comparable<Escape> {

        private static final SerializedString EMPTY_ESCAPE_SEQUENCE = new SerializedString("");

        /**
         * The character code of the character that will be replaced with the {@link #escapeSequence} when written.
         */
        private int targetCharacterCode = -1;
        /**
         * The string with which the {@link #targetCharacterCode} will be replaced.
         */
        private SerializedString escapeSequence = EMPTY_ESCAPE_SEQUENCE;

        public Escape() {
        }

        public Escape(String target, String escapeSequence) {
            setTarget(target);
            setEscapeSequence(escapeSequence);
        }

        public Escape(int targetCharacterCode, String escapeSequence) {
            setTargetCharacterCode(targetCharacterCode);
            setEscapeSequence(escapeSequence);
        }

        public Escape(char targetCharacter, String escapeSequence) {
            setTargetCharacter(targetCharacter);
            setEscapeSequence(escapeSequence);
        }

        /**
         * Sets the target string that will be replaced with the {@link #escapeSequence}.  Must have length == 1
         *
         * @param target the target string that will be escaped
         * @throws IllegalArgumentException if target length is != 1
         */
        public void setTarget(String target) {
            if (target == null || target.length() != 1) {
                throw new IllegalArgumentException("target's length must be 1");
            }
            setTargetCharacterCode((int) target.charAt(0));
        }

        /**
         * Sets the target character that will be replaced with the {@link #escapeSequence}.
         * 
         * @param targetCharacter the target character
         */
        public void setTargetCharacter(char targetCharacter) {
            setTargetCharacterCode((char) targetCharacter);
        }

        public int getTargetCharacterCode() {
            return targetCharacterCode;
        }
        public void setTargetCharacterCode(int targetCharacterCode) {
            if (targetCharacterCode < 0) {
                throw new IllegalArgumentException("targetCharacterCode must be greater than zero");
            }
            this.targetCharacterCode = targetCharacterCode;
        }

        public SerializedString getEscapeSequence() {
            return escapeSequence;
        }
        public void setEscapeSequence(String escapeSequence) {
            if (escapeSequence == null) {
                this.escapeSequence = EMPTY_ESCAPE_SEQUENCE;
            } else {
                this.escapeSequence = new SerializedString(escapeSequence);
            }
        }

        private void assertValid() {
            if (targetCharacterCode < 0) {
                throw new IllegalArgumentException("targetCharacterCode must be 0 or greater");
            }
        }

        @Override
        public int compareTo(Escape that) {
            if (that == null) {
                return 1;
            }
            int targetCharacterCodeComparison = this.targetCharacterCode - that.targetCharacterCode;
            if (targetCharacterCodeComparison != 0) {
                return targetCharacterCodeComparison;
            }

            return this.escapeSequence.getValue().compareTo(that.escapeSequence.getValue());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Escape that = (Escape) o;
            return this.targetCharacterCode == that.targetCharacterCode
                    && this.escapeSequence.equals(that.escapeSequence);
        }

        @Override
        public int hashCode() {
            return Objects.hash(targetCharacterCode);
        }

    }

    /**
     * When true (the default), the standard ASCII escape codes for JSON will be included by default.
     * Any additional escapes configured for ASCII characters will override these.
     *
     * When false, no escaping for ASCII will be provided by default.
     * Only the escapes configured for ASCII characters will be used.
     */
    private boolean includeStandardAsciiEscapesForJSON = true;

    /**
     * The custom escapes that have been registered so far.
     */
    private final List<Escape> escapes = new ArrayList<>();

    /**
     * Indicates when the {@link CharacterEscapesJsonFactoryDecorator#characterEscapes} field needs to be re-initialized.
     */
    private boolean needsInitialization = true;

    /**
     * A {@link CharacterEscapes} implementation that has been created from the registered {@link CharacterEscapesJsonFactoryDecorator#escapes}
     */
    private CustomizedCharacterEscapes characterEscapes;

    @Override
    public JsonFactory decorate(JsonFactory factory) {
        if (needsInitialization) {
            characterEscapes = new CustomizedCharacterEscapes(includeStandardAsciiEscapesForJSON, escapes);
            needsInitialization = false;
        }
        return factory.setCharacterEscapes(characterEscapes);
    }

    public boolean isIncludeStandardAsciiEscapesForJSON() {
        return includeStandardAsciiEscapesForJSON;
    }
    public void setIncludeStandardAsciiEscapesForJSON(boolean includeStandardAsciiEscapesForJSON) {
        this.includeStandardAsciiEscapesForJSON = includeStandardAsciiEscapesForJSON;
        needsInitialization = true;
    }

    public void addEscape(Escape escape) {
        escape.assertValid();
        escapes.add(escape);
        needsInitialization = true;
    }
    public void removeEscape(Escape escape) {
        escapes.remove(escape);
        needsInitialization = true;
    }
}
