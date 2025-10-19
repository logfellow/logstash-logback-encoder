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

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BigIntegerNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.DecimalNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.FloatNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.LongNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.ShortNode;
import tools.jackson.databind.node.StringNode;

/**
 * @author brenuart
 *
 */
public class SimpleObjectJsonGeneratorDelegateTests {

    public static final JsonMapper MAPPER = JsonMapper.builder().build();
    private JsonGenerator delegate;
    private SimpleObjectJsonGeneratorDelegate generator;
    
    @BeforeEach
    public void setup() {
        delegate = spy(MAPPER.createGenerator(new StringWriter()));
        generator = new SimpleObjectJsonGeneratorDelegate(delegate);
    }
    
    @Test
    public void writePOJO_Null() {
        generator.writePOJO(null);
        
        verify(delegate).writeNull();
    }
    
    
    @Test
    public void writePOJO_String() {
        generator.writePOJO("foo");
        
        verify(delegate).writeString("foo");
    }
    
    
    @Test
    public void writePOJO_Boolean() {
        generator.writePOJO(Boolean.TRUE);
        generator.writePOJO(new AtomicBoolean(false));
        
        verify(delegate).writeBoolean(true);
        verify(delegate).writeBoolean(false);
    }
    
    
    @Test
    public void writePOJO_Numbers() {
        generator.writePOJO((byte) 1);
        generator.writePOJO((short) 2);
        generator.writePOJO(3);
        generator.writePOJO((long) 4);
        generator.writePOJO((double) 5);
        generator.writePOJO((float) 6);
        generator.writePOJO(BigInteger.valueOf(7));
        generator.writePOJO(BigDecimal.valueOf(8));
        generator.writePOJO(new AtomicInteger(9));
        generator.writePOJO(new AtomicLong(10));
        
        verify(delegate).writeNumber((byte) 1);
        verify(delegate).writeNumber((short) 2);
        verify(delegate).writeNumber(3);
        verify(delegate).writeNumber((long) 4);
        verify(delegate).writeNumber((double) 5);
        verify(delegate).writeNumber((float) 6);
        verify(delegate).writeNumber(BigInteger.valueOf(7));
        verify(delegate).writeNumber(BigDecimal.valueOf(8));
        verify(delegate).writeNumber(9);
        verify(delegate).writeNumber((long) 10);
    }
    
    
    @Test
    public void writePOJO_byteArray() {
        byte[] data = new byte[] {1, 2};
        generator.writePOJO(data);
        
        verify(delegate).writeBinary(any(), eq(data), eq(0), eq(2));
    }
    
    
    @Test
    public void writePOJO_jsonNode_String() {
        generator.writePOJO(new StringNode("foo"));
    }
    
    
    @Test
    public void writePOJO_jsonNode_Numbers() {
        generator.writePOJO(new IntNode(1));
        generator.writePOJO(new LongNode(2));
        generator.writePOJO(new ShortNode((short) 3));
        generator.writePOJO(new FloatNode(4));
        generator.writePOJO(new DoubleNode(5));
        generator.writePOJO(new BigIntegerNode(BigInteger.valueOf(6)));
        generator.writePOJO(new DecimalNode(BigDecimal.valueOf(7)));
        
        verify(delegate).writeNumber(1);
        verify(delegate).writeNumber((long) 2);
        verify(delegate).writeNumber((short) 3);
        verify(delegate).writeNumber((float) 4);
        verify(delegate).writeNumber((double) 5);
        verify(delegate).writeNumber(BigInteger.valueOf(6));
        verify(delegate).writeNumber(BigDecimal.valueOf(7));
    }
    
    
    @Test
    public void writePOJO_jsonNode_Boolean() {
        generator.writePOJO(BooleanNode.TRUE);
        
        verify(delegate).writeBoolean(true);
    }

    
    @Test
    public void writePOJO_jsonNode_Object() {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("string", "foo");
        node.put("boolean", true);
        
        generator.writePOJO(node);
        
        verify(delegate).writeStartObject(any());
        verify(delegate).writeName("string");
        verify(delegate).writeString("foo");
        verify(delegate).writeName("boolean");
        verify(delegate).writeBoolean(true);
        verify(delegate).writeEndObject();
    }
    
    
    @Test
    public void writePOJO_jsonNode_Array() {
        ArrayNode node = MAPPER.createArrayNode();
        node.add("string");
        node.add(true);
        node.add(1);
        
        generator.writePOJO(node);
        
        verify(delegate).writeStartArray(any(), eq(3));
        verify(delegate).writeString("string");
        verify(delegate).writeBoolean(true);
        verify(delegate).writeNumber(1);
        verify(delegate).writeEndArray();
    }
    
    
    @Test
    public void writePOJO_jsonNode_Null() {
        generator.writePOJO(NullNode.instance);
        
        verify(delegate).writeNull();
    }
    
    
    @Test
    public void writePOJO_POJO() {
        MyPojo obj = new MyPojo();
        generator.writePOJO(obj);
        
        verify(delegate).writePOJO(obj);
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
