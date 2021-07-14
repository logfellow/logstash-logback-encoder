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
package net.logstash.logback.encoder.wrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChainedPayloadConverter implements PayloadConverter {
    private List<PayloadConverter> chain;

    public ChainedPayloadConverter() {
        this.chain = new ArrayList<>();
    }

    public void add(PayloadConverter converter) {
        chain.add(converter);
    }

    public List<PayloadConverter> getChain() {
        return chain;
    }

    public void setChain(List<PayloadConverter> chain) {
        this.chain = chain;
    }

    @Override
    public void start() {
        chain.forEach(PayloadConverter::start);
    }

    @Override
    public void stop() {
        chain.forEach(PayloadConverter::stop);
    }

    @Override
    public boolean isStarted() {
        return chain.stream().allMatch(PayloadConverter::isStarted);
    }

    @Override
    public byte[] convert(byte[] encoded) throws IOException {
        byte[] result = encoded;
        for (PayloadConverter converter : chain) {
            result = converter.convert(result);
        }
        return result;
    }
}
