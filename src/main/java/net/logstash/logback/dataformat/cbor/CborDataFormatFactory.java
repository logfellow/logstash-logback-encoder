/*
 * Copyright 2013-2025 the original author or authors.
 *
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
import net.logstash.logback.decorate.TokenStreamFactoryFeatureDecorator;
import net.logstash.logback.decorate.cbor.CborWriteFeatureDecorator;

import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.dataformat.cbor.CBORFactory;
import tools.jackson.dataformat.cbor.CBORFactoryBuilder;
import tools.jackson.dataformat.cbor.CBORMapper;
import tools.jackson.dataformat.cbor.CBORWriteFeature;

/**
 * A {@link DataFormatFactory} for the CBOR data format.
 *
 * <p>See also {@link CborWriteFeatureDecorator} for configuring {@link CBORWriteFeature}s,
 * and {@link TokenStreamFactoryFeatureDecorator} for configuring {@link TokenStreamFactory.Feature}s</p>
 */
public class CborDataFormatFactory implements DataFormatFactory<CBORFactory, CBORFactoryBuilder, CBORMapper, CBORMapper.Builder> {

    @Override
    public String getName() {
        return CBOR;
    }

    @Override
    public CBORFactoryBuilder createTokenStreamFactoryBuilder() {
        return CBORFactory.builder()
                /*
                 * When CBORGenerator is flushed, flush the internal buffer to the underlying outputStream,
                 * but don't flush the underlying outputStream.
                 *
                 * This allows some streaming optimizations when using an encoder.
                 *
                 * The encoder generally determines when the stream should be flushed
                 * by an 'immediateFlush' property.
                 *
                 * The 'immediateFlush' property of the encoder can be set to false
                 * when the appender performs the flushes at appropriate times
                 * (such as the end of a batch in the AbstractLogstashTcpSocketAppender).
                 */
                .disable(StreamWriteFeature.FLUSH_PASSED_TO_STREAM);
    }

    @Override
    public CBORMapper.Builder createMapperBuilder(CBORFactory factory) {
        return CBORMapper.builder(factory);
    }
}
