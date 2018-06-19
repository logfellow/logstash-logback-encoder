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
package net.logstash.logback.argument;


import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.helpers.MessageFormatter;

@RunWith(Parameterized.class)
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

    @Parameterized.Parameters(name = "{0}: {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"null" , "null", null},
                {"Array of primitive" , "[0, 1, 2, 3]", new int[]{0, 1, 2, 3}},
                {"Array of object" , "[a, b, c, d]", new String[]{"a", "b", "c", "d"}},
                {"Nested array" , "[[a, b, c, d], 1, 2, [0, 1, 2, 3]]", new Object[]{new String[]{"a", "b", "c", "d"}, "1", "2", new int[]{0, 1, 2, 3}}},
                {"Exception" , "[FAILED toString()]" , new BuguyToString()},
        });
    }

    @Parameterized.Parameter(0)
    public String testName;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameter(2)
    public Object arg;

    @Test
    public void testToString() throws Exception {
        Assert.assertEquals(expected, StructuredArguments.toString(arg));
        Assert.assertEquals(MessageFormatter.format("{}", arg).getMessage() , StructuredArguments.toString(arg));
    }

}
