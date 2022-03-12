/*
 * Copyright 2013-2022 the original author or authors.
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

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * JsonGenerator with an optimized implementation of the {@link #writeObject(Object)} method that tries
 * to call appropriate write method for the given untyped Object and delegates to the underlying generator
 * as fallback.
 */
public class SimpleObjectJsonGeneratorDelegate extends JsonGeneratorDelegate {
    public SimpleObjectJsonGeneratorDelegate(JsonGenerator delegate) {
        super(delegate, false);
    }
    
    @Override
    public void writeObject(Object value) throws IOException {
        if (value == null) {
            writeNull();
            return;
        }
        if (value instanceof String) {
            writeString((String) value);
            return;
        }
        if (value instanceof Number) {
            Number n = (Number) value;
            if (n instanceof Integer) {
                writeNumber(n.intValue());
                return;
            }
            if (n instanceof Long) {
                writeNumber(n.longValue());
                return;
            }
            if (n instanceof Double) {
                writeNumber(n.doubleValue());
                return;
            }
            if (n instanceof Float) {
                writeNumber(n.floatValue());
                return;
            }
            if (n instanceof Short) {
                writeNumber(n.shortValue());
                return;
            }
            if (n instanceof Byte) {
                writeNumber(n.byteValue());
                return;
            }
            if (n instanceof BigInteger) {
                writeNumber((BigInteger) n);
                return;
            }
            if (n instanceof BigDecimal) {
                writeNumber((BigDecimal) n);
                return;
            }
            if (n instanceof AtomicInteger) {
                writeNumber(((AtomicInteger) n).get());
                return;
            }
            if (n instanceof AtomicLong) {
                writeNumber(((AtomicLong) n).get());
                return;
            }
        }
        if (value instanceof byte[]) {
            writeBinary((byte[]) value);
            return;
        }
        if (value instanceof Boolean) {
            writeBoolean((Boolean) value);
            return;
        }
        if (value instanceof AtomicBoolean) {
            writeBoolean(((AtomicBoolean) value).get());
            return;
        }
        if (value instanceof JsonNode) {
            JsonNode node = (JsonNode) value;
            
            switch (node.getNodeType()) {
                case NULL:
                    writeNull();
                    return;
                    
                case STRING:
                    writeString(node.asText());
                    return;
                    
                case BOOLEAN:
                    writeBoolean(node.asBoolean());
                    return;
                    
                case BINARY:
                    writeBinary(node.binaryValue());
                    return;
                    
                case NUMBER:
                    if (node.isInt()) {
                        writeNumber(node.intValue());
                        return;
                    }
                    if (node.isLong()) {
                        writeNumber(node.longValue());
                        return;
                    }
                    if (node.isShort()) {
                        writeNumber(node.shortValue());
                        return;
                    }
                    if (node.isDouble()) {
                        writeNumber(node.doubleValue());
                        return;
                    }
                    if (node.isFloat()) {
                        writeNumber(node.floatValue());
                        return;
                    }
                    if (node.isBigDecimal()) {
                        writeNumber(node.decimalValue());
                        return;
                    }
                    if (node.isBigInteger()) {
                        writeNumber(node.bigIntegerValue());
                        return;
                    }
                    
                case OBJECT:
                    writeStartObject(node);
                    for (Iterator<Entry<String, JsonNode>> entries = ((ObjectNode) node).fields(); entries.hasNext();) {
                        Entry<String, JsonNode> entry = entries.next();
                        writeObjectField(entry.getKey(), entry.getValue());
                    }
                    writeEndObject();
                    return;
                    
                case ARRAY:
                    ArrayNode arrayNode = (ArrayNode) node;
                    int size = arrayNode.size();
                    writeStartArray(arrayNode, size);
                    for (Iterator<JsonNode> elements = arrayNode.elements(); elements.hasNext();) {
                        writeObject(elements.next());
                    }
                    writeEndArray();
                    return;
                    
                default:
                    // default case is handled below
                    break;
            }
        }
        

        // Default case if not handled by one of the specialized methods above
        //
        delegate.writeObject(value);
    }
}
