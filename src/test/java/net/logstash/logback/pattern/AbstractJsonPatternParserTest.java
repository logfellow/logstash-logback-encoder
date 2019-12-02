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
package net.logstash.logback.pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAware;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * @author <a href="mailto:dimas@dataart.com">Dmitry Andrianov</a>
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractJsonPatternParserTest<Event> {

    @Mock
    protected ContextAware contextAware;

    protected ObjectMapper objectMapper;

    private JsonGenerator jsonGenerator;

    private StringWriter buffer = new StringWriter();

    private Event event;

    protected AbstractJsonPatternParser<Event> parser;

    @Mock
    private Context context;

    @Before
    public void setUp() throws Exception {

        event = createEvent();

        given(contextAware.getContext()).willReturn(context);

        objectMapper = JsonMapper.builder().build();
        jsonGenerator = objectMapper.createGenerator(buffer);

        parser = createParser();
    }

    abstract protected Event createEvent();

    abstract protected AbstractJsonPatternParser<Event> createParser();

    private Map<String, Object> parseJson(final String text) throws IOException {
        return objectMapper.readValue(text, new TypeReference<Map<String, Object>>() { });
    }

    protected void verifyFields(String patternJson, String expectedJson) throws IOException {

        Map<String, Object> actualResult = parseJson(process(patternJson));
        Map<String, Object> expectedResult = parseJson(expectedJson);

        assertThat(actualResult).isEqualTo(expectedResult);
    }

    private String process(final String patternJson) throws IOException {
        NodeWriter<Event> root = parser.parse(patternJson);
        assertThat(root).isNotNull();
        
        jsonGenerator.writeStartObject();
        root.write(jsonGenerator, event);
        jsonGenerator.writeEndObject();
        jsonGenerator.flush();

        return buffer.toString();
    }

    @Test
    public void shouldKeepPrimitiveConstantValues() throws IOException {

        String pattern = ""
                + "{\n"
                + "    \"const\": null,\n"
                + "    \"string\": \"value\",\n"
                + "    \"integer\": 1024,\n"
                + "    \"double\": 0.1,\n"
                + "    \"bool\": false\n"
                + "}";

        String expected = ""
                + "{\n"
                + "    \"const\": null,\n"
                + "    \"string\": \"value\",\n"
                + "    \"integer\": 1024,\n"
                + "    \"double\": 0.1,\n"
                + "    \"bool\": false\n"
                + "}";

        verifyFields(pattern, expected);
    }

    @Test
    public void shouldAllowUsingArraysAsValues() throws IOException {

        String pattern = ""
                + "{\n"
                + "    \"list\": [\n"
                + "        \"value\",\n"
                + "        100,\n"
                + "        0.33,\n"
                + "        true\n"
                + "    ]\n"
                + "}";

        String expected = ""
                + "{\n"
                + "    \"list\": [\n"
                + "        \"value\",\n"
                + "        100,\n"
                + "        0.33,\n"
                + "        true\n"
                + "    ]\n"
                + "}";

        verifyFields(pattern, expected);
    }

    @Test
    public void shouldAllowUsingObjectsAsValues() throws IOException {

        String pattern = ""
                + "{\n"
                + "    \"map\": {\n"
                + "        \"string\": \"value\",\n"
                + "        \"int\": 100,\n"
                + "        \"double\": 0.33,\n"
                + "        \"bool\": true\n"
                + "    }\n"
                + "}";

        String expected = ""
                + "{\n"
                + "    \"map\": {\n"
                + "        \"string\": \"value\",\n"
                + "        \"int\": 100,\n"
                + "        \"double\": 0.33,\n"
                + "        \"bool\": true\n"
                + "    }\n"
                + "}";

        verifyFields(pattern, expected);
    }

    @Test
    public void asLongShouldTransformTextValueToLong() throws IOException {

        String pattern = ""
                + "{\n"
                + "    \"key\": \"#asLong{555}\"\n"
                + "}";

        String expected = ""
                + "{\n"
                + "    \"key\": 555\n"
                + "}";

        verifyFields(pattern, expected);
    }

    @Test
    public void asDoubleShouldTransformTextValueToDouble() throws IOException {

        String pattern = ""
                + "{\n"
                + "    \"key\": \"#asDouble{0.5}\"\n"
                + "}";

        String expected = ""
                + "{\n"
                + "    \"key\": 0.5\n"
                + "}";

        verifyFields(pattern, expected);
    }

    @Test
    public void asJsonShouldTransformTextValueToJson() throws IOException {

        String pattern = ""
                + "{\n"
                + "    \"key1\": \"#asJson{true}\",\n"
                + "    \"key2\": \"#asJson{123}\",\n"
                + "    \"key3\": \"#asJson{123.4}\",\n"
                + "    \"key4\": \"#asJson{\\\"123\\\"}\",\n"
                + "    \"key5\": \"#asJson{[1, 2]}\",\n"
                + "    \"key6\": \"#asJson{[1, \\\"2\\\"]}\",\n"
                + "    \"key7\": \"#asJson{{\\\"field\\\":\\\"value\\\"}}\",\n"
                + "    \"key8\": \"#asJson{{\\\"field\\\":\\\"value\\\",\\\"num\\\":123}}\",\n"
                + "    \"key9\": \"#asJson{one two three}\"\n"
                + "}";

        String expected = ""
                + "{\n"
                + "    \"key1\": true,\n"
                + "    \"key2\": 123,\n"
                + "    \"key3\": 123.4,\n"
                + "    \"key4\": \"123\",\n"
                + "    \"key5\": [1, 2],\n"
                + "    \"key6\": [1, \"2\"],\n"
                + "    \"key7\": {\"field\":\"value\"},\n"
                + "    \"key8\": {\"field\":\"value\", \"num\":123},\n"
                + "    \"key9\": null\n"
                + "}";

        verifyFields(pattern, expected);
    }

    @Test
    public void tryJsonShouldTransformTextValueToJson() throws IOException {

        String pattern = ""
                + "{\n"
                + "    \"key1\": \"#tryJson{true}\",\n"
                + "    \"key2\": \"#tryJson{123}\",\n"
                + "    \"key3\": \"#tryJson{123.4}\",\n"
                + "    \"key4\": \"#tryJson{\\\"123\\\"}\",\n"
                + "    \"key5\": \"#tryJson{[1, 2]}\",\n"
                + "    \"key6\": \"#tryJson{[1, \\\"2\\\"]}\",\n"
                + "    \"key7\": \"#tryJson{{\\\"field\\\":\\\"value\\\"}}\",\n"
                + "    \"key8\": \"#tryJson{{\\\"field\\\":\\\"value\\\",\\\"num\\\":123}}\",\n"
                + "    \"key9\": \"#tryJson{{\\\"field\\\":\\\"value\\\"} extra}\",\n"
                + "    \"key10\": \"#tryJson{one two three}\",\n"
                + "    \"key11\": \"#tryJson{ false }\",\n"
                + "    \"key12\": \"#tryJson{ false true}\",\n"
                + "    \"key13\": \"#tryJson{123 foo}\",\n"
                + "    \"key14\": \"#tryJson{ 123 }\"\n"
                + "}";

        String expected = ""
                + "{\n"
                + "    \"key1\": true,\n"
                + "    \"key2\": 123,\n"
                + "    \"key3\": 123.4,\n"
                + "    \"key4\": \"123\",\n"
                + "    \"key5\": [1, 2],\n"
                + "    \"key6\": [1, \"2\"],\n"
                + "    \"key7\": {\"field\":\"value\"},\n"
                + "    \"key8\": {\"field\":\"value\", \"num\":123},\n"
                + "    \"key9\": \"{\\\"field\\\":\\\"value\\\"} extra\",\n"
                + "    \"key10\": \"one two three\",\n"
                + "    \"key11\": false,\n"
                + "    \"key12\": \" false true\",\n"
                + "    \"key13\": \"123 foo\",\n"
                + "    \"key14\": 123\n"
                + "}";

        verifyFields(pattern, expected);
    }

    @Test
    public void shouldSendNonTransformableValuesAsNulls() throws IOException {

        String pattern = ""
                + "{\n"
                + "    \"key1\": \"#asLong{abc}\",\n"
                + "    \"key2\": \"test\",\n"
                + "    \"key3\": \"#asDouble{abc}\",\n"
                + "    \"key4\": \"#asJson{[1, 2}\"\n"
                + "}";

        String expected = ""
                + "{\n"
                + "    \"key1\": null,\n"
                + "    \"key2\": \"test\",\n"
                + "    \"key3\": null,\n"
                + "    \"key4\": null\n"
                + "}";

        verifyFields(pattern, expected);
    }

    @Test
    public void shouldKeepUnrecognisedOrInvalidOperationsAsStringLiterals() throws IOException {

        String pattern = ""
                + "{\n"
                + "    \"key1\": \"#asDouble{0\",\n"
                + "    \"key2\": \"#asDouble\",\n"
                + "    \"key3\": \"#something\",\n"
                + "    \"key4\": \"#asJson{[1, 2]\"\n"
                + "}";

        String expected = ""
                + "{\n"
                + "    \"key1\": \"#asDouble{0\",\n"
                + "    \"key2\": \"#asDouble\",\n"
                + "    \"key3\": \"#something\",\n"
                + "    \"key4\": \"#asJson{[1, 2]\"\n"
                + "}";

        verifyFields(pattern, expected);
    }

    @Test
    public void shouldOmitNullConstants() throws IOException {
        parser.setOmitEmptyFields(true);

        String pattern = ""
                + "{\n"
                + "    \"const\": null\n"
                + "}";

        String expected = ""
                + "{\n"
                + "}";

        verifyFields(pattern, expected);
    }

    @Test
    public void shouldOmitEmptyConstants() throws IOException {
        parser.setOmitEmptyFields(true);

        String pattern = ""
                + "{\n"
                + "    \"string\": \"\",\n"
                + "    \"list\": [\n"
                + "    ],\n"
                + "    \"map\": {\n"
                + "    }\n"
                + "}";

        String expected = ""
                + "{\n"
                + "}";

        verifyFields(pattern, expected);
    }

    @Test
    public void shouldOmitEmptyJsonValues() throws IOException {
        parser.setOmitEmptyFields(true);
        String pattern = ""
                + "{\n"
                + "    \"null\": \"#asJson{null}\",\n"
                + "    \"string\": \"#asJson{}\",\n"
                + "    \"list\": \"#asJson{[]}\",\n"
                + "    \"object\": \"#asJson{{}}\"\n"
                + "}";

        String expected = ""
                + "{\n"
                + "}";

        verifyFields(pattern, expected);
    }

    @Test
    public void shouldOmitEmptyConstantsRecursively() throws IOException {
        parser.setOmitEmptyFields(true);

        String pattern = ""
                + "{\n"
                + "    \"object\": {\n"
                + "        \"string\": \"\",\n"
                + "        \"list\": [\n"
                + "        ],\n"
                + "        \"map\": {\n"
                + "        }\n"
                + "    },\n"
                + "    \"list\": [\n"
                + "        {\n"
                + "            \"string\": \"\",\n"
                + "            \"list\": [\n"
                + "            ],\n"
                + "            \"map\": {\n"
                + "            }\n"
                + "        }\n"
                + "    ]\n"
                + "}";

        String expected = ""
                + "{\n"
                + "}";

        verifyFields(pattern, expected);
    }

}
