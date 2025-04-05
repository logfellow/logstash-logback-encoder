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

import com.fasterxml.jackson.core.JsonFactory;

/**
 * A {@link JsonFactoryDecorator} that allows enabling/disabling of {@link JsonFactory} features.
 */
public class FeatureJsonFactoryDecorator extends FeatureDecorator<JsonFactory, JsonFactory.Feature> implements JsonFactoryDecorator {

    public FeatureJsonFactoryDecorator() {
        super(JsonFactory.Feature.class, JsonFactory::enable, JsonFactory::disable);
    }
}
