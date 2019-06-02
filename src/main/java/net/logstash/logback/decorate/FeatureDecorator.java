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
package net.logstash.logback.decorate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import com.fasterxml.jackson.core.JsonFactory;

/**
 * A generic decorator that allows enabling/disabling of Jackson features.
 *
 * @param <T> Type of object being decorated (e.g. {@link JsonFactory})
 * @param <E> Feature enum type (e.g. {@link JsonFactory.Feature})
 */
public class FeatureDecorator<T, F extends Enum<F>> {

    /**
     * Jackson feature enum type (e.g. {@link JsonFactory.Feature})
     */
    private final Class<F> enumType;
    /**
     * Function to enable a feature.
     */
    private final BiFunction<T, F, T> enableFunction;
    /**
     * Function to disable a feature.
     */
    private final BiFunction<T, F, T> disableFunction;

    /**
     * Features to enable
     */
    private final List<F> enables = new ArrayList<>();
    /**
     * Features to disable
     */
    private final List<F> disables = new ArrayList<>();

    protected FeatureDecorator(
            Class<F> enumType,
            BiFunction<T, F, T> enableFunction,
            BiFunction<T, F, T> disableFunction) {
        this.enumType = enumType;
        this.enableFunction = enableFunction;
        this.disableFunction = disableFunction;
    }

    public T decorate(T target) {
        T modifiedTarget = target;
        for (F feature : enables) {
            modifiedTarget = enableFunction.apply(modifiedTarget, feature);
        }
        for (F feature : disables) {
            modifiedTarget = disableFunction.apply(modifiedTarget, feature);
        }
        return modifiedTarget;
    }

    /**
     * Enables the feature with the given name.
     * Reflectively called by logback when reading xml configuration.
     * @param feature the name of the feature to enable
     */
    public void addEnable(String feature) {
        enable(Enum.valueOf(enumType, feature));
    }

    /**
     * Enables the given feature.
     * Use this method for programmatic configuration.
     * @param feature the feature to enable
     */
    public void enable(F feature) {
        enables.add(feature);
    }
    /**
     * Disables the feature with the given name.
     * Reflectively called by logback when reading xml configuration.
     * @param feature the name of the feature to disable
     */
    public void addDisable(String feature) {
        disable(Enum.valueOf(enumType, feature));
    }
    /**
     * Disables the given feature.
     * Use this method for programmatic configuration.
     * @param feature the feature to disable
     */
    public void disable(F feature) {
        disables.add(feature);
    }
}
