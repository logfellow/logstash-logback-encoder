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
package net.logstash.logback.argument;


import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.helpers.MessageFormatter;

public class StructuredArgumentsToStringTest {

    private static  class BuguyToString {
        @Override
        public String toString() {
            throw new NullPointerException("npe") {
                @Override
                public synchronized Throwable fillInStackTrace() {
                    return this;
                }
            };
        }
    }

    private static Stream<Arguments> data() {
        return Stream.of(
                arguments("null", null),
                arguments("[0, 1, 2, 3]", new int[]{0, 1, 2, 3}),
                arguments("[a, b, c, d]", new String[]{"a", "b", "c", "d"}),
                arguments("[[a, b, c, d], 1, 2, [0, 1, 2, 3]]", new Object[]{new String[]{"a", "b", "c", "d"}, "1", "2", new int[]{0, 1, 2, 3}}),
                arguments("[FAILED toString()]" , new BuguyToString())
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testToString(String expected, Object arg) throws Exception {
        Assertions.assertEquals(expected, StructuredArguments.toString(arg));
        Assertions.assertEquals(MessageFormatter.format("{}", arg).getMessage() , StructuredArguments.toString(arg));
    }

}
