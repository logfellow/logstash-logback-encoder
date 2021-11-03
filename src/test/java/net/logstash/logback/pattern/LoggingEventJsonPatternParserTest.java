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

import static org.mockito.BDDMockito.given;

import java.util.HashMap;
import java.util.Map;

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
        return event;
    }

    @Override
    protected AbstractJsonPatternParser<ILoggingEvent> createParser(Context context, JsonFactory jsonFactory) {
        return new LoggingEventJsonPatternParser(context, jsonFactory);
    }

    @Test
    public void shouldRunPatternLayoutConversions() throws Exception {

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
    public void shouldAllowIndividualMdcItemsToBeIncludedUsingConverter() throws Exception {

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
    public void shouldOmitNullMdcValue() throws Exception {
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
}
