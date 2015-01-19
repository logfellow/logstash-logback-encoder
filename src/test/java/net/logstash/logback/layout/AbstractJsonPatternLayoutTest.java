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
package net.logstash.logback.layout;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.layout.parser.AbstractJsonPatternParser;
import net.logstash.logback.layout.parser.NodeWriter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * This test just verifies that Layout delegates all the work to Parser
 *
 * @param <E> - type of the event (ILoggingEvent, IAccessEvent)
 *
 * @author <a href="mailto:dimas@dataart.com">Dmitry Andrianov</a>
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractJsonPatternLayoutTest<E> {

    // What our TestNodeWriter generates when invoked
    public static final String TEST_NODEWRITER_RESULT = "generated string";

    @Mock
    private E event;

    private AbstractJsonPatternParser<E> parser;

    private NodeWriter<E> nodeWriter = new TestNodeWriter<E>();

    private AbstractJsonPatternLayout<E> layout;

    @Before
    public void setUp() throws Exception {
        layout = createLayout();
    }

    protected abstract AbstractJsonPatternLayout<E> createLayout();

    static class TestNodeWriter<E> implements NodeWriter<E> {
        @Override
        public void write(final JsonGenerator generator, final E event) throws IOException {
            generator.writeString(TEST_NODEWRITER_RESULT);
        }
    }

    protected AbstractJsonPatternParser<E> decorateParser(AbstractJsonPatternParser<E> parser) {
        this.parser = spy(parser);
        doReturn(nodeWriter).when(this.parser).parse(any(JsonFactory.class), anyString());
        return this.parser;
    }

    @Test
    public void shouldDelegateToParser() {
        // pattern used does not matter because decorated "parser" will always generate TEST_NODEWRITER_RESULT
        final String pattern = "{\"key\":\"value\"}";
        layout.setPattern(pattern);
        layout.start();

        String result = layout.doLayout(event);

        // should actually invoke parser with the pattern requested
        verify(parser).parse(any(JsonFactory.class), eq(pattern));
        // and the end result should be what NodeWriter returned by the parser produces
        assertThat(result, is('"' + TEST_NODEWRITER_RESULT + '"'));

    }
}