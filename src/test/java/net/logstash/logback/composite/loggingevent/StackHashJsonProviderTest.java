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
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StackHashJsonProviderTest {
    
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    
    private StackHashJsonProvider provider = new StackHashJsonProvider();
    
    @Mock
    private JsonGenerator generator;
    
    @Mock
    private ILoggingEvent event;

    @Mock
    private ThrowableProxy throwableProxy;

    @Test
    public void testDefaultName() throws IOException {
        // GIVEN
        when(event.getThrowableProxy()).thenReturn(throwableProxy);
        when(throwableProxy.getThrowable()).thenReturn(new Exception("test error"));
        // WHEN
        provider.writeTo(generator, event);
        // THEN
        verify(generator).writeStringField(eq(StackHashJsonProvider.FIELD_NAME), anyString());
    }

    @Test
    public void testFieldName() throws IOException {
        // GIVEN
        when(event.getThrowableProxy()).thenReturn(throwableProxy);
        when(throwableProxy.getThrowable()).thenReturn(new Exception("test error"));
        provider.setFieldName("newFieldName");
        // WHEN
        provider.writeTo(generator, event);
        // THEN
        verify(generator).writeStringField(eq("newFieldName"), anyString());
    }
}
