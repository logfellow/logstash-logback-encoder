/**
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
package net.logstash.logback.dataformat.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.core.json.JsonFactoryBuilder;

public class JsonFactoryFeatureDecoratorTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private JsonFactoryBuilder jsonFactoryBuilder;

    @Test
    public void test() {
        JsonFactoryFeatureDecorator decorator = new JsonFactoryFeatureDecorator();
        decorator.addDisable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES.name());
        decorator.addEnable(JsonFactory.Feature.FAIL_ON_SYMBOL_HASH_OVERFLOW.name());

        when(jsonFactoryBuilder.configure(any(JsonFactory.Feature.class), anyBoolean())).thenReturn(jsonFactoryBuilder);

        JsonFactoryBuilder decoratedFactory = decorator.decorate(jsonFactoryBuilder);

        verify(jsonFactoryBuilder).configure(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES, false);
        verify(jsonFactoryBuilder).configure(JsonFactory.Feature.FAIL_ON_SYMBOL_HASH_OVERFLOW, true);

        assertThat(decoratedFactory).isSameAs(jsonFactoryBuilder);
    }

}