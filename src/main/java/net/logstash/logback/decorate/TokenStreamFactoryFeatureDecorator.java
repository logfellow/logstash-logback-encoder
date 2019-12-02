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

/**
 * A {@link TokenStreamFactoryBuilderDecorator} that allows enabling/disabling of {@link TokenStreamFactory.Feature} features.
 */
public class TokenStreamFactoryFeatureDecorator<F extends TokenStreamFactory, B extends TokenStreamFactory.TSFBuilder<F, B>>
        extends FeatureDecorator<B, TokenStreamFactory.Feature>
        implements TokenStreamFactoryBuilderDecorator<F, B> {

    public TokenStreamFactoryFeatureDecorator() {
        super(TokenStreamFactory.Feature.class);
    }

    @Override
    protected B configure(B builder, TokenStreamFactory.Feature feature, boolean state) {
        return builder.configure(feature, state);
    }
}
