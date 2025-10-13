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
package net.logstash.logback.dataformat.yaml;

import net.logstash.logback.dataformat.DataFormatFactory;
import net.logstash.logback.decorate.TokenStreamFactoryFeatureDecorator;
import net.logstash.logback.decorate.yaml.YamlWriteFeatureDecorator;

import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLFactoryBuilder;
import tools.jackson.dataformat.yaml.YAMLGenerator;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * A {@link DataFormatFactory} for the YAML data format.
 *
 * <p>See also {@link YamlWriteFeatureDecorator} for configuring {@link YAMLGenerator} features
 * and {@link TokenStreamFactoryFeatureDecorator} for configuring {@link TokenStreamFactory.Feature}s</p>
 */
public class YamlDataFormatFactory implements DataFormatFactory<YAMLFactory, YAMLFactoryBuilder, YAMLMapper, YAMLMapper.Builder> {

    @Override
    public String getName() {
        return YAML;
    }

    @Override
    public YAMLFactoryBuilder createTokenStreamFactoryBuilder() {
        return YAMLFactory.builder()
                /*
                 * YAMLGenerator needs to pass the flush to the stream.
                 * It doesn't maintain an internal buffer like the other generators.
                 * To see this, look at the .flush() implementations of each of the generator classes.
                 */
                .enable(StreamWriteFeature.FLUSH_PASSED_TO_STREAM);
    }

    @Override
    public YAMLMapper.Builder createMapperBuilder(YAMLFactory factory) {
        return YAMLMapper.builder(factory);
    }
}
