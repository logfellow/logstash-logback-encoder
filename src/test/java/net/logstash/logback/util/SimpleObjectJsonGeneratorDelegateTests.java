/*
 * Copyright 2013-2025 the original author or authors.
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
package net.logstash.logback.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ShortNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author brenuart
 *
 */
public class SimpleObjectJsonGeneratorDelegateTests {

    private static final JsonFactory FACTORY = new MappingJsonFactory();

    private JsonGenerator delegate;
    private SimpleObjectJsonGeneratorDelegate generator;
    
    @BeforeEach
    public void setup() throws IOException {
        delegate = spy(FACTORY.createGenerator(new StringWriter()));
        generator = new SimpleObjectJsonGeneratorDelegate(delegate);
    }
    
    @Test
    public void writeObject_Null() throws IOException {
        generator.writeObject(null);
        
        verify(delegate).writeNull();
    }
    
    
    @Test
    public void writeObject_String() throws IOException {
        generator.writeObject("foo");
        
        verify(delegate).writeString("foo");
    }
    
    
    @Test
    public void writeObject_Boolean() throws IOException {
        generator.writeObject(Boolean.TRUE);
        generator.writeObject(new AtomicBoolean(false));
        
        verify(delegate).writeBoolean(true);
        verify(delegate).writeBoolean(false);
    }
    
    
    @Test
    public void writeObject_Numbers() throws IOException {
        generator.writeObject((byte) 1);
        generator.writeObject((short) 2);
        generator.writeObject((int) 3);
        generator.writeObject((long) 4);
        generator.writeObject((double) 5);
        generator.writeObject((float) 6);
        generator.writeObject(BigInteger.valueOf(7));
        generator.writeObject(BigDecimal.valueOf(8));
        generator.writeObject(new AtomicInteger(9));
        generator.writeObject(new AtomicLong(10));
        
        verify(delegate).writeNumber((byte) 1);
        verify(delegate).writeNumber((short) 2);
        verify(delegate).writeNumber((int) 3);
        verify(delegate).writeNumber((long) 4);
        verify(delegate).writeNumber((double) 5);
        verify(delegate).writeNumber((float) 6);
        verify(delegate).writeNumber(BigInteger.valueOf(7));
        verify(delegate).writeNumber(BigDecimal.valueOf(8));
        verify(delegate).writeNumber((int) 9);
        verify(delegate).writeNumber((long) 10);
    }
    
    
    @Test
    public void writeObject_byteArray() throws IOException {
        byte[] data = new byte[] {1, 2};
        generator.writeObject(data);
        
        verify(delegate).writeBinary(any(), eq(data), eq(0), eq(2));
    }
    
    
    @Test
    public void writeObject_jsonNode_String() throws IOException {
        generator.writeObject(new TextNode("foo"));
    }
    
    
    @Test
    public void writeObject_jsonNode_Numbers() throws IOException {
        generator.writeObject(new IntNode(1));
        generator.writeObject(new LongNode(2));
        generator.writeObject(new ShortNode((short) 3));
        generator.writeObject(new FloatNode(4));
        generator.writeObject(new DoubleNode(5));
        generator.writeObject(new BigIntegerNode(BigInteger.valueOf(6)));
        generator.writeObject(new DecimalNode(BigDecimal.valueOf(7)));
        
        verify(delegate).writeNumber((int) 1);
        verify(delegate).writeNumber((long) 2);
        verify(delegate).writeNumber((short) 3);
        verify(delegate).writeNumber((float) 4);
        verify(delegate).writeNumber((double) 5);
        verify(delegate).writeNumber(BigInteger.valueOf(6));
        verify(delegate).writeNumber(BigDecimal.valueOf(7));
    }
    
    
    @Test
    public void writeObject_jsonNode_Boolean() throws IOException {
        generator.writeObject(BooleanNode.TRUE);
        
        verify(delegate).writeBoolean(true);
    }

    
    @Test
    public void writeObject_jsonNode_Object() throws IOException {
        ObjectNode node = (ObjectNode) generator.getCodec().createObjectNode();
        node.put("string", "foo");
        node.put("boolean", true);
        
        generator.writeObject(node);
        
        verify(delegate).getCodec();
        verify(delegate).writeStartObject(any());
        verify(delegate).writeFieldName("string");
        verify(delegate).writeString("foo");
        verify(delegate).writeFieldName("boolean");
        verify(delegate).writeBoolean(true);
        verify(delegate).writeEndObject();
    }
    
    
    @Test
    public void writeObject_jsonNode_Array() throws IOException {
        ArrayNode node = (ArrayNode) generator.getCodec().createArrayNode();
        node.add("string");
        node.add(true);
        node.add(1);
        
        generator.writeObject(node);
        
        verify(delegate).getCodec();
        verify(delegate).writeStartArray(any(), eq(3));
        verify(delegate).writeString("string");
        verify(delegate).writeBoolean(true);
        verify(delegate).writeNumber((int) 1);
        verify(delegate).writeEndArray();
    }
    
    
    @Test
    public void writeObject_jsonNode_Null() throws IOException {
        generator.writeObject(NullNode.instance);
        
        verify(delegate).writeNull();
    }
    
    
    @Test
    public void writeObject_POJO() throws IOException {
        MyPojo obj = new MyPojo();
        generator.writeObject(obj);
        
        verify(delegate).writeObject(obj);
    }
    
    @SuppressWarnings("unused")
    private static class MyPojo {
        private String field = "foo";

        public String getField() {
            return field;
        }
        
        public void setField(String field) {
            this.field = field;
        }
    }
}
