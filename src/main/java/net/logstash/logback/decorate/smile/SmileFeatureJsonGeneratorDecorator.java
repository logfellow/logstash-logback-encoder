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
package net.logstash.logback.decorate.smile;

import net.logstash.logback.decorate.FeatureDecorator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;

/**
 * A {@link JsonGeneratorDecorator} that allows enabling/disabling of {@link SmileGenerator} features.
 *
 * <p>Only valid for {@link SmileGenerator}s.
 * Use in conjunction with {@link SmileJsonFactoryDecorator}.</p>
 */
public class SmileFeatureJsonGeneratorDecorator extends FeatureDecorator<SmileGenerator, SmileGenerator.Feature> implements JsonGeneratorDecorator {

    public SmileFeatureJsonGeneratorDecorator() {
        super(SmileGenerator.Feature.class, SmileGenerator::enable, SmileGenerator::disable);
    }

    @Override
    public JsonGenerator decorate(JsonGenerator generator) {
        if (!(generator instanceof SmileGenerator)) {
            throw new ClassCastException("Expected generator to be of type " + SmileGenerator.class.getName() + ".  See " + SmileJsonFactoryDecorator.class.getName());
        }
        return super.decorate((SmileGenerator) generator);
    }
}
