/*
 * Copyright 2013-2021 the original author or authors.
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
package net.logstash.logback.pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import net.logstash.logback.pattern.AbstractJsonPatternParser.JsonPatternException;
import net.logstash.logback.test.AbstractLogbackTest;

import ch.qos.logback.core.Context;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author <a href="mailto:dimas@dataart.com">Dmitry Andrianov</a>
 */
@ExtendWith(MockitoExtension.class)
public abstract class AbstractJsonPatternParserTest<Event> extends AbstractLogbackTest {

    private JsonFactory jsonFactory = new MappingJsonFactory();

    private Event event;

    protected AbstractJsonPatternParser<Event> parser;


    @BeforeEach
    public void setup() throws Exception {
        super.setup();
        
        this.event = createEvent();
        this.parser = createParser(context, jsonFactory);
    }

    protected abstract Event createEvent();

    protected abstract AbstractJsonPatternParser<Event> createParser(Context context, JsonFactory jsonFactory);

    private Map<String, Object> parseJson(final String text) throws IOException {
        try (JsonParser jsonParser = jsonFactory.createParser(text)) {
            return jsonParser.readValueAs(new TypeReference<Map<String, Object>>() { });
        }
    }

    protected void verifyFields(String patternJson, String expectedJson) throws Exception {

        Map<String, Object> actualResult = parseJson(process(patternJson));
        Map<String, Object> expectedResult = parseJson(expectedJson);

        assertThat(actualResult).isEqualTo(expectedResult);
    }

    private String process(final String patternJson) throws JsonPatternException, IOException {
        NodeWriter<Event> root = parser.parse(patternJson);
        assertThat(root).isNotNull();
        
        StringWriter buffer = new StringWriter();
        
        try (JsonGenerator jsonGenerator = jsonFactory.createGenerator(buffer)) {
            jsonGenerator.writeStartObject();
            root.write(jsonGenerator, event);
            jsonGenerator.writeEndObject();
            jsonGenerator.flush();
        }
        
        return buffer.toString();
    }

    @Test
    public void shouldKeepPrimitiveConstantValues() throws Exception {

        String pattern = toJson(
                  "{                        "
                + "    'const'  : null,     "
                + "    'string' : 'value',  "
                + "    'integer': 1024,     "
                + "    'double' : 0.1,      "
                + "    'bool'   : false     "
                + "}                        ");

        String expected = toJson(
                  "{                        "
                + "    'const'  : null,     "
                + "    'string' : 'value',  "
                + "    'integer': 1024,     "
                + "    'double' : 0.1,      "
                + "    'bool'   : false     "
                + "}                        ");

        verifyFields(pattern, expected);
    }

    @Test
    public void shouldAllowUsingArraysAsValues() throws Exception {

        String pattern = toJson(
                  "{                    "
                + "    'list': [        "
                + "        'value',     "
                + "        100,         "
                + "        0.33,        "
                + "        true         "
                + "    ]                "
                + "}                    ");

        String expected = toJson(
                  "{                    "
                + "    'list': [        "
                + "        'value',     "
                + "        100,         "
                + "        0.33,        "
                + "        true         "
                + "    ]                "
                + "}                    ");

        verifyFields(pattern, expected);
    }

    @Test
    public void shouldAllowUsingObjectsAsValues() throws Exception {

        String pattern = toJson(
                  "{                              "
                + "    'map': {                   "
                + "        'string': 'value',     "
                + "        'int'   : 100,         "
                + "        'double': 0.33,        "
                + "        'bool'  : true         "
                + "    }                          "
                + "}                              ");

        String expected = toJson(
                  "{                              "
                + "    'map': {                   "
                + "        'string': 'value',     "
                + "        'int'   : 100,         "
                + "        'double': 0.33,        "
                + "        'bool'  : true         "
                + "    }                          "
                + "}                              ");

        verifyFields(pattern, expected);
    }

    @Test
    public void asLongShouldTransformTextValueToLong() throws Exception {

        String pattern = toJson(
                "{ 'key': '#asLong{555}' }");

        String expected = toJson(
                "{ 'key': 555 }");

        verifyFields(pattern, expected);
    }

    @Test
    public void asDoubleShouldTransformTextValueToDouble() throws Exception {

        String pattern = toJson(
                "{ 'key': '#asDouble{0.5}' }");

        String expected = toJson(
                "{ 'key': 0.5 }");

        verifyFields(pattern, expected);
    }

    @Test
    public void asJsonShouldTransformTextValueToJson() throws Exception {

        String pattern = toJson(
                  "{                                                                 "
                + "    'key1' : '#asJson{true}',                                     "
                + "    'key2' : '#asJson{123}',                                      "
                + "    'key3' : '#asJson{123.4}',                                    "
                + "    'key4' : '#asJson{\\'123\\'}',                                "
                + "    'key5' : '#asJson{[1, 2]}',                                   "
                + "    'key6' : '#asJson{[1, \\'2\\']}',                             "
                + "    'key7' : '#asJson{{\\'field\\':\\'value\\'}}',                "
                + "    'key8' : '#asJson{{\\'field\\':\\'value\\',\\'num\\':123}}',  "
                + "    'key9' : '#asJson{one two three}',                            "
                + "    'key10': '#asJson{1 suffix}'                                  "
                + "}                                                                 ");

        String expected = toJson(
                  "{                                                                 "
                + "    'key1' : true,                                                "
                + "    'key2' : 123,                                                 "
                + "    'key3' : 123.4,                                               "
                + "    'key4' : '123',                                               "
                + "    'key5' : [1, 2],                                              "
                + "    'key6' : [1, '2'],                                            "
                + "    'key7' : {'field':'value'},                                   "
                + "    'key8' : {'field':'value', 'num':123},                        "
                + "    'key9' : null,                                                "
                + "    'key10': null                                                 "
                + "}                                                                 ");

        verifyFields(pattern, expected);
    }

    @Test
    public void tryJsonShouldTransformTextValueToJson() throws Exception {

        String pattern = toJson(
                  "{                                                                  "
                + "    'key1' : '#tryJson{true}',                                     "
                + "    'key2' : '#tryJson{123}',                                      "
                + "    'key3' : '#tryJson{123.4}',                                    "
                + "    'key4' : '#tryJson{\\'123\\'}',                                "
                + "    'key5' : '#tryJson{[1, 2]}',                                   "
                + "    'key6' : '#tryJson{[1, \\'2\\']}',                             "
                + "    'key7' : '#tryJson{{\\'field\\':\\'value\\'}}',                "
                + "    'key8' : '#tryJson{{\\'field\\':\\'value\\',\\'num\\':123}}',  "
                + "    'key9' : '#tryJson{{\\'field\\':\\'value\\'} extra}',          "
                + "    'key10': '#tryJson{one two three}',                            "
                + "    'key11': '#tryJson{ false }',                                  "
                + "    'key12': '#tryJson{ false true}',                              "
                + "    'key13': '#tryJson{123 foo}',                                  "
                + "    'key14': '#tryJson{ 123 }'                                     "
                + "}                                                                  ");

        String expected = toJson(
                  "{                                                                  "
                + "    'key1' : true,                                                 "
                + "    'key2' : 123,                                                  "
                + "    'key3' : 123.4,                                                "
                + "    'key4' : '123',                                                "
                + "    'key5' : [1, 2],                                               "
                + "    'key6' : [1, '2'],                                             "
                + "    'key7' : {'field':'value'},                                    "
                + "    'key8' : {'field':'value', 'num':123},                         "
                + "    'key9' : '{\\'field\\':\\'value\\'} extra',                    "
                + "    'key10': 'one two three',                                      "
                + "    'key11': false,                                                "
                + "    'key12': ' false true',                                        "
                + "    'key13': '123 foo',                                            "
                + "    'key14': 123                                                   "
                + "}                                                                  ");

        verifyFields(pattern, expected);
    }

    @Test
    public void shouldSendNonTransformableValuesAsNulls() throws Exception {

        String pattern = toJson(
                  "{                               "
                + "    'key1': '#asLong{abc}',     "
                + "    'key2': 'test',             "
                + "    'key3': '#asDouble{abc}',   "
                + "    'key4': '#asJson{[1, 2}'    "
                + "}                               ");

        String expected = toJson(
                  "{                               "
                + "    'key1': null,               "
                + "    'key2': 'test',             "
                + "    'key3': null,               "
                + "    'key4': null                "
                + "}                               ");

        verifyFields(pattern, expected);
    }

    @Test
    public void shouldKeepUnrecognisedOrInvalidOperationsAsStringLiterals() throws Exception {

        String pattern = toJson(
                  "{                               "
                + "    'key1': '#asDouble{0',      "
                + "    'key2': '#asDouble',        "
                + "    'key3': '#something',       "
                + "    'key4': '#asJson{[1, 2]'    "
                + "}                               ");

        String expected = toJson(
                  "{                               "
                + "    'key1': '#asDouble{0',      "
                + "    'key2': '#asDouble',        "
                + "    'key3': '#something',       "
                + "    'key4': '#asJson{[1, 2]'    "
                + "}                               ");

        verifyFields(pattern, expected);
    }

    @Test
    public void shouldOmitNullConstants() throws Exception {
        parser.setOmitEmptyFields(true);

        String pattern = toJson(
                "{ 'const': null }");

        String expected = toJson(
                "{}");

        verifyFields(pattern, expected);
    }

    @Test
    public void shouldOmitEmptyConstants() throws Exception {
        parser.setOmitEmptyFields(true);

        String pattern = toJson(
                  "{                  "
                + "    'string': '',  "
                + "    'list'  : [],  "
                + "    'map'   : {}   "
                + "}                  ");

        String expected = toJson("{}");

        verifyFields(pattern, expected);
    }

    @Test
    public void shouldOmitEmptyJsonValues() throws Exception {
        parser.setOmitEmptyFields(true);
        String pattern = toJson(
                  "{                             "
                + "  'null'   : '#asJson{null}', "
                + "  'string:': '#asJson{}',     "
                + "  'list'   : '#asJson{[]}',   "
                + "  'object' : '#asJson{{}}'    "
                + "}                             ");
        
        String expected = toJson("{}");

        verifyFields(pattern, expected);
    }

    @Test
    public void shouldOmitEmptyConstantsRecursively() throws Exception {
        parser.setOmitEmptyFields(true);

        String pattern = toJson(
                  "{                          "
                + "    'object': {            "
                + "        'string': '',      "
                + "        'list'  : [],      "
                + "        'map'   : {}       "
                + "    },                     "
                + "    'list': [              "
                + "        {                  "
                + "            'string': '',  "
                + "            'list'  : [],  "
                + "            'map'   : {}   "
                + "        }                  "
                + "    ]                      "
                + "}                          ");
        
        String expected = toJson("{}");

        verifyFields(pattern, expected);
    }

    
    @Test
    public void invalidPatternLayout() throws Exception {
        assertThatExceptionOfType(JsonPatternException.class).isThrownBy(() -> parser.parse(toJson("{'msg':'%foo'}")));
    }
    
    
    @Test
    public void invalidJSON() throws Exception {
        parser.parse(toJson("{'msg' : '#asJson{foo}' }"));
        assertThatExceptionOfType(JsonPatternException.class).isThrownBy(() -> parser.parse(toJson("{'msg' = #asJson{foo} }")));
    }
    
    
    protected static String toJson(String str) {
        return str.replace("'", "\"");
    }
}
