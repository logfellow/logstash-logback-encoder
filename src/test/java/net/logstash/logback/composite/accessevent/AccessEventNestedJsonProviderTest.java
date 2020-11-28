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
package net.logstash.logback.composite.accessevent;

import static org.mockito.Mockito.inOrder;

import java.io.IOException;

import ch.qos.logback.access.spi.IAccessEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonGenerator;

@ExtendWith(MockitoExtension.class)
public class AccessEventNestedJsonProviderTest {

    @InjectMocks
    private AccessEventNestedJsonProvider provider;
    
    @Mock
    private JsonGenerator generator;
    
    @Mock
    private IAccessEvent event;
    
    @Mock
    private AccessEventJsonProviders providers;
    
    @Test
    public void testWrite() throws IOException {
        
        provider.setFieldName("newFieldName");
        
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator, providers);
        
        inOrder.verify(generator).writeFieldName("newFieldName");
        inOrder.verify(generator).writeStartObject();
        inOrder.verify(providers).writeTo(generator, event);
        inOrder.verify(generator).writeEndObject();
    }

}
