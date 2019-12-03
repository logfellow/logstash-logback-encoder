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

import net.logstash.logback.decorate.FeatureDecorator;
import net.logstash.logback.decorate.TokenStreamFactoryBuilderDecorator;

import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.core.json.JsonFactoryBuilder;

/**
 * A {@link TokenStreamFactoryBuilderDecorator} that allows enabling/disabling of {@link JsonFactory.Feature} features.
 */
public class JsonFactoryFeatureDecorator
        extends FeatureDecorator<JsonFactoryBuilder, JsonFactory.Feature>
        implements TokenStreamFactoryBuilderDecorator<JsonFactory, JsonFactoryBuilder> {

    public JsonFactoryFeatureDecorator() {
        super(JsonFactory.Feature.class);
    }

    @Override
    protected JsonFactoryBuilder configure(JsonFactoryBuilder builder, JsonFactory.Feature feature, boolean state) {
        return builder.configure(feature, state);
    }
}
