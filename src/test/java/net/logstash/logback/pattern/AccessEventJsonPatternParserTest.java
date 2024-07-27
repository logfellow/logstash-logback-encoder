/*
 * Copyright 2013-2023 the original author or authors.
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

import ch.qos.logback.access.common.spi.IAccessEvent;
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
public class AccessEventJsonPatternParserTest extends AbstractJsonPatternParserTest<IAccessEvent> {

    @Mock(lenient = true)
    private IAccessEvent event;

    @Override
    protected IAccessEvent createEvent() {
        given(event.getMethod()).willReturn("PUT");
        given(event.getAttribute("MISSING")).willReturn("-"); // Just like logback itself does for non-existent attrs
        return event;
    }

    @Override
    protected AbstractJsonPatternParser<IAccessEvent> createParser(Context context, JsonFactory jsonFactory) {
        return new AccessEventJsonPatternParser(context, jsonFactory);
    }

    @Test
    public void shouldRunPatternLayoutConversions() throws Exception {

        String pattern = toJson(
                  "{                              "
                + "    'level': '%requestMethod'  "
                + "}                              ");

        String expected = toJson(
                  "{                              "
                + "    'level': 'PUT'             "
                + "}                              ");

        verifyFields(pattern, expected);
    }

    @Test
    public void noNaOperationShouldNullifySingleDash() throws Exception {

        String pattern = toJson(
                  "{                                                     "
                + "    'cookie1': '%requestAttribute{MISSING}',          "
                + "    'cookie2': '#nullNA{%requestAttribute{MISSING}}'  "
                + "}                                                     ");

        String expected = toJson(
                  "{                                                     "
                + "    'cookie1': '-',                                   "
                + "    'cookie2': null                                   "
                + "}                                                     ");

        verifyFields(pattern, expected);
    }
}
