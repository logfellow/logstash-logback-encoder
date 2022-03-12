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

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Decorates the {@link JsonGenerator} used for serializing json.
 * <p>
 * Allows you to customize the {@link JsonGenerator}.
 */
public interface JsonGeneratorDecorator {

    /**
     * Decorates the given generator, and returns the decorated generator.
     *
     * <p>The returned decorator does not need to be the same object as the given generator.</p>
     *
     * @param generator the generator to decorate
     * @return the decorated generator
     */
    JsonGenerator decorate(JsonGenerator generator);

}
