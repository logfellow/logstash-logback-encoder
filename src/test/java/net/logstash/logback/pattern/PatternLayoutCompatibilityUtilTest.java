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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests the type-name branches in {@link PatternLayoutCompatibilityUtil} without a shaded Logback jar.
 */
public class PatternLayoutCompatibilityUtilTest {

    private static final String UNSHADED_SUPPLIER =
            "java.util.function.Supplier<ch.qos.logback.core.pattern.DynamicConverter>";

    private static final String RELOCATED_SUPPLIER =
            "java.util.function.Supplier<org.apache.juli.logging.ch.qos.logback.core.pattern.DynamicConverter>";

    private static final String CLASS_NAME = "java.lang.String";

    private static final String SUPPLIER_OF_STRING = "java.util.function.Supplier<java.lang.String>";

    @Test
    public void unshadedSupplierIsSupplierBranch() {
        assertThat(PatternLayoutCompatibilityUtil.isSupplierOfDynamicConverter(UNSHADED_SUPPLIER)).isTrue();
        assertThat(PatternLayoutCompatibilityUtil.isClassNameConverterMapValue(UNSHADED_SUPPLIER)).isFalse();
    }

    @Test
    public void relocatedSupplierIsSupplierBranch() {
        assertThat(PatternLayoutCompatibilityUtil.isSupplierOfDynamicConverter(RELOCATED_SUPPLIER)).isTrue();
        assertThat(PatternLayoutCompatibilityUtil.isClassNameConverterMapValue(RELOCATED_SUPPLIER)).isFalse();
    }

    @Test
    public void stringValueIsClassNameBranch() {
        assertThat(PatternLayoutCompatibilityUtil.isClassNameConverterMapValue(CLASS_NAME)).isTrue();
        assertThat(PatternLayoutCompatibilityUtil.isSupplierOfDynamicConverter(CLASS_NAME)).isFalse();
    }

    @Test
    public void supplierOfStringFallsThroughToIncompatibleClassChange() {
        assertThat(PatternLayoutCompatibilityUtil.isClassNameConverterMapValue(SUPPLIER_OF_STRING)).isFalse();
        assertThat(PatternLayoutCompatibilityUtil.isSupplierOfDynamicConverter(SUPPLIER_OF_STRING)).isFalse();
    }

    @Test
    public void supplierOfAnotherDynamicConverterNameIsNotTheSupplierBranch() {
        assertThat(PatternLayoutCompatibilityUtil.isSupplierOfDynamicConverter(
                "java.util.function.Supplier<ch.qos.logback.core.pattern.DynamicConverterFactory>")).isFalse();
        assertThat(PatternLayoutCompatibilityUtil.isSupplierOfDynamicConverter(
                "java.util.function.Supplier<com.DynamicConverter.Other>")).isFalse();
        assertThat(PatternLayoutCompatibilityUtil.isSupplierOfDynamicConverter(
                "java.util.function.Supplier<java.util.Map<java.lang.String, ch.qos.logback.core.pattern.DynamicConverter>>")).isFalse();
    }
}
