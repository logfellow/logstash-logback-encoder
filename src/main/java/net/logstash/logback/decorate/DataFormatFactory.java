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

import com.fasterxml.jackson.core.TokenStreamFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;

/**
 * Factory for creating builders for a jackson data format.
 *
 * @param <F> Type of TokenStreamFactory
 * @param <FB> Type of TokenStreamFactory.TSFBuilder (that builds the TokenStreamFactory)
 * @param <M> Type of ObjectMapper
 * @param <MB> Type of MapperBuilder (that builds the ObjectMapper)
 */
public interface DataFormatFactory<
        F extends TokenStreamFactory,
        FB extends TokenStreamFactory.TSFBuilder<F, FB>,
        M extends ObjectMapper,
        MB extends MapperBuilder<M,MB>> {

    String JSON = "json";
    String YAML = "yaml";
    String SMILE = "smile";
    String CBOR = "cbor";

    /**
     * Returns the name of the data format.
     * @return the name of the data format.
     */
    String getName();

    /**
     * Creates and returns a new {@link TokenStreamFactory} for this data format.
     *
     * @return a new {@link TokenStreamFactory} for this data format.
     */
    FB createTokenStreamFactoryBuilder();

    /**
     * Creates and returns a new {@link MapperBuilder} for this data format backed by the given factory.
     *
     * @param factory the factory constructed from the builder returned by {@link #createTokenStreamFactoryBuilder()} (perhaps decorated with other configuration)
     * @return a new {@link MapperBuilder} for this data format backed by the given factory.
     */
    MB createMapperBuilder(F factory);
}
