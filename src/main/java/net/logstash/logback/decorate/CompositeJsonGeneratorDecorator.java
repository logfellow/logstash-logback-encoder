/*
 * Copyright 2013-2021 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Combines a list of decorators into a single decorator, so multiple decorators can be used together.
 */
public class CompositeJsonGeneratorDecorator implements JsonGeneratorDecorator {
    
    private final List<JsonGeneratorDecorator> decorators = new ArrayList<>();
    
    @Override
    public JsonGenerator decorate(JsonGenerator generator) {
        JsonGenerator decoratedGenerator = generator;
        for (JsonGeneratorDecorator decorator : decorators) {
            decoratedGenerator = decorator.decorate(decoratedGenerator);
        }
        return decoratedGenerator;
    }
    
    public void addDecorator(JsonGeneratorDecorator decorator) {
        decorators.add(decorator);
    }
    
    public boolean removeDecorator(JsonGeneratorDecorator decorator) {
        return decorators.remove(decorator);
    }

}
