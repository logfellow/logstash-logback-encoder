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
package net.logstash.logback.decorate.cbor;

import net.logstash.logback.decorate.FeatureDecorator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;

/**
 * A {@link JsonGeneratorDecorator} that allows enabling/disabling of {@link CBORGenerator} features.
 *
 * <p>Only valid for {@link CBORGenerator}s.
 * Use in conjunction with {@link CborJsonFactoryDecorator}.</p>
 */
public class CborFeatureJsonGeneratorDecorator extends FeatureDecorator<CBORGenerator, CBORGenerator.Feature> implements JsonGeneratorDecorator {

    public CborFeatureJsonGeneratorDecorator() {
        super(CBORGenerator.Feature.class, CBORGenerator::enable, CBORGenerator::disable);
    }

    @Override
    public JsonGenerator decorate(JsonGenerator generator) {
        if (!(generator instanceof CBORGenerator)) {
            throw new ClassCastException("Expected generator to be of type " + CBORGenerator.class.getName() +".  See " + CborJsonFactoryDecorator.class.getName());
        }
        return super.decorate((CBORGenerator) generator);
    }
}
