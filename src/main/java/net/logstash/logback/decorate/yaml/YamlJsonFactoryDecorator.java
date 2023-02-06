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
package net.logstash.logback.decorate.yaml;

import net.logstash.logback.decorate.JsonFactoryDecorator;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

/**
 * A {@link JsonFactoryDecorator} that will switch the output
 * to YAML output instead of JSON text.
 *
 * <p>See also {@link YamlFeatureJsonGeneratorDecorator} for configuring {@link YAMLGenerator} features.</p>
 */
public class YamlJsonFactoryDecorator implements JsonFactoryDecorator {

    @Override
    public JsonFactory decorate(JsonFactory factory) {
        YAMLFactory yamlFactory = new YAMLFactory();
        ObjectMapper mapper = new ObjectMapper(yamlFactory);
        yamlFactory.setCodec(mapper);
        /*
         * YAMLGenerator needs to pass the flush to the stream.
         * It doesn't maintain an internal buffer like the other generators.
         * To see this, look at the .flush() implementations of each of the generator classes.
         */
        yamlFactory.enable(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM);
        return yamlFactory;
    }
}
