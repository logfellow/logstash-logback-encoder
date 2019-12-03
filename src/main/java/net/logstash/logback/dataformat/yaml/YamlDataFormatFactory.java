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

import net.logstash.logback.dataformat.DataFormatFactory;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactoryBuilder;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

/**
 * A {@link DataFormatFactory} for the YAML data format.
 *
 * <p>See also {@link YamlGeneratorFeatureDecorator} for configuring {@link YAMLGenerator} features.</p>
 */
public class YamlDataFormatFactory implements DataFormatFactory<YAMLFactory, YAMLFactoryBuilder, YAMLMapper, YAMLMapper.Builder> {

    @Override
    public String getName() {
        return YAML;
    }

    @Override
    public YAMLFactoryBuilder createTokenStreamFactoryBuilder() {
        return YAMLFactory.builder();
    }

    @Override
    public YAMLMapper.Builder createMapperBuilder(YAMLFactory factory) {
        return YAMLMapper.builder(factory);
    }
}
