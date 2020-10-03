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
package net.logstash.logback.dataformat.cbor;

import net.logstash.logback.decorate.FeatureDecorator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;

import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;

/**
 * A {@link JsonGeneratorDecorator} that allows enabling/disabling of {@link CBORGenerator.Feature} features.
 *
 * <p>Only valid for {@link CBORGenerator}s.
 * Use in conjunction with {@link CborDataFormatFactory}.</p>
 */
public class CborGeneratorFeatureDecorator
        extends FeatureDecorator<CBORGenerator, CBORGenerator.Feature>
        implements JsonGeneratorDecorator<CBORGenerator> {

    public CborGeneratorFeatureDecorator() {
        super(CBORGenerator.Feature.class);
    }

    @Override
    protected CBORGenerator configure(CBORGenerator generator, CBORGenerator.Feature feature, boolean state) {
        return state
                ? generator.enable(feature)
                : generator.disable(feature);
    }
}
