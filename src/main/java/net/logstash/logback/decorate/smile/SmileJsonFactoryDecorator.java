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
package net.logstash.logback.decorate.smile;

import net.logstash.logback.decorate.JsonFactoryDecorator;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;

/**
 * A {@link JsonFactoryDecorator} that will switch the output
 * to binary smile output instead of JSON text.
 * 
 * <p>See also {@link SmileFeatureJsonGeneratorDecorator} for configuring {@link SmileGenerator} features.</p>
 */
public class SmileJsonFactoryDecorator implements JsonFactoryDecorator {

    @Override
    public JsonFactory decorate(JsonFactory factory) {
        SmileFactory smileFactory = new SmileFactory();
        ObjectMapper mapper = new ObjectMapper(smileFactory);
        smileFactory.setCodec(mapper);
        return smileFactory;
    }
}
