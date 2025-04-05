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
package net.logstash.logback.decorate.yaml;

import net.logstash.logback.decorate.FeatureDecorator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

/**
 * A {@link JsonGeneratorDecorator} that allows enabling/disabling of {@link YAMLGenerator} features.
 *
 * <p>Only valid for {@link YAMLGenerator}s.
 * Use in conjunction with {@link YamlJsonFactoryDecorator}.</p>
 */
public class YamlFeatureJsonGeneratorDecorator extends FeatureDecorator<YAMLGenerator, YAMLGenerator.Feature> implements JsonGeneratorDecorator {

    public YamlFeatureJsonGeneratorDecorator() {
        super(YAMLGenerator.Feature.class, YAMLGenerator::enable, YAMLGenerator::disable);
    }

    @Override
    public JsonGenerator decorate(JsonGenerator generator) {
        if (!(generator instanceof YAMLGenerator)) {
            throw new ClassCastException("Expected generator to be of type " + YAMLGenerator.class.getName() + ".  See " + YamlJsonFactoryDecorator.class.getName());
        }
        return super.decorate((YAMLGenerator) generator);
    }
}
