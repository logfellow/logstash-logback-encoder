/*
 * Copyright 2013-2022 the original author or authors.
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
package net.logstash.logback.decorate;

import net.logstash.logback.composite.AbstractCompositeJsonFormatter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MappingJsonFactory;

/**
 * Decorates the {@link JsonFactory} used by a
 * {@link AbstractCompositeJsonFormatter}.
 * <p>
 * This allows you to customize the factory used by the formatters.
 * <p>
 * Implementations must be idempotent.
 * The decorator configured on a formatter is called each time a formatter is started,
 * and there is no way to 'un-decorate' the factory when when formatter is stopped.
 * So, the factory could be decorated multiple times if the formatter is restarted.
 */
public interface JsonFactoryDecorator {

    /**
     * Decorates the given {@link JsonFactory}.
     *
     * <p>By default, returns the given factory unchanged.</p>
     *
     * <p>Note that the default {@link JsonFactory} created by logstash-logback-encoder
     * is a {@link MappingJsonFactory}, but can be changed by {@link JsonFactoryDecorator}s
     * to any subclass of {@link JsonFactory}.</p>
     *
     * @param factory the factory to decorate
     * @return the decorated {@link JsonFactory}
     */
    default JsonFactory decorate(JsonFactory factory) {
        return factory;
    }

}
