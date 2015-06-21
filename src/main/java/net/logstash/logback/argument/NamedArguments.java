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
import java.util.Map;

/**
 * {@link NamedArgument} factory helper
 */
public class NamedArguments {

    public static final String DEFAULT_FORMAT = "{0}={1}";

    private NamedArguments() {
    }

    /**
     * Construct a {@link FormattedNamedArgument} using the provided arguments
     * @param key cannot be null
     * @param value
     * @param format cannot be null
     * @return
     */
    public static NamedArgument keyValue(String key, Object value, String format) {
        return new FormattedNamedArgument(key, value, format);
    }

    /**
     * Synonym of {@link #keyValue(String, Object, String)}
     * @param key
     * @param value
     * @param format
     * @return
     */
    public static NamedArgument kv(String key, Object value, String format) {
        return keyValue(key, value, format);
    }

    /**
     * Synonym of {@link #keyValue(String, Object, String)} with format equals to {@link #DEFAULT_FORMAT}
     * @param key
     * @param value
     * @return
     */
    public static NamedArgument keyValue(String key, Object value) {
        return keyValue(key, value, DEFAULT_FORMAT);
    }

    /**
     * Synonym of {@link #keyValue(String, Object)}
     * @param key
     * @param value
     * @return
     */
    public static NamedArgument kv(String key, Object value) {
        return keyValue(key, value);
    }

    /**
     * Construct a {@link DefaultNamedArgument} using the provided arguments
     * @param key cannot be null
     * @param value
     * @return
     */
    public static NamedArgument value(String key, Object value) {
        return new DefaultNamedArgument(key, value);
    }

    /**
     * Synonym of {@link #value(String, Object)}
     * @param key
     * @param value
     * @return
     */
    public static NamedArgument v(String key, Object value) {
        return value(key, value);
    }


    /**
     * Format the argument into a string. This class mimic the slf4j behaviour:
     * array objects are formatted as array using {@link Arrays#toString}, non array object using {@link String#valueOf}. <br/><br/>
     *
     * See {@link org.slf4j.helpers.MessageFormatter#deeplyAppendParameter(StringBuilder, Object, Map)}.
     *
     * @param arg
     * @return
     */
    public static String toString(Object arg) {

        if (arg == null) {
            return "null";
        }

        Class argClass = arg.getClass();

        try {
            if (!argClass.isArray()) {
                return String.valueOf(arg);
            } else {

                if (argClass == byte[].class) {
                    return Arrays.toString((byte[]) arg);
                } else if (argClass == short[].class) {
                    return Arrays.toString((short[]) arg);
                } else if (argClass == int[].class) {
                    return Arrays.toString((int[]) arg);
                } else if (argClass == long[].class) {
                    return Arrays.toString((long[]) arg);
                } else if (argClass == char[].class) {
                    return Arrays.toString((char[]) arg);
                } else if (argClass == float[].class) {
                    return Arrays.toString((float[]) arg);
                } else if (argClass == double[].class) {
                    return Arrays.toString((double[]) arg);
                } else if (argClass == boolean[].class) {
                    return Arrays.toString((boolean[]) arg);
                } else {
                    return Arrays.deepToString((Object[]) arg);
                }
            }

        } catch (Exception e) {
            System.err.println("Failed toString() invocation on an object of type [" + argClass.getName() + "]");
            e.printStackTrace();
            return "[FAILED toString()]";
        }
    }
}
