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
package net.logstash.logback.dataformat.smile;

import net.logstash.logback.dataformat.DataFormatFactory;

import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileFactoryBuilder;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper;

/**
 * A {@link DataFormatFactory} for the Smile data format.
 *
 * <p>See also {@link SmileGeneratorFeatureDecorator} for configuring {@link SmileGenerator} features.</p>
 */
public class SmileDataFormatFactory implements DataFormatFactory<SmileFactory, SmileFactoryBuilder, SmileMapper, SmileMapper.Builder> {

    @Override
    public String getName() {
        return SMILE;
    }

    @Override
    public SmileFactoryBuilder createTokenStreamFactoryBuilder() {
        return SmileFactory.builder();
    }

    @Override
    public SmileMapper.Builder createMapperBuilder(SmileFactory factory) {
        return SmileMapper.builder(factory);
    }
}
