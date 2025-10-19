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

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.MapperBuilder;

/**
 * A {@link MapperBuilderDecorator} that allows enabling/disabling of {@link DateTimeFeature} features.
 */
public class DateTimeFeatureDecorator<M extends ObjectMapper, B extends MapperBuilder<M, B>>
        extends FeatureDecorator<B, DateTimeFeature>
        implements MapperBuilderDecorator<M, B> {

    public DateTimeFeatureDecorator() {
        super(DateTimeFeature.class);
    }

    @Override
    protected B configure(B builder, DateTimeFeature feature, boolean state) {
        return builder.configure(feature, state);
    }
}
