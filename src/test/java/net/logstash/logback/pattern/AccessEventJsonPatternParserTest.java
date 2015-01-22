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

import ch.qos.logback.access.spi.IAccessEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.mockito.BDDMockito.given;

/**
 * @author <a href="mailto:dimas@dataart.com">Dmitry Andrianov</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class AccessEventJsonPatternParserTest extends AbstractJsonPatternParserTest<IAccessEvent>{

    @Mock
    private IAccessEvent event;

    @Override
    protected IAccessEvent createEvent() {
        given(event.getMethod()).willReturn("PUT");
        given(event.getAttribute("MISSING")).willReturn("-"); // Just like logback itself does for non-existent attrs
        return event;
    }

    @Override
    protected AbstractJsonPatternParser<IAccessEvent> createParser() {
        return new AccessEventJsonPatternParser(contextAware, jsonFactory);
    }

    @Test
    public void shouldRunPatternLayoutConversions() throws IOException {

        String pattern = ""
                + "{\n"
                + "    \"level\": \"%requestMethod\"\n"
                + "}";

        String expected = ""
                + "{\n"
                + "    \"level\": \"PUT\"\n"
                + "}";

        verifyWithoutDefaultFields(pattern, expected);
    }

    @Test
    public void noNaOperationShouldNullifySingleDash() throws IOException {

        String pattern = ""
                + "{\n"
                + "    \"cookie1\": \"%requestAttribute{MISSING}\",\n"
                + "    \"cookie2\": \"#nullNA{%requestAttribute{MISSING}}\"\n"
                + "}";

        String expected = ""
                + "{\n"
                + "    \"cookie1\": \"-\",\n"
                + "    \"cookie2\": null\n"
                + "}";

        verifyWithoutDefaultFields(pattern, expected);
    }
}
