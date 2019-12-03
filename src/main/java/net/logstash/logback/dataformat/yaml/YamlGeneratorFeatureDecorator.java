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
package net.logstash.logback.dataformat.yaml;

import net.logstash.logback.decorate.FeatureDecorator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;

import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

/**
 * A {@link JsonGeneratorDecorator} that allows enabling/disabling of {@link YAMLGenerator.Feature} features.
 *
 * <p>Only valid for {@link YAMLGenerator}s.
 * Use in conjunction with {@link YamlDataFormatFactory}.</p>
 */
public class YamlGeneratorFeatureDecorator
        extends FeatureDecorator<YAMLGenerator, YAMLGenerator.Feature>
        implements JsonGeneratorDecorator<YAMLGenerator> {

    public YamlGeneratorFeatureDecorator() {
        super(YAMLGenerator.Feature.class);
    }

    @Override
    protected YAMLGenerator configure(YAMLGenerator generator, YAMLGenerator.Feature feature, boolean state) {
        return generator.configure(feature, state);
    }
}
