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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;

/**
 * Enables pretty printing on the {@link MapperBuilder}
 */
public class PrettyPrintingMapperBuilderDecorator<M extends ObjectMapper, B extends MapperBuilder<M, B>> implements MapperBuilderDecorator<M, B> {

    @Override
    public B decorate(B builder) {
        return builder.enable(SerializationFeature.INDENT_OUTPUT);
    }
}
