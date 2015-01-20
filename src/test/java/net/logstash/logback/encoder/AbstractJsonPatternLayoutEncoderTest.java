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
package net.logstash.logback.encoder;

import ch.qos.logback.core.Context;
import net.logstash.logback.layout.AbstractJsonPatternLayout;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

/**
 * This test just verifies that encoder delegates all the work to Layout
 *
 * @param <E> - type of the event (ILoggingEvent, IAccessEvent)
 *
 * @author <a href="mailto:dimas@dataart.com">Dmitry Andrianov</a>
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractJsonPatternLayoutEncoderTest<E> {

    // What our Layout generates when invoked
    public static final String TEST_LAYOUT_RESULT = "[\"generated string\"]";

    @Mock
    private E event;

    @Mock
    private Context context;

    private AbstractJsonPatternLayout<E> layout;

    private AbstractJsonPatternLayoutEncoder<E> encoder;

    @Before
    public void setUp() throws Exception {
        encoder = createEncoder();
        encoder.setPattern("{}");
    }

    protected abstract AbstractJsonPatternLayoutEncoder<E> createEncoder();

    protected AbstractJsonPatternLayout<E> decorateLayout(AbstractJsonPatternLayout<E> layout) {
        this.layout = spy(layout);
        doReturn(TEST_LAYOUT_RESULT).when(this.layout).doLayout(event);
        return this.layout;
    }

    @Test
    public void shouldPassFormatToLayout() {
        encoder.start();
        encoder.setPattern("xyz");
        verify(layout).setPattern("xyz");
    }

    @Test
    public void shouldStartLayoutWhenStarted() {
        encoder.setContext(context);
        encoder.start();

        verify(layout).setContext(context);
        verify(layout).start();
    }

    @Test
    public void doEncodeShouldCallLayout() throws IOException {
        // pattern used does not matter because decorated "parser" will always generate TEST_NODEWRITER_RESULT
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        encoder.init(os);
        encoder.start();

        encoder.doEncode(event);

        // should actually invoke layout with the event
        verify(layout).doLayout(event);
        // and the end result should be what layout returns. But only match prefix as newline will be added
        String result = new String(os.toByteArray());
        assertThat(result, Matchers.startsWith(TEST_LAYOUT_RESULT));
    }
}