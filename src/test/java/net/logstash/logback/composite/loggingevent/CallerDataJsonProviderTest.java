/*
 * Copyright 2013-2021 the original author or authors.
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
package net.logstash.logback.composite.loggingevent;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import net.logstash.logback.fieldnames.LogstashFieldNames;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CallerDataJsonProviderTest {
    
    private static final StackTraceElement CALLER_DATA = new StackTraceElement("declaringClass", "methodName", "fileName", 100);

    private CallerDataJsonProvider provider = new CallerDataJsonProvider();
    
    @Mock
    private JsonGenerator generator;
    
    @Mock
    private ILoggingEvent event;
    
    private void setupCallerData() {
        when(event.getCallerData()).thenReturn(new StackTraceElement[] {CALLER_DATA});
    }
    
    @Test
    public void testUnwrapped() throws IOException {
        setupCallerData();
        
        provider.writeTo(generator, event);
        
        verify(generator, never()).writeStartObject();
        verify(generator, never()).writeEndObject();
        
        InOrder inOrder = inOrder(generator);
        
        inOrder.verify(generator).writeStringField(CallerDataJsonProvider.FIELD_CALLER_CLASS_NAME, CALLER_DATA.getClassName());
        inOrder.verify(generator).writeStringField(CallerDataJsonProvider.FIELD_CALLER_METHOD_NAME, CALLER_DATA.getMethodName());
        inOrder.verify(generator).writeStringField(CallerDataJsonProvider.FIELD_CALLER_FILE_NAME, CALLER_DATA.getFileName());
        inOrder.verify(generator).writeNumberField(CallerDataJsonProvider.FIELD_CALLER_LINE_NUMBER, CALLER_DATA.getLineNumber());
    }

    @Test
    public void testWrapped() throws IOException {
        setupCallerData();
        
        provider.setFieldName("caller");
        
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        
        inOrder.verify(generator).writeObjectFieldStart("caller");
        inOrder.verify(generator).writeStringField(CallerDataJsonProvider.FIELD_CALLER_CLASS_NAME, CALLER_DATA.getClassName());
        inOrder.verify(generator).writeStringField(CallerDataJsonProvider.FIELD_CALLER_METHOD_NAME, CALLER_DATA.getMethodName());
        inOrder.verify(generator).writeStringField(CallerDataJsonProvider.FIELD_CALLER_FILE_NAME, CALLER_DATA.getFileName());
        inOrder.verify(generator).writeNumberField(CallerDataJsonProvider.FIELD_CALLER_LINE_NUMBER, CALLER_DATA.getLineNumber());
        inOrder.verify(generator).writeEndObject();
    }

    @Test
    public void testNoCallerData() throws IOException {
        provider.writeTo(generator, event);
        
        verifyNoMoreInteractions(generator);
        
    }

    @Test
    public void testFieldName() throws IOException {
        setupCallerData();
        
        provider.setFieldName("caller");
        provider.setClassFieldName("class");
        provider.setMethodFieldName("method");
        provider.setFileFieldName("file");
        provider.setLineFieldName("line");
        
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        
        inOrder.verify(generator).writeObjectFieldStart("caller");
        inOrder.verify(generator).writeStringField("class", CALLER_DATA.getClassName());
        inOrder.verify(generator).writeStringField("method", CALLER_DATA.getMethodName());
        inOrder.verify(generator).writeStringField("file", CALLER_DATA.getFileName());
        inOrder.verify(generator).writeNumberField("line", CALLER_DATA.getLineNumber());
        inOrder.verify(generator).writeEndObject();
    }

    @Test
    public void testFieldNames() throws IOException {
        setupCallerData();

        LogstashFieldNames fieldNames = new LogstashFieldNames();
        fieldNames.setCaller("caller");
        fieldNames.setCallerClass("class");
        fieldNames.setCallerMethod("method");
        fieldNames.setCallerFile("file");
        fieldNames.setCallerLine("line");
        
        provider.setFieldNames(fieldNames);
        
        provider.writeTo(generator, event);
        
        InOrder inOrder = inOrder(generator);
        
        inOrder.verify(generator).writeObjectFieldStart("caller");
        inOrder.verify(generator).writeStringField("class", CALLER_DATA.getClassName());
        inOrder.verify(generator).writeStringField("method", CALLER_DATA.getMethodName());
        inOrder.verify(generator).writeStringField("file", CALLER_DATA.getFileName());
        inOrder.verify(generator).writeNumberField("line", CALLER_DATA.getLineNumber());
        inOrder.verify(generator).writeEndObject();
    }

    @Test
    public void testPrepareForDeferredProcessing() throws IOException {
        provider.prepareForDeferredProcessing(event);
        
        verify(event).getCallerData();
    }

}
