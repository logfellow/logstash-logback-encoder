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
import java.util.function.Supplier;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.WarnStatus;
import net.logstash.logback.marker.MapEntriesAppendingMarker;
import net.logstash.logback.marker.ObjectAppendingMarker;
import net.logstash.logback.marker.ObjectFieldsAppendingMarker;
import net.logstash.logback.marker.RawJsonAppendingMarker;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating {@link StructuredArgument}s.
 */
public class StructuredArguments {
    
    /**
     * The default message format used when writing key value pairs to the log message.
     */
    public static final String DEFAULT_KEY_VALUE_MESSAGE_FORMAT_PATTERN = "{0}={1}";
    
    /**
     * A message format pattern that will only write
     * the argument value to a log message (i.e. it won't write the key).
     */
    public static final String VALUE_ONLY_MESSAGE_FORMAT_PATTERN = "{1}";
    
    private StructuredArguments() {
    }

    /**
     * Convenience method for calling {@link #keyValue(String, Object, String)}
     * using the {@link #DEFAULT_KEY_VALUE_MESSAGE_FORMAT_PATTERN}.
     * <p>
     * Basically, adds "key":"value" to the JSON event AND
     * name=value to the formatted message.
     *  
     * @see ObjectAppendingMarker
     * @see #DEFAULT_KEY_VALUE_MESSAGE_FORMAT_PATTERN
     */
    public static StructuredArgument keyValue(String key, Object value) {
        return keyValue(key, value, DEFAULT_KEY_VALUE_MESSAGE_FORMAT_PATTERN);
    }

    /**
     * Abbreviated convenience method for calling {@link #keyValue(String, Object)}.
     * 
     * @see ObjectAppendingMarker
     */
    public static StructuredArgument kv(String key, Object value) {
        return keyValue(key, value);
    }

    /**
     * Adds "key":"value" to the JSON event AND
     * name/value to the formatted message using the given messageFormatPattern.
     *  
     * @see ObjectAppendingMarker
     */
    public static StructuredArgument keyValue(String key, Object value, String messageFormatPattern) {
        return new ObjectAppendingMarker(key, value, messageFormatPattern);
    }

    /**
     * Abbreviated convenience method for calling {@link #keyValue(String, Object, String)}.
     * 
     * @see ObjectAppendingMarker
     */
    public static StructuredArgument kv(String key, Object value, String messageFormatPattern) {
        return keyValue(key, value, messageFormatPattern);
    }

    /**
     * Adds "key":"value" to the JSON event AND
     * value to the formatted message (without the key). 
     * 
     * @see ObjectAppendingMarker
     * @see #VALUE_ONLY_MESSAGE_FORMAT_PATTERN
     */
    public static StructuredArgument value(String key, Object value) {
        return keyValue(key, value, VALUE_ONLY_MESSAGE_FORMAT_PATTERN);
    }

    /**
     * Abbreviated convenience method for calling {@link #value(String, Object)}.
     * 
     * @see ObjectAppendingMarker
     */
    public static StructuredArgument v(String key, Object value) {
        return value(key, value);
    }


    /**
     * Adds a "key":"value" entry for each Map entry to the JSON event AND
     * map.toString() to the formatted message.
     * 
     * @see MapEntriesAppendingMarker
     */
    public static StructuredArgument entries(Map<?, ?> map) {
        return new MapEntriesAppendingMarker(map);
    }
    
    /**
     * Abbreviated convenience method for calling {@link #entries(Map)}.
     * 
     * @see MapEntriesAppendingMarker
     */
    public static StructuredArgument e(Map<?, ?> map) {
        return entries(map);
    }
    
    /**
     * Adds a "key":"value" entry for each field in the given object to the JSON event AND
     * object.toString() to the formatted message.
     * 
     * @see ObjectFieldsAppendingMarker
     */
    public static StructuredArgument fields(Object object) {
        return new ObjectFieldsAppendingMarker(object);
    }
    /**
     * Abbreviated convenience method for calling {@link #fields(Object)}.
     * 
     * @see ObjectFieldsAppendingMarker
     */
    public static StructuredArgument f(Object object) {
        return fields(object);
    }
    
    /**
     * Adds a field to the JSON event whose key is fieldName and whose value is a JSON array of objects AND
     * a string version of the array to the formatted message.
     * 
     * @see ObjectAppendingMarker
     */
    public static StructuredArgument array(String fieldName, Object... objects) {
        return new ObjectAppendingMarker(fieldName, objects);
    }
    /**
     * Abbreviated convenience method for calling {@link #array(String, Object...)}.
     * 
     * @see ObjectAppendingMarker
     */
    public static StructuredArgument a(String fieldName, Object... objects) {
        return array(fieldName, objects);
    }
    
    /**
     * Adds the rawJsonValue to the JSON event AND
     * the rawJsonValue to the formatted message.
     * 
     * @see RawJsonAppendingMarker
     */
    public static StructuredArgument raw(String fieldName, String rawJsonValue) {
        return new RawJsonAppendingMarker(fieldName, rawJsonValue);
    }
    /**
     * Abbreviated convenience method for calling {@link #raw(String, String)}.
     * 
     * @see RawJsonAppendingMarker
     */
    public static StructuredArgument r(String fieldName, String rawJsonValue) {
        return raw(fieldName, rawJsonValue);
    }

    /**
     * @see DeferredStructuredArgument
     */
    public static StructuredArgument defer(Supplier<? extends StructuredArgument> structuredArgumentSupplier) {
        return new DeferredStructuredArgument(structuredArgumentSupplier);
    }

    /**
     * Format the argument into a string.
     * <p>
     * This method mimics the slf4j behaviour:
     * array objects are formatted as array using {@link Arrays#toString},
     * non array object using {@link String#valueOf}.
     * <p>
     *
     * see org.slf4j.helpers.MessageFormatter#deeplyAppendParameter(StringBuilder, Object, Map)}.
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
            ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
            if (loggerFactory instanceof Context) {
                Context context = (Context) loggerFactory;
                StatusManager statusManager = context.getStatusManager();
                statusManager.add(new WarnStatus(
                        "Failed toString() invocation on an object of type [" + argClass.getName() + "]",
                        StructuredArguments.class,
                        e));
            } else {
                System.err.println("Failed toString() invocation on an object of type [" + argClass.getName() + "]");
                e.printStackTrace();
            }
            return "[FAILED toString()]";
        }
    }
}
