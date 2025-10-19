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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonFactoryBuilder;

public class TokenStreamFactoryFeatureDecoratorTest {

    @Test
    public void test() {
        TokenStreamFactoryFeatureDecorator<JsonFactory, JsonFactoryBuilder> decorator = new TokenStreamFactoryFeatureDecorator<>();
        decorator.addDisable(TokenStreamFactory.Feature.CANONICALIZE_PROPERTY_NAMES.name());
        decorator.addEnable(TokenStreamFactory.Feature.FAIL_ON_SYMBOL_HASH_OVERFLOW.name());


        JsonFactoryBuilder builder = JsonFactory.builder()
                .enable(TokenStreamFactory.Feature.CANONICALIZE_PROPERTY_NAMES)
                .disable(TokenStreamFactory.Feature.FAIL_ON_SYMBOL_HASH_OVERFLOW);

        JsonFactory jsonFactory = decorator.decorate(builder).build();

        assertThat(jsonFactory.isEnabled(TokenStreamFactory.Feature.CANONICALIZE_PROPERTY_NAMES)).isFalse();
        assertThat(jsonFactory.isEnabled(TokenStreamFactory.Feature.FAIL_ON_SYMBOL_HASH_OVERFLOW)).isTrue();
    }
}
