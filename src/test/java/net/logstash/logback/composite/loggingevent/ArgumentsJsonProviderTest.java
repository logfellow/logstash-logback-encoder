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

import java.io.IOException;

import net.logstash.logback.argument.NamedArguments;
import net.logstash.logback.fieldnames.LogstashFieldNames;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;


public class ArgumentsJsonProviderTest {


    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private ArgumentsJsonProvider provider = new ArgumentsJsonProvider();

    @Mock
    private JsonGenerator generator;

    @Mock
    private ILoggingEvent event;

    private Object[] arguments;

    @Before
    public void setup() {
        arguments = new Object[]{
                NamedArguments.keyValue("k1", "v1"),
                NamedArguments.keyValue("k2", "v2", "{0}=[{1}]"),
                NamedArguments.value("k3", "v3"),
                "v4",
        };
        when(event.getArgumentArray()).thenReturn(arguments);
    }

    @Test
    public void testUnwrapped() throws IOException {

        provider.writeTo(generator, event);

        verify(generator).writeFieldName("k1");
        verify(generator).writeObject("v1");
        verify(generator).writeFieldName("k2");
        verify(generator).writeObject("v2");
        verify(generator).writeFieldName("k3");
        verify(generator).writeObject("v3");
    }

    @Test
    public void testWrapped() throws IOException {
        provider.setFieldName("args");

        provider.writeTo(generator, event);

        InOrder inOrder = inOrder(generator);
        inOrder.verify(generator).writeObjectFieldStart("args");
        verify(generator).writeFieldName("k1");
        verify(generator).writeObject("v1");
        verify(generator).writeFieldName("k2");
        verify(generator).writeObject("v2");
        verify(generator).writeFieldName("k3");
        verify(generator).writeObject("v3");
        inOrder.verify(generator).writeEndObject();
    }

    @Test
    public void testWrappedUsingFieldNames() throws IOException {
        LogstashFieldNames fieldNames = new LogstashFieldNames();
        fieldNames.setArguments("args");

        provider.setFieldNames(fieldNames);

        provider.writeTo(generator, event);

        InOrder inOrder = inOrder(generator);
        inOrder.verify(generator).writeObjectFieldStart("args");
        verify(generator).writeFieldName("k1");
        verify(generator).writeObject("v1");
        verify(generator).writeFieldName("k2");
        verify(generator).writeObject("v2");
        verify(generator).writeFieldName("k3");
        verify(generator).writeObject("v3");
        inOrder.verify(generator).writeEndObject();
    }


    @Test
    public void testIncludeArgumentWithNoKey() throws IOException {
        provider.setIncludeArgumentWithNoKey(true);

        provider.writeTo(generator, event);

        verify(generator).writeFieldName("k1");
        verify(generator).writeObject("v1");
        verify(generator).writeFieldName("k2");
        verify(generator).writeObject("v2");
        verify(generator).writeFieldName("k3");
        verify(generator).writeObject("v3");
        verify(generator).writeFieldName("arg4");
        verify(generator).writeObject("v4");
    }

    @Test
    public void testIncludeArgumentWithNoKeyAndCustomPrefix() throws IOException {
        provider.setIncludeArgumentWithNoKey(true);
        provider.setArgumentWithNoKeyPrefix("prefix");

        provider.writeTo(generator, event);

        verify(generator).writeFieldName("k1");
        verify(generator).writeObject("v1");
        verify(generator).writeFieldName("k2");
        verify(generator).writeObject("v2");
        verify(generator).writeFieldName("k3");
        verify(generator).writeObject("v3");
        verify(generator).writeFieldName("prefix4");
        verify(generator).writeObject("v4");
    }

}
