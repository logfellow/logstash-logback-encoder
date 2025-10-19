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
package net.logstash.logback.decorate.cbor;

import net.logstash.logback.decorate.FeatureDecorator;
import net.logstash.logback.decorate.TokenStreamFactoryBuilderDecorator;

import tools.jackson.dataformat.cbor.CBORFactory;
import tools.jackson.dataformat.cbor.CBORFactoryBuilder;
import tools.jackson.dataformat.cbor.CBORWriteFeature;

/**
 * A {@link TokenStreamFactoryBuilderDecorator} that allows enabling/disabling of {@link CBORWriteFeature} features.
 */
public class CborWriteFeatureDecorator
        extends FeatureDecorator<CBORFactoryBuilder, CBORWriteFeature>
        implements TokenStreamFactoryBuilderDecorator<CBORFactory, CBORFactoryBuilder> {

    public CborWriteFeatureDecorator() {
        super(CBORWriteFeature.class);
    }

    @Override
    protected CBORFactoryBuilder configure(CBORFactoryBuilder builder, CBORWriteFeature feature, boolean state) {
        return builder.configure(feature, state);
    }
}
