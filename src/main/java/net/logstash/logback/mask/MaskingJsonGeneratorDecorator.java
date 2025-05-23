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
package net.logstash.logback.mask;

import static net.logstash.logback.util.StringUtils.commaDelimitedListToStringArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.logstash.logback.decorate.JsonGeneratorDecorator;

import ch.qos.logback.core.spi.LifeCycle;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * A {@link JsonGeneratorDecorator} that wraps a {@link JsonGenerator} with a {@link MaskingJsonGenerator},
 * so that sensitive field values can be masked.
 */
public class MaskingJsonGeneratorDecorator implements JsonGeneratorDecorator, LifeCycle {

    /**
     * Paths to mask with the default mask
     */
    private final PathMask pathsWithDefaultMask = new PathMask();

    /**
     * Suppliers of {@link PathMask}s.
     */
    private final List<PathMaskSupplier> pathMaskSuppliers = new ArrayList<>();

    /**
     * Custom {@link FieldMasker}s.
     */
    private final List<FieldMasker> fieldMaskers = new ArrayList<>();

    /**
     * Values to mask with the default mask
     */
    private final ValueMask valuesWithDefaultMask = new ValueMask();

    /**
     * Values to mask with specific masks.
     */
    private final List<ValueMaskSupplier> valueMaskSuppliers = new ArrayList<>();

    /**
     * Custom {@link ValueMasker}s.
     */
    private final List<ValueMasker> valueMaskers = new ArrayList<>();

    /**
     * The decorator to be used to decorate the {@link JsonGenerator}.
     * Will be updated during {@link #start()}.
     */
    private JsonGeneratorDecorator delegate;

    /**
     * True if this decorator is currently started.
     */
    private boolean started;

    /**
     * Supplies a {@link PathMask} dynamically at runtime.
     *
     * <p>Use this if the list of paths to mask should be determined
     * from somewhere other than the logback configuration.
     * E.g. by dynamically loading them from classes on the classpath.</p>
     */
    public interface PathMaskSupplier extends Supplier<PathMask> {
    }

    /**
     * Paths to mask, and the value to write as the mask.
     */
    public static class PathMask {
        /**
         * The absolute or partial field paths to mask (see {@link PathBasedFieldMasker} for format)
         */
        private final List<String> paths = new ArrayList<>();

        /**
         * The value to write as a mask for the {@link #paths}.
         */
        private String mask = MaskingJsonGenerator.MASK;

        public PathMask() {
        }

        public PathMask(String path) {
            this(path, MaskingJsonGenerator.MASK);
        }

        public PathMask(String path, String mask) {
            this(Collections.singletonList(path), mask);
        }

        public PathMask(List<String> paths) {
            this(paths, MaskingJsonGenerator.MASK);
        }

        public PathMask(List<String> paths, String mask) {
            paths.forEach(this::addPath);
            setMask(mask);
        }

        /**
         * @param path the absolute or partial field path to mask (see {@link PathBasedFieldMasker} for format)
         */
        public void addPath(String path) {
            PathBasedFieldMasker.validatePathToMask(path);
            this.paths.add(path);
        }

        /**
         * @param paths a comma-separated string of absolute or partial field paths to mask (see {@link PathBasedFieldMasker} for format)
         */
        public void addPaths(String paths) {
            for (String path: commaDelimitedListToStringArray(paths)) {
                addPath(path);
            }
        }

        /**
         * @param mask the value to write as a mask for any paths that match the {@link #paths}.
         */
        public void setMask(String mask) {
            this.mask = Objects.requireNonNull(mask);
        }
    }

    /**
     * Supplies a {@link ValueMask} dynamically at runtime.
     *
     * <p>Use this if the list of values to mask should be determined
     * from somewhere other than the logback configuration.
     * E.g. by dynamically loading them from classes on the classpath.</p>
     */
    public interface ValueMaskSupplier extends Supplier<ValueMask> {
    }

    /**
     * Values to mask, and the value to write as the mask.
     */
    public static class ValueMask {
        /**
         * The regexes used to identify values to mask.
         */
        private final List<String> values = new ArrayList<>();

        /**
         * The value to write as a mask for values that match the regex (can contain back references to capture groups in the regex).
         */
        private String mask = MaskingJsonGenerator.MASK;

        public ValueMask() {
        }

        public ValueMask(String values) {
            this(values, MaskingJsonGenerator.MASK);
        }

        public ValueMask(String value, String mask) {
            this(Collections.singletonList(value), mask);
        }

        public ValueMask(List<String> values) {
            this(values, MaskingJsonGenerator.MASK);
        }

        public ValueMask(List<String> values, String mask) {
            values.forEach(this::addValue);
            setMask(mask);
        }

        /**
         * @param value the regex used to identify values to mask
         */
        public void addValue(String value) {
            this.values.add(Objects.requireNonNull(value));
        }

        /**
         * @param values a comma-separated string of regexes to mask
         */
        public void addValues(String values) {
            for (String value: commaDelimitedListToStringArray(values)) {
                addValue(value);
            }
        }

        /**
         * @param mask the value to write as a mask for values that match the {@link #values} regexes
         *             (can contain back references to capture groups in the regex)
         */
        public void setMask(String mask) {
            this.mask = Objects.requireNonNull(mask);
        }
    }

    @Override
    public synchronized boolean isStarted() {
        return started;
    }

    @Override
    public synchronized void start() {
        if (!started) {
            final List<FieldMasker> effectiveFieldMaskers = getEffectiveFieldMaskers();

            final List<ValueMasker> effectiveValueMaskers = getEffectiveValueMaskers();

            if (effectiveFieldMaskers.isEmpty() && effectiveValueMaskers.isEmpty()) {
                delegate = generator -> generator;
            } else {
                delegate = generator -> new MaskingJsonGenerator(generator, effectiveFieldMaskers, effectiveValueMaskers);
            }

            started = true;
        }
    }

    @Override
    public synchronized void stop() {
        started = false;
    }

    private List<FieldMasker> getEffectiveFieldMaskers() {
        Map<String, Set<String>> fieldNamesByMask = new HashMap<>();

        List<FieldMasker> pathFieldMaskers = new ArrayList<>();

        Stream.concat(Stream.of(pathsWithDefaultMask), pathMaskSuppliers.stream().map(Supplier::get))
                .forEach(pathMask ->
                    pathMask.paths.forEach(path -> {
                        String mask = pathMask.mask;
                        if (PathBasedFieldMasker.isSingleFieldName(path)) {
                            /*
                             * Optimize single field name matching by grouping all single field names
                             * in a Set, to be checked by a FieldNameMatcher
                             *
                             * The FieldNameMatcher is much more efficient than a JsonPathMatcher.
                             */
                            fieldNamesByMask
                                    .computeIfAbsent(mask, r -> new HashSet<>())
                                    .add(PathBasedFieldMasker.unescapeJsonPointerToken(path));
                        } else {
                            pathFieldMaskers.add(new PathBasedFieldMasker(path, mask));
                        }
                    })
                );

        return Collections.unmodifiableList(Stream.concat(
                fieldNamesByMask.entrySet().stream()
                        .map(entry -> new FieldNameBasedFieldMasker(entry.getValue(), entry.getKey())),
                Stream.concat(
                        pathFieldMaskers.stream(),
                        fieldMaskers.stream()))
                .collect(Collectors.toList()));
    }

    private List<ValueMasker> getEffectiveValueMaskers() {
        return Collections.unmodifiableList(Stream.concat(
                        Stream.concat(Stream.of(valuesWithDefaultMask), valueMaskSuppliers.stream().map(Supplier::get))
                                .flatMap(valueMask -> valueMask.values.stream()
                                        .map(value -> new RegexValueMasker(value, valueMask.mask))),
                        valueMaskers.stream())
                .collect(Collectors.toList()));

    }

    @Override
    public JsonGenerator decorate(JsonGenerator generator) {
        return delegate.decorate(generator);
    }

    /**
     * Sets the default mask value to use for any paths added via {@link #addPath(String)}
     * and values added via {@link #addValue(String)}.
     *
     * <p>By default, this is {@value MaskingJsonGenerator#MASK}.</p>
     *
     * @param defaultMask the default mask value to be used to mask real values.
     */
    public void setDefaultMask(String defaultMask) {
        Objects.requireNonNull(defaultMask, "defaultMask must not be null");
        this.valuesWithDefaultMask.setMask(defaultMask);
        this.pathsWithDefaultMask.setMask(defaultMask);
    }

    /**
     * Adds the given path to the paths that will be masked.
     *
     * <p>The {@link #setDefaultMask(String) default mask} value will be substituted for values at the given path.</p>
     *
     * @param pathToMask the path to mask. See {@link PathBasedFieldMasker} for the format.
     */
    public void addPath(String pathToMask) {
        pathsWithDefaultMask.addPath(pathToMask);
    }

    /**
     * Adds the given comma separated paths to the paths that will be masked.
     *
     * <p>The {@link #setDefaultMask(String) default mask} value will be substituted for values at the given paths.</p>
     *
     * @param pathsToMask comma separate string of paths to mask. See {@link PathBasedFieldMasker} for the format.
     */
    public void addPaths(String pathsToMask) {
        pathsWithDefaultMask.addPaths(pathsToMask);
    }

    /**
     * Adds the given paths and mask that will be used to determine if a field should be masked.
     *
     * <p>The {@link PathMask#setMask(String) mask} value will be written for any of the {@link PathMask#addPath(String) paths}.</p>
     *
     * @param pathMask a paths used to determine if a value should be masked, and their corresponding mask value.
     */
    public void addPathMask(PathMask pathMask) {
        addPathMaskSupplier(() -> pathMask);
    }

    /**
     * Adds the given supplier of paths and mask that will be used to determine if a field should be masked.
     *
     * <p>The {@link PathMask#setMask(String) mask} value will be written for any of the {@link PathMask#addPath(String) paths}.</p>
     *
     * @param pathMaskSupplier a supplier of paths used to determine if a value should be masked, and their corresponding mask value.
     */
    public void addPathMaskSupplier(PathMaskSupplier pathMaskSupplier) {
        this.pathMaskSuppliers.add(pathMaskSupplier);
    }

    /**
     * Add the given {@link FieldMasker} to the maskers used to mask a field.
     *
     * @param fieldMasker the masker to add
     */
    public void addFieldMasker(FieldMasker fieldMasker) {
        fieldMaskers.add(fieldMasker);
    }

    /**
     * Adds the given value regex to the regexes that will be used to determine if a field value should be masked.
     *
     * <p>The {@link #setDefaultMask(String) default mask} value will be substituted for values that match the given regex.</p>
     *
     * @param valueToMask a regular expression used to determine if a value should be masked.
     */
    public void addValue(String valueToMask) {
        valuesWithDefaultMask.addValue(valueToMask);
    }

    /**
     * Adds the comma separated string of value regexes to the regexes that will be used to determine if a field value should be masked.
     *
     * <p>The {@link #setDefaultMask(String) default mask} value will be substituted for values that match the given regexes.</p>
     *
     * @param valuesToMask comma-separated string of regular expressions used to determine if a value should be masked.
     */
    public void addValues(String valuesToMask) {
        valuesWithDefaultMask.addValues(valuesToMask);
    }

    /**
     * Adds the given value regexes and mask to the regexes that will be used to determine if a field value should be masked.
     *
     * <p>The {@link ValueMask#setMask(String) mask} value will be written for values that match any of the {@link ValueMask#addValue(String) value regexes}.</p>
     *
     * @param valueMask regular expressions used to determine if a value should be masked, and their corresponding mask value
     */
    public void addValueMask(ValueMask valueMask) {
        addValueMaskSupplier(() -> valueMask);
    }

    /**
     * Adds the given supplier of value regexes and mask to the regexes that will be used to determine if a field value should be masked.
     *
     * <p>The {@link ValueMask#setMask(String) mask} value will be written for values that match any of the {@link ValueMask#addValue(String) value regexes}.</p>
     *
     * @param valueMaskSupplier a supplier of regular expressions used to determine if a value should be masked, and their corresponding mask value
     */
    public void addValueMaskSupplier(ValueMaskSupplier valueMaskSupplier) {
        valueMaskSuppliers.add(valueMaskSupplier);
    }

    /**
     * Add the given {@link ValueMasker} to the maskers used to mask a value.
     *
     * @param valueMasker the masker to add
     */
    public void addValueMasker(ValueMasker valueMasker) {
        valueMaskers.add(valueMasker);
    }
}
