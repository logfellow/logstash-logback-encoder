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
package net.logstash.logback.composite.loggingevent;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;

public class SequenceJsonProviderTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private SequenceJsonProvider provider = new SequenceJsonProvider();

    @Mock
    private JsonGenerator generator;

    @Mock
    private ILoggingEvent event;

    @Test
    public void testDefaultName() throws IOException {
        provider.writeTo(generator, event);

        verify(generator).writeNumberField(eq(SequenceJsonProvider.FIELD_SEQUENCE),1);

    }

    @Test
    public void testFieldName() throws IOException {
        provider.setFieldName("newFieldName");

        provider.writeTo(generator, event);

        verify(generator).writeNumberField(eq("newFieldName"), 1);
    }
}
