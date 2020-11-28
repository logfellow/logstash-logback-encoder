/**
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
package net.logstash.logback.stacktrace;

import java.util.Deque;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StackHasherTest {

    private static class StackTraceElementGenerator {
        public static void generateSingle() {
            oneSingle();
        }

        public static void oneSingle() {
            twoSingle();
        }

        private static void twoSingle() {
            threeSingle();
        }

        private static void threeSingle() {
            four();
        }

        private static void four() {
            five();
        }

        private static void five() {
            six();
        }

        private static void six() {
            seven();
        }

        private static void seven() {
            eight();
        }

        private static void eight() {
            throw new RuntimeException("message");
        }

        public static void generateCausedBy() {
            oneCausedBy();
        }

        private static void oneCausedBy() {
            twoCausedBy();
        }

        private static void twoCausedBy() {
            try {
                threeSingle();
            } catch (RuntimeException e) {
                throw new RuntimeException("wrapper", e);
            }
        }

        public static void generateSuppressed() {
            oneSuppressed();
        }

        private static void oneSuppressed() {
            twoSuppressed();
        }

        private static void twoSuppressed() {
            try {
                threeSingle();
            } catch (RuntimeException e) {
                RuntimeException newException = new RuntimeException();
                newException.addSuppressed(e);
                throw newException;
            }
        }
    }

    @Test
    public void one_hash_should_be_generated() {
        try {
            StackTraceElementGenerator.generateSingle();
            Assertions.fail();
        } catch (RuntimeException e) {
            // GIVEN
            StackHasher hasher = new StackHasher();

            // WHEN
            Deque<String> hashes = hasher.hexHashes(e);

            // THEN
            Assertions.assertEquals(1, hashes.size());
        }
    }

    @Test
    public void two_hashes_should_be_generated() {
        try {
            StackTraceElementGenerator.generateCausedBy();
            Assertions.fail();
        } catch (RuntimeException e) {
            // GIVEN
            StackHasher hasher = new StackHasher();

            // WHEN
            Deque<String> hashes = hasher.hexHashes(e);

            // THEN
            Assertions.assertEquals(2, hashes.size());
        }
    }
    private static class OnlyFromStackTraceElementGeneratorFilter extends StackElementFilter {
        @Override
        public boolean accept(StackTraceElement element) {
            return element.getClassName().equals(StackTraceElementGenerator.class.getName());
        }
    }

    /**
     * Warning: computes expected hash based on StackTraceElementGenerator elements
     *
     * do not change methods name, line or it will break the test
     */
    @Test
    public void expected_hash_should_be_generated() {
        try {
            StackTraceElementGenerator.generateSingle();
            Assertions.fail();
        } catch (RuntimeException e) {
            // GIVEN
            StackHasher hasher = new StackHasher(new OnlyFromStackTraceElementGeneratorFilter());

            // WHEN
            Deque<String> hashes = hasher.hexHashes(e);

            // THEN
            Assertions.assertEquals(1, hashes.size());
            Assertions.assertEquals("e30d4cae", hashes.getFirst());
        }
    }
}