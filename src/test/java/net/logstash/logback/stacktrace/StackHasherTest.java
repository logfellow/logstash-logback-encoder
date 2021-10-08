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
package net.logstash.logback.stacktrace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Deque;

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
    }

    @Test
    public void one_hash_should_be_generated() {
        try {
            StackTraceElementGenerator.generateSingle();
            fail("Exception must have been thrown");
        } catch (RuntimeException e) {
            // GIVEN
            StackHasher hasher = new StackHasher();

            // WHEN
            Deque<String> hashes = hasher.hexHashes(e);

            // THEN
            assertThat(hashes).hasSize(1);
        }
    }

    @Test
    public void two_hashes_should_be_generated() {
        try {
            StackTraceElementGenerator.generateCausedBy();
            fail("Exception must have been thrown");
        } catch (RuntimeException e) {
            // GIVEN
            StackHasher hasher = new StackHasher();

            // WHEN
            Deque<String> hashes = hasher.hexHashes(e);

            // THEN
            assertThat(hashes).hasSize(2);
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
            fail("Exception must have been thrown");
        } catch (RuntimeException e) {
            // GIVEN
            StackHasher hasher = new StackHasher(new OnlyFromStackTraceElementGeneratorFilter());

            // WHEN
            Deque<String> hashes = hasher.hexHashes(e);

            // THEN
            assertThat(hashes)
                .hasSize(1)
                .first().isEqualTo("ae4a4ab2");
        }
    }
}
