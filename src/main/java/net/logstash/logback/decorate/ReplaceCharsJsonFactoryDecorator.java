package net.logstash.logback.decorate;

import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import java.util.HashMap;
import java.util.Map;

public class ReplaceCharsJsonFactoryDecorator implements JsonFactoryDecorator {

    private static class CharactersReplacer extends CharacterEscapes {

        private final Map<Integer, SerializedString> replacements = new HashMap<>();
        private final int[] escapeCodesForAscii;

        private CharactersReplacer() {
            escapeCodesForAscii = standardAsciiEscapesForJSON();
        }

        void addReplace(Replace replace) {
            int charIdx = replace.targetIdx();
            this.replacements.put(charIdx, new SerializedString(replace.getReplacement()));

            if (charIdx <= 127) {
                escapeCodesForAscii[charIdx] = ESCAPE_CUSTOM;
            }
        }

        boolean removeReplace(Replace replace) {
            int charIdx = replace.targetIdx();
            if (charIdx <= 127) {
                escapeCodesForAscii[charIdx] = ESCAPE_STANDARD;
            }
            return null != replacements.remove(charIdx);
        }

        @Override
        public SerializableString getEscapeSequence(int ch) {
            // for `ch <= 127` only registered `ch` will be passed.
            // for `ch > 127` null value is ok (if we got not replacement)
            // todo think how to reduce unboxing
            return replacements.get(ch);
        }

        @Override
        public int[] getEscapeCodesForAscii() {
            return escapeCodesForAscii;
        }
    }

    public static class Replace {

        private String target;
        private String replacement;

        public static Replace create(String target, String replacement) {
            Replace instance = new Replace();
            instance.target = target;
            instance.replacement = replacement;
            return instance;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public void setReplacement(String replacement) {
            this.replacement = replacement;
        }

        String getReplacement() {
            return replacement;
        }

        int targetIdx() {
            char i = target.charAt(0);
            return (int)i;

        }

        void assertValid() {
            if (null == target || null == replacement) {
                throw new IllegalArgumentException("Target and Replacement must be non-empty strings");
            }

            if (target.length() != 1) {
                throw new IllegalArgumentException("Target's length must be 1");
            }
        }
    }

    private final CharactersReplacer escapes = new CharactersReplacer();

    @Override
    public MappingJsonFactory decorate(MappingJsonFactory factory) {
        return (MappingJsonFactory)factory.setCharacterEscapes(escapes);
    }

    public void addReplace(Replace escape) {
        escape.assertValid();
        escapes.addReplace(escape);
    }

    public boolean removeReplace(Replace escape) {
        escape.assertValid();
        return escapes.removeReplace(escape);
    }
}
