package net.logstash.logback.composite.accessevent;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ch.qos.logback.access.spi.IAccessEvent;

import com.fasterxml.jackson.core.JsonGenerator;

@RunWith(MockitoJUnitRunner.class)
public class RequestHeadersJsonProviderTest {
    
    private RequestHeadersJsonProvider provider = new RequestHeadersJsonProvider();
    
    private Map<String, String> headers = new LinkedHashMap<String, String>(); 

    @Mock
    private JsonGenerator generator;

    @Mock
    private IAccessEvent event;
    
    @Before
    public void setup() {
        headers.put("headerA", "valueA");
        headers.put("headerB", "valueB");
        when(event.getRequestHeaderMap()).thenReturn(headers);
    }
    
    @Test
    public void testNoFieldName() throws IOException {
        provider.writeTo(generator, event);
        verifyNoMoreInteractions(generator);
    }

    @Test
    public void testFieldName() throws IOException {
        provider.setFieldName("fieldName");
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        inOrder.verify(generator).writeFieldName("fieldName");
        inOrder.verify(generator).writeStartObject();
        inOrder.verify(generator).writeStringField("headerA", "valueA");
        inOrder.verify(generator).writeStringField("headerB", "valueB");
        inOrder.verify(generator).writeEndObject();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFieldNameWithLowerCase() throws IOException {
        provider.setFieldName("fieldName");
        provider.setLowerCaseHeaderNames(true);
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        inOrder.verify(generator).writeFieldName("fieldName");
        inOrder.verify(generator).writeStartObject();
        inOrder.verify(generator).writeStringField("headera", "valueA");
        inOrder.verify(generator).writeStringField("headerb", "valueB");
        inOrder.verify(generator).writeEndObject();
        inOrder.verifyNoMoreInteractions();
    }

}
