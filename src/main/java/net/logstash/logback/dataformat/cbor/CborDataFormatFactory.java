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

import net.logstash.logback.dataformat.DataFormatFactory;

import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORFactoryBuilder;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

/**
 * A {@link DataFormatFactory} for the CBOR data format.
 *
 * <p>See also {@link CborGeneratorFeatureDecorator} for configuring {@link CBORGenerator} features.</p>
 */
public class CborDataFormatFactory implements DataFormatFactory<CBORFactory, CBORFactoryBuilder, CBORMapper, CBORMapper.Builder> {

    @Override
    public String getName() {
        return CBOR;
    }

    @Override
    public CBORFactoryBuilder createTokenStreamFactoryBuilder() {
        return CBORFactory.builder();
    }

    @Override
    public CBORMapper.Builder createMapperBuilder(CBORFactory factory) {
        return CBORMapper.builder(factory);
    }
}
