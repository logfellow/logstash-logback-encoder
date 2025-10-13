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
package net.logstash.logback.decorate;

import net.logstash.logback.composite.AbstractCompositeJsonFormatter;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperBuilder;

/**
 * Decorates the {@link MapperBuilder} used by a
 * {@link AbstractCompositeJsonFormatter} to create an {@link ObjectMapper}.
 * <p>
 * This allows you to customize the mapper used by the formatters.
 * <p>
 * Implementations must be idempotent.
 * The decorator configured on a formatter is called each time a formatter is started,
 * and there is no way to 'un-decorate' the mapper when the formatter is stopped.
 * So, the mapper could be decorated multiple times if the formatter is restarted.
 */
public interface MapperBuilderDecorator<M extends ObjectMapper, B extends MapperBuilder<M, B>> extends Decorator<B> {

}
