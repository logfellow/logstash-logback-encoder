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
package net.logstash.logback.layout.parser;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAware;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;

/**
 * @author <a href="mailto:dimas@dataart.com">Dmitry Andrianov</a>
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractJsonPatternParserTest<E> {

    @Mock
    protected ContextAware contextAware;

    private JsonFactory jsonFactory;

    private JsonGenerator jsonGenerator;

    private StringWriter buffer = new StringWriter();

    private E event;

    private AbstractJsonPatternParser<E> parser;

    @Mock
    private Context context;

    @Before
    public void setUp() throws Exception {

        event = createEvent();

        given(contextAware.getContext()).willReturn(context);

        jsonFactory = new MappingJsonFactory();
        jsonGenerator = jsonFactory.createGenerator(buffer);

        parser = createParser();
    }

    abstract protected E createEvent();

    abstract protected AbstractJsonPatternParser<E> createParser();

    private Map<String, Object> parseJson(final String text) throws IOException {
        return jsonFactory.createParser(text).readValueAs(new TypeReference<Map<String, Object>>() { });
    }

    protected void verifyWithoutDefaultFields(String patternJson, String expectedJson) throws IOException {

        Map<String, Object> actualResult = parseJson(process(patternJson));
        Map<String, Object> expectedResult = parseJson(expectedJson);

        actualResult.remove("@timestamp");
        actualResult.remove("@version");

        assertThat(actualResult, equalTo(expectedResult));
    }

    protected void verifyAllFields(String patternJson, String expectedJson) throws IOException {

        Map<String, Object> actualResult = parseJson(process(patternJson));
        Map<String, Object> expectedResult = parseJson(expectedJson);

        assertThat(actualResult, equalTo(expectedResult));
    }

    private String process(final String patternJson) throws IOException {NodeWriter<E> root = parser.parse(jsonFactory , patternJson);
        assertThat(root, notNullValue());

        root.write(jsonGenerator, event);
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

        verifyWithoutDefaultFields(pattern, expected);
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

        verifyWithoutDefaultFields(pattern, expected);
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

        verifyWithoutDefaultFields(pattern, expected);
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

        verifyWithoutDefaultFields(pattern, expected);
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

        verifyWithoutDefaultFields(pattern, expected);
    }

    @Test
    public void shouldOmitNonTransformableValuesFromOutput() throws IOException {

        String pattern = ""
                + "{\n"
                + "    \"key1\": \"#asLong{abc}\",\n"
                + "    \"key2\": \"test\",\n"
                + "    \"key3\": \"#asDouble{abc}\"\n"
                + "}";

        String expected = ""
                + "{\n"
                + "    \"key2\": \"test\"\n"
                + "}";

        verifyWithoutDefaultFields(pattern, expected);
    }

    @Test
    public void shouldKeepUnrecognisedOrInvalidOperationsAsStringLiterals() throws IOException {

        String pattern = ""
                + "{\n"
                + "    \"key1\": \"#asDouble{0\",\n"
                + "    \"key2\": \"#asDouble\",\n"
                + "    \"key3\": \"#something\"\n"
                + "}";

        String expected = ""
                + "{\n"
                + "    \"key1\": \"#asDouble{0\",\n"
                + "    \"key2\": \"#asDouble\",\n"
                + "    \"key3\": \"#something\"\n"
                + "}";

        verifyWithoutDefaultFields(pattern, expected);
    }

    @Test
    public void shouldGenerateDefaultFields() throws IOException {

        String patternJson = "{}";

        Map<String, Object> actualResult = parseJson(process(patternJson));

        assertThat(actualResult, allOf(
                hasKey("@timestamp"),
                hasEntry("@version", (Object) 1)
        ));
    }

    @Test
    public void shouldAllowOverridingDefaultFields() throws IOException {

        String pattern = ""
                + "{\n"
                + "    \"@timestamp\": \"end of times\",\n"
                + "    \"@version\": 0\n"
                + "}";

        String expected = ""
                + "{\n"
                + "    \"@timestamp\": \"end of times\",\n"
                + "    \"@version\": 0\n"
                + "}";

        verifyAllFields(pattern, expected);
    }
}
