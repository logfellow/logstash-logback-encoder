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

    private static class CharactersReplacer extends CharacterEscapes {

        private final int[] escapeCodesForAscii;
        private final List<Replace> replaces = new ArrayList<>();

        private CharactersReplacer() {
            escapeCodesForAscii = new int[128];
            Arrays.fill(escapeCodesForAscii, ESCAPE_NONE);
        }

        void addReplace(Replace replace) {
            replaces.add(replace);
            if (replace.target < 128) {
                escapeCodesForAscii[replace.target] = ESCAPE_CUSTOM;
            }
        }

        boolean removeReplace(Replace replace) {
            if (replace.target < 128) {
                escapeCodesForAscii[replace.target] = ESCAPE_NONE;
            }
            return replaces.remove(replace);
        }

        @Override
        public SerializableString getEscapeSequence(int ch) {
            // old fashioned for-loop. No iterator instance created
            for (int i = 0; i < replaces.size(); i++) {
                if (replaces.get(i).target == ch) {
                    return replaces.get(i).replacement;
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

        private int target;
        private SerializedString replacement;

        public static Replace create(String target, String replacement) {
            Replace instance = new Replace();
            instance.setTarget(target);
            instance.setReplacement(replacement);
            return instance;
        }

        public void setTarget(String target) {
            if (target.length() != 1) {
                throw new IllegalArgumentException("Target's length must be 1");
            }
            this.target = (int)target.charAt(0);
        }

        public void setReplacement(String replacement) {
            this.replacement = new SerializedString(replacement);
        }

        void assertValid() {
            if (0 == target || null == replacement) {
                throw new IllegalArgumentException("Target and Replacement must be non-empty strings");
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
