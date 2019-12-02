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
package net.logstash.logback.decorate;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;

/**
 * A {@link MapperBuilderDecorator} that allows enabling/disabling of {@link MapperFeature} features.
 */
public class MapperFeatureDecorator<M extends ObjectMapper, B extends MapperBuilder<M,B>>
        extends FeatureDecorator<B, MapperFeature>
        implements MapperBuilderDecorator<M, B> {

    public MapperFeatureDecorator() {
        super(MapperFeature.class);
    }

    @Override
    protected B configure(B builder, MapperFeature feature, boolean state) {
        return builder.configure(feature, state);
    }
}
