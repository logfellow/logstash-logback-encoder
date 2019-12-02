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
package net.logstash.logback.decorate.smile;

import net.logstash.logback.decorate.FeatureDecorator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;

/**
 * A {@link JsonGeneratorDecorator} that allows enabling/disabling of {@link SmileGenerator.Feature} features.
 *
 * <p>Only valid for {@link SmileGenerator}s.
 * Use in conjunction with {@link SmileDataFormatFactory}.</p>
 */
public class SmileGeneratorFeatureDecorator
        extends FeatureDecorator<SmileGenerator, SmileGenerator.Feature>
        implements JsonGeneratorDecorator<SmileGenerator> {

    public SmileGeneratorFeatureDecorator() {
        super(SmileGenerator.Feature.class);
    }

    @Override
    protected SmileGenerator configure(SmileGenerator generator, SmileGenerator.Feature feature, boolean state) {
        return generator.configure(feature, state);
    }
}
