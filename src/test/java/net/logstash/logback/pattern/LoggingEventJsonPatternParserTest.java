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
package net.logstash.logback.pattern;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.HashMap;
import java.util.Map;

import net.logstash.logback.pattern.AbstractJsonPatternParser.JsonPatternException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import com.fasterxml.jackson.core.JsonFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author <a href="mailto:dimas@dataart.com">Dmitry Andrianov</a>
 */
@ExtendWith(MockitoExtension.class)
public class LoggingEventJsonPatternParserTest extends AbstractJsonPatternParserTest<ILoggingEvent> {

    @Mock(lenient = true)
    private ILoggingEvent event;

    private Map<String, String> mdc = new HashMap<String, String>();

    @Override
    protected ILoggingEvent createEvent() {
        mdc.put("key1", "value1");
        mdc.put("key2", "value2");
        given(event.getMDCPropertyMap()).willReturn(mdc);
        given(event.getLevel()).willReturn(Level.DEBUG);
        given(event.getLoggerContextVO()).willAnswer(invocation -> context.getLoggerContextRemoteView());
        return event;
    }

    @Override
    protected AbstractJsonPatternParser<ILoggingEvent> createParser(Context context, JsonFactory jsonFactory) {
        return new LoggingEventJsonPatternParser(context, jsonFactory);
    }

    @Test
    public void shouldRunPatternLayoutConversions() throws JsonPatternException {

        String pattern = toJson(
                  "{                      "
                + "    'level': '%level'  "
                + "}                      ");

        String expected = toJson(
                  "{                      "
                + "    'level': 'DEBUG'   "
                + "}                      ");

        verifyFields(pattern, expected);
    }

    @Test
    public void shouldAllowIndividualMdcItemsToBeIncludedUsingConverter() throws JsonPatternException {

        String pattern = toJson(
                  "{                             "
                + "    'mdc.key1': '%mdc{key1}'  "
                + "}                             ");

        String expected = toJson(
                  "{                             "
                + "    'mdc.key1': 'value1'      "
                + "}                             ");

        verifyFields(pattern, expected);
    }

    @Test
    public void shouldOmitNullMdcValue() throws JsonPatternException {
        parser.setOmitEmptyFields(true);

        String pattern = toJson(
                  "{                              "
                + "    'mdc.key1': '%mdc{key1}',  "
                + "    'mdc.key3': '%mdc{key3}'   "
                + "}                              ");

        String expected = toJson(
                  "{                              "
                + "    'mdc.key1': 'value1'       "
                + "}                              ");

        verifyFields(pattern, expected);
    }
    
    
    @Test
    public void propertyDefined() throws JsonPatternException {
        context.putProperty("PROP", "value");

        String pattern = toJson(
                  "{                  "
                + "    'prop1': '%property{PROP}',      "
                + "    'prop2': '%property{PROP:-}',    "
                + "    'prop3': '%property{PROP:-foo}'  "
                + "}                  ");

        String expected = toJson(
                "{                           "
              + "    'prop1': 'value',       "
              + "    'prop2': 'value',       "
              + "    'prop3': 'value'        "
              + "}                           ");
              
        verifyFields(pattern, expected);
    }
    
    
    @Test
    public void propertyUndefined() throws JsonPatternException {

        String pattern = toJson(
                  "{                  "
                + "    'prop1': '%property{PROP}',      "
                + "    'prop2': '%property{PROP:-}',    "
                + "    'prop3': '%property{PROP:-foo}'  "
                + "}                  ");

        String expected = toJson(
                "{                      "
              + "    'prop1': '',       "
              + "    'prop2': '',       "
              + "    'prop3': 'foo'     "
              + "}                      ");
              
        verifyFields(pattern, expected);
    }
    
    
    @Test
    public void propertyInvalid() throws JsonPatternException {
        assertThatThrownBy(() -> parser.parse(toJson("{ 'prop': '%property{}' }"))).isInstanceOf(JsonPatternException.class);
    }
}
