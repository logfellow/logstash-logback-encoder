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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.given;

/**
 * @author <a href="mailto:dimas@dataart.com">Dmitry Andrianov</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class LoggingEventJsonPatternParserTest extends AbstractJsonPatternParserTest<ILoggingEvent>{

    @Mock
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
    protected AbstractJsonPatternParser<ILoggingEvent> createParser() {
        return new LoggingEventJsonPatternParser(contextAware, jsonFactory);
    }

    @Test
    public void shouldRunPatternLayoutConversions() throws IOException {

        String pattern = ""
                + "{\n"
                + "    \"level\": \"%level\"\n"
                + "}";

        String expected = ""
                + "{\n"
                + "    \"level\": \"DEBUG\"\n"
                + "}";

        verifyFields(pattern, expected);
    }

    @Test
    public void shouldAllowIndividualMdcItemsToBeIncludedUsingConverter() throws IOException {

        String pattern = ""
                + "{\n"
                + "    \"mdc.key1\": \"%mdc{key1}\"\n"
                + "}";

        String expected = ""
                + "{\n"
                + "    \"mdc.key1\": \"value1\"\n"
                + "}";

        verifyFields(pattern, expected);
    }
}
