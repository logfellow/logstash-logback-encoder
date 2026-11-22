/*
 * Copyright 2013-2025 the original author or authors.
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
package net.logstash.logback.pattern;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Supplier;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.DynamicConverter;
import ch.qos.logback.core.pattern.PatternLayoutBase;

/**
 * Utilities for working around backwards incompatibilities introduced in logback's
 * PatternLayout related classes to maintain support for multiple logback versions.
 *
 * <p>This can be removed in a major version of logstash-logback-encoder,
 * which bumps the minimum logback version to >= 1.5.13.</p>
 */
class PatternLayoutCompatibilityUtil {

    /**
     * Registers the given converter in the layout's {@link PatternLayoutBase#getInstanceConverterMap()}.
     *
     * @param layout the layout to which to add the converter
     * @param converterName the name of the converter
     * @param converterClass the class of the converter
     * @param converterSupplier a supplier of instances of the converter
     * @param <E> type of object converted by the converter
     * @param <C> type of converter
     */
    static <E, C extends DynamicConverter<E>> void registerConverter(
            PatternLayoutBase<ILoggingEvent> layout,
            String converterName,
            Class<C> converterClass,
            Supplier<C> converterSupplier) {

        try {
            Method method = PatternLayoutBase.class.getDeclaredMethod("getInstanceConverterMap");
            ParameterizedType type = (ParameterizedType) method.getGenericReturnType();
            Type valueType = type.getActualTypeArguments()[1];
            String valueTypeName = valueType.getTypeName();

            if (isClassNameConverterMapValue(valueTypeName)) {
                /*
                 * In logback <= 1.5.12, getInstanceConverterMap() is a Map<String, String>.
                 *     key = converter name
                 *     value = name of converter class
                 */
                putUnchecked(layout.getInstanceConverterMap(), converterName, converterClass.getName());
            } else if (isSupplierOfDynamicConverter(valueTypeName)) {
                /*
                 * In logback >= 1.5.13, getInstanceConverterMap() is a
                 * Map<String, Supplier<DynamicConverter>>.
                 *     key = converter name
                 *     value = supplier of converter instances
                 *
                 * Shaded Logback relocates ch.qos.logback, so the type argument's
                 * package can differ. Match a Supplier whose single raw type
                 * argument's simple name is DynamicConverter.
                 *
                 * Related issues:
                 *     https://github.com/qos-ch/logback/issues/803
                 *     https://github.com/qos-ch/logback/issues/931
                 *     https://github.com/qos-ch/logback/issues/932
                 *     https://github.com/logfellow/logstash-logback-encoder/issues/1113
                 */
                putUnchecked(layout.getInstanceConverterMap(), converterName, converterSupplier);
            } else {
                throw new IncompatibleClassChangeError(
                        "Unexpected return type found for PatternLayoutBase.getInstanceConverterMap(), logback-core dependency version is not compatible");
            }

        } catch (NoSuchMethodException e) {
            throw new IncompatibleClassChangeError(
                    "Method PatternLayoutBase.getInstanceConverterMap() not found, logback-core dependency version is not compatible");
        }
    }

    /**
     * Logback &lt;= 1.5.12 declares the instance converter map value as a class name.
     *
     * @param valueTypeName {@link Type#getTypeName()} of the map value
     * @return {@code true} when the value is {@code java.lang.String}
     */
    static boolean isClassNameConverterMapValue(String valueTypeName) {
        return "java.lang.String".equals(valueTypeName);
    }

    /**
     * Returns whether {@code valueTypeName} is a {@link Supplier} of one raw {@link DynamicConverter}.
     *
     * <p>The unshaded Logback 1.5.13+ name is
     * {@code java.util.function.Supplier<ch.qos.logback.core.pattern.DynamicConverter>}.
     * A relocated name, such as
     * {@code java.util.function.Supplier<org.apache.juli.logging.ch.qos.logback.core.pattern.DynamicConverter>},
     * is the same branch. Another {@link Supplier}, including {@code Supplier<String>}, is not.</p>
     *
     * @param valueTypeName {@link Type#getTypeName()} of the map value
     * @return {@code true} when the value should be registered as a converter supplier
     */
    static boolean isSupplierOfDynamicConverter(String valueTypeName) {
        final String prefix = "java.util.function.Supplier<";
        if (valueTypeName == null
                || !valueTypeName.startsWith(prefix)
                || !valueTypeName.endsWith(">")) {
            return false;
        }

        String typeArgument = valueTypeName.substring(prefix.length(), valueTypeName.length() - 1);
        int simpleNameStart = typeArgument.lastIndexOf('.') + 1;
        if (!"DynamicConverter".equals(typeArgument.substring(simpleNameStart))) {
            return false;
        }
        return isRawClassPackage(typeArgument.substring(0, simpleNameStart));
    }

    /**
     * @param packagePrefix empty when the type has no package, otherwise package segments ending in {@code .}
     */
    private static boolean isRawClassPackage(String packagePrefix) {
        if (packagePrefix.isEmpty()) {
            return true;
        }
        if (packagePrefix.charAt(packagePrefix.length() - 1) != '.') {
            return false;
        }
        String[] segments = packagePrefix.substring(0, packagePrefix.length() - 1).split("\\.", -1);
        for (String segment : segments) {
            if (!isJavaIdentifier(segment)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isJavaIdentifier(String segment) {
        if (segment.isEmpty() || !Character.isJavaIdentifierStart(segment.charAt(0))) {
            return false;
        }
        for (int i = 1; i < segment.length(); i++) {
            if (!Character.isJavaIdentifierPart(segment.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void putUnchecked(Map map, String key, Object value) {
        map.put(key, value);
    }

}
