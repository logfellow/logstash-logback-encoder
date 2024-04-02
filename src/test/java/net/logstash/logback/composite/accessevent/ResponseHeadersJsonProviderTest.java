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
package net.logstash.logback.composite.accessevent;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import ch.qos.logback.access.common.spi.IAccessEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResponseHeadersJsonProviderTest {
    
    private ResponseHeadersJsonProvider provider = new ResponseHeadersJsonProvider();
    
    private Map<String, String> headers = new LinkedHashMap<String, String>();

    @Mock
    private JsonGenerator generator;

    @Mock
    private IAccessEvent event;
    
    @BeforeEach
    public void setup() {
        headers.put("headerA", "valueA");
        headers.put("headerB", "valueB");
        when(event.getResponseHeaderMap()).thenReturn(headers);
    }
    
    @Test
    public void testNoFieldName() throws IOException {
        provider.setLowerCaseHeaderNames(false);
        provider.writeTo(generator, event);
        verifyNoMoreInteractions(generator);
    }

    @Test
    public void testFieldName() throws IOException {
        provider.setFieldName("fieldName");
        provider.setLowerCaseHeaderNames(false);
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        inOrder.verify(generator).writeObjectFieldStart("fieldName");
        inOrder.verify(generator).writeStringField("headerA", "valueA");
        inOrder.verify(generator).writeStringField("headerB", "valueB");
        inOrder.verify(generator).writeEndObject();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFieldNameWithLowerCase() throws IOException {
        provider.setFieldName("fieldName");
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        inOrder.verify(generator).writeObjectFieldStart("fieldName");
        inOrder.verify(generator).writeStringField("headera", "valueA");
        inOrder.verify(generator).writeStringField("headerb", "valueB");
        inOrder.verify(generator).writeEndObject();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFilter() throws IOException {
        
        IncludeExcludeHeaderFilter filter = new IncludeExcludeHeaderFilter();
        filter.addInclude("headerb");
        
        provider.setFieldName("fieldName");
        provider.setFilter(filter);
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        inOrder.verify(generator).writeObjectFieldStart("fieldName");
        inOrder.verify(generator).writeStringField("headerb", "valueB");
        inOrder.verify(generator).writeEndObject();
        inOrder.verifyNoMoreInteractions();
    }

}
