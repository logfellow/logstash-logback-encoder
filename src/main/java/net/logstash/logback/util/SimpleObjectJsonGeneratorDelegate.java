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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.util.JsonGeneratorDelegate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

/**
 * JsonGenerator with an optimized implementation of the {@link #writePOJO(Object)} method that tries
 * to call appropriate write method for the given untyped Object and delegates to the underlying generator
 * as fallback.
 */
public class SimpleObjectJsonGeneratorDelegate extends JsonGeneratorDelegate {
    public SimpleObjectJsonGeneratorDelegate(JsonGenerator delegate) {
        super(delegate, false);
    }

    @Override
    public JsonGenerator writePOJO(Object value) {
        if (value == null) {
            writeNull();
            return this;
        }
        if (value instanceof String) {
            writeString((String) value);
            return this;
        }
        if (value instanceof Number n) {
            if (n instanceof Integer) {
                writeNumber(n.intValue());
                return this;
            }
            if (n instanceof Long) {
                writeNumber(n.longValue());
                return this;
            }
            if (n instanceof Double) {
                writeNumber(n.doubleValue());
                return this;
            }
            if (n instanceof Float) {
                writeNumber(n.floatValue());
                return this;
            }
            if (n instanceof Short) {
                writeNumber(n.shortValue());
                return this;
            }
            if (n instanceof Byte) {
                writeNumber(n.byteValue());
                return this;
            }
            if (n instanceof BigInteger) {
                writeNumber((BigInteger) n);
                return this;
            }
            if (n instanceof BigDecimal) {
                writeNumber((BigDecimal) n);
                return this;
            }
            if (n instanceof AtomicInteger) {
                writeNumber(((AtomicInteger) n).get());
                return this;
            }
            if (n instanceof AtomicLong) {
                writeNumber(((AtomicLong) n).get());
                return this;
            }
        }
        if (value instanceof byte[]) {
            writeBinary((byte[]) value);
            return this;
        }
        if (value instanceof Boolean) {
            writeBoolean((Boolean) value);
            return this;
        }
        if (value instanceof AtomicBoolean) {
            writeBoolean(((AtomicBoolean) value).get());
            return this;
        }
        if (value instanceof JsonNode node) {

            switch (node.getNodeType()) {
                case NULL:
                    writeNull();
                    return this;
                    
                case STRING:
                    writeString(node.asString());
                    return this;
                    
                case BOOLEAN:
                    writeBoolean(node.asBoolean());
                    return this;
                    
                case BINARY:
                    writeBinary(node.binaryValue());
                    return this;
                    
                case NUMBER:
                    if (node.isInt()) {
                        writeNumber(node.intValue());
                        return this;
                    }
                    if (node.isLong()) {
                        writeNumber(node.longValue());
                        return this;
                    }
                    if (node.isShort()) {
                        writeNumber(node.shortValue());
                        return this;
                    }
                    if (node.isDouble()) {
                        writeNumber(node.doubleValue());
                        return this;
                    }
                    if (node.isFloat()) {
                        writeNumber(node.floatValue());
                        return this;
                    }
                    if (node.isBigDecimal()) {
                        writeNumber(node.decimalValue());
                        return this;
                    }
                    if (node.isBigInteger()) {
                        writeNumber(node.bigIntegerValue());
                        return this;
                    }
                    
                case OBJECT:
                    writeStartObject(node);
                    for (Entry<String, JsonNode> entry : node.properties()) {
                        writePOJOProperty(entry.getKey(), entry.getValue());
                    }
                    writeEndObject();
                    return this;
                    
                case ARRAY:
                    ArrayNode arrayNode = (ArrayNode) node;
                    int size = arrayNode.size();
                    writeStartArray(arrayNode, size);
                    for (JsonNode jsonNode : arrayNode.elements()) {
                        writePOJO(jsonNode);
                    }
                    writeEndArray();
                    return this;
                    
                default:
                    // default case is handled below
                    break;
            }
        }
        

        // Default case if not handled by one of the specialized methods above
        //
        delegate.writePOJO(value);
        return this;
    }
}
