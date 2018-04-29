package net.logstash.logback.decorate;

import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ReplaceCharsJsonFactoryDecorator implements JsonFactoryDecorator {

    private static class CustomizableCharacterReplacer extends CharacterEscapes {

        private final int[] escapeCodesForAscii;
        private final List<Replace> replacements = new ArrayList<>();

        private CustomizableCharacterReplacer() {
            escapeCodesForAscii = new int[128];
            Arrays.fill(escapeCodesForAscii, ESCAPE_NONE);
        }

        void addReplace(Replace replace) {
            replacements.add(replace);
            if (replace.target < 128) {
                escapeCodesForAscii[replace.target] = ESCAPE_CUSTOM;
            }
        }

        boolean removeReplace(Replace replace) {
            if (replace.target < 128) {
                escapeCodesForAscii[replace.target] = ESCAPE_NONE;
            }
            return replacements.remove(replace);
        }

        @Override
        public SerializableString getEscapeSequence(int ch) {
            // old fashioned for-loop. No iterator instance created
            for (int i = 0; i < replacements.size(); i++) {
                if (replacements.get(i).target == ch) {
                    return replacements.get(i).replacement;
                }
            }
            // for `ch < 128` only registered `characters` will be passed.
            // for `ch >= 128` null value is ok (if we got not replacement)
            return null;
        }

        @Override
        public int[] getEscapeCodesForAscii() {
            return escapeCodesForAscii;
        }
    }

    public static class Replace {

        // empty default replacement to simplify xml-configuration
        private static final SerializedString EMPTY_REPLACEMENT = new SerializedString("");

        private int target;
        private SerializedString replacement = EMPTY_REPLACEMENT;

        public static Replace create(String target, String replacement) {
            Replace instance = new Replace();
            instance.setTarget(target);
            instance.setReplacement(replacement);
            return instance;
        }

        public void setTarget(String target) {
            if (null == target || target.length() != 1) {
                throw new IllegalArgumentException("Target's length must be 1");
            }
            this.target = (int)target.charAt(0);
        }

        public void setTargetNumber(int idx) {
            if (idx < 1) {
                throw new IllegalArgumentException("Target char number should be positive integer");
            }
            this.target = idx;
        }

        public void setReplacement(String replacement) {
            this.replacement = new SerializedString(replacement);
        }

        void assertValid() {
            if (0 == target) {
                throw new IllegalArgumentException("Target must be non empty string");
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Replace replace = (Replace) o;
            return target == replace.target;
        }

        @Override
        public int hashCode() {
            return Objects.hash(target);
        }
    }

    private final CustomizableCharacterReplacer replacements = new CustomizableCharacterReplacer();

    @Override
    public MappingJsonFactory decorate(MappingJsonFactory factory) {
        return (MappingJsonFactory)factory.setCharacterEscapes(replacements);
    }

    public void addReplace(Replace replacement) {
        replacement.assertValid();
        replacements.addReplace(replacement);
    }

    public boolean removeReplace(Replace replacement) {
        replacement.assertValid();
        return replacements.removeReplace(replacement);
    }
}