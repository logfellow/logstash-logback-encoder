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
package net.logstash.logback.composite.loggingevent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.logstash.logback.fieldnames.LogstashFieldNames;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.KeyValuePair;

@ExtendWith(MockitoExtension.class)
public class KeyValuePairsJsonProviderTest {

    private KeyValuePairsJsonProvider provider = new KeyValuePairsJsonProvider();

    private ByteArrayOutputStream resultStream;
    private JsonGenerator generator;

    @Mock
    private ILoggingEvent event;

    private List<KeyValuePair> keyValuePairs;

    @BeforeEach
    public void setup() throws Exception {
        keyValuePairs = new ArrayList<>();
        keyValuePairs.add(new KeyValuePair("name1", "value1"));
        keyValuePairs.add(new KeyValuePair("name2", 2023));
        keyValuePairs.add(new KeyValuePair("name3", new TestValue()));
        when(event.getKeyValuePairs()).thenReturn(keyValuePairs);
        resultStream = new ByteArrayOutputStream();
        generator = new JsonFactory().createGenerator(resultStream);
        generator.setCodec(new ObjectMapper());
    }

    @Test
    public void testUnwrapped() throws IOException {
        assertThat(generateJson())
                .isEqualTo("{\"name1\":\"value1\",\"name2\":2023,\"name3\":{\"a\":1}}");
    }

    @Test
    public void testWrapped() throws IOException {
        provider.setFieldName("kvp");
        assertThat(generateJson())
                .isEqualTo("{\"kvp\":{\"name1\":\"value1\",\"name2\":2023,\"name3\":{\"a\":1}}}");
    }

    @Test
    public void testWrappedUsingFieldNames() throws IOException {
        LogstashFieldNames fieldNames = new LogstashFieldNames();
        fieldNames.setKeyValuePair("kvp");
        provider.setFieldNames(fieldNames);
        assertThat(generateJson())
                .isEqualTo("{\"kvp\":{\"name1\":\"value1\",\"name2\":2023,\"name3\":{\"a\":1}}}");
    }

    @Test
    public void testInclude() throws IOException {
        provider.setIncludeKeyNames(Collections.singletonList("name1"));

        assertThat(generateJson())
                .isEqualTo("{\"name1\":\"value1\"}");
    }

    @Test
    public void testExclude() throws IOException {
        provider.setExcludeKeyNames(Collections.singletonList("name1"));

        assertThat(generateJson())
                .isEqualTo("{\"name2\":2023,\"name3\":{\"a\":1}}");
    }

    @Test
    public void testAlternativeFieldName() throws IOException {
        provider.addKeyFieldName("name1=alternativeName1");

        assertThat(generateJson())
                .isEqualTo("{\"alternativeName1\":\"value1\",\"name2\":2023,\"name3\":{\"a\":1}}");
    }

    private String generateJson() throws IOException {
        generator.writeStartObject();
        provider.writeTo(generator, event);
        generator.writeEndObject();

        generator.flush();
        return resultStream.toString();
    }

    private class TestValue {
        private final int a = 1;

        public int getA() {
            return a;
        }
    }
}
