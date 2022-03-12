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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Enables the {@link com.fasterxml.jackson.core.JsonGenerator.Feature#ESCAPE_NON_ASCII} feature on the {@link JsonFactory}.
 * 
 * Prior to 5.0, {@link com.fasterxml.jackson.core.JsonGenerator.Feature#ESCAPE_NON_ASCII} was enabled by default.
 * In 5.0, the feature is disabled by default, and can be re-enabled with this decorator.
 */
public class EscapeNonAsciiJsonFactoryDecorator implements JsonFactoryDecorator {

    @Override
    public JsonFactory decorate(JsonFactory factory) {
        return factory.enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);
    }

}
