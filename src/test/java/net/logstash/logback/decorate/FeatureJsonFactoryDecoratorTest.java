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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonFactory;

@ExtendWith(MockitoExtension.class)
public class FeatureJsonFactoryDecoratorTest {

    @Mock
    private JsonFactory jsonFactory;

    @Test
    public void test() {
        FeatureJsonFactoryDecorator decorator = new FeatureJsonFactoryDecorator();
        decorator.addDisable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES.name());
        decorator.addEnable(JsonFactory.Feature.FAIL_ON_SYMBOL_HASH_OVERFLOW.name());

        when(jsonFactory.enable(any(JsonFactory.Feature.class))).thenReturn(jsonFactory);
        when(jsonFactory.disable(any(JsonFactory.Feature.class))).thenReturn(jsonFactory);

        JsonFactory decoratedFactory = decorator.decorate(jsonFactory);

        verify(jsonFactory).disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES);
        verify(jsonFactory).enable(JsonFactory.Feature.FAIL_ON_SYMBOL_HASH_OVERFLOW);

        assertThat(decoratedFactory).isSameAs(jsonFactory);
    }

}