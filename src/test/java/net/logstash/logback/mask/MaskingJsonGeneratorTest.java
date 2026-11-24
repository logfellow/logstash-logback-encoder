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
package net.logstash.logback.mask;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.core.Base64Variants;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.io.SerializedString;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.NullNode;

class MaskingJsonGeneratorTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    static Stream<Arguments> arrayValues() {
        return Stream.of(
                value("string", g -> g.writeString("secret")),
                value("chars", g -> g.writeString("secret".toCharArray(), 0, 6)),
                value("serialized string", g -> g.writeString(new SerializedString("secret"))),
                value("reader", g -> g.writeString(new StringReader("secret"), 6)),
                value("utf8", g -> g.writeUTF8String("secret".getBytes(StandardCharsets.UTF_8), 0, 6)),
                value("short", g -> g.writeNumber((short) 42)),
                value("int", g -> g.writeNumber(42)),
                value("long", g -> g.writeNumber(42L)),
                value("float", g -> g.writeNumber(42F)),
                value("double", g -> g.writeNumber(42D)),
                value("big integer", g -> g.writeNumber(BigInteger.TEN)),
                value("big decimal", g -> g.writeNumber(BigDecimal.TEN)),
                value("encoded number", g -> g.writeNumber("42")),
                value("number chars", g -> g.writeNumber("42".toCharArray(), 0, 2)),
                value("embedded binary", g -> g.writeEmbeddedObject(new byte[] {1, 2})),
                value("boolean", g -> g.writeBoolean(true)),
                value("null", JsonGenerator::writeNull),
                value("binary", g -> g.writeBinary(new byte[] {1, 2})),
                value("binary slice", g -> g.writeBinary(new byte[] {1, 2}, 0, 2)),
                value("base64 binary", g -> g.writeBinary(Base64Variants.getDefaultVariant(), new byte[] {1, 2}, 0, 2)),
                value("binary stream", g -> g.writeBinary(new ByteArrayInputStream(new byte[] {1, 2}), 2)),
                value("base64 stream", g -> g.writeBinary(Base64Variants.getDefaultVariant(), new ByteArrayInputStream(new byte[] {1, 2}), 2)),
                value("object", g -> {
                    g.writeStartObject();
                    g.writeStringProperty("secret", "value");
                    g.writeEndObject();
                }),
                value("object with value", g -> {
                    g.writeStartObject(new Object());
                    g.writeEndObject();
                }),
                value("object with size", g -> {
                    g.writeStartObject(new Object(), 0);
                    g.writeEndObject();
                }),
                value("array", g -> {
                    g.writeStartArray();
                    g.writeString("secret");
                    g.writeEndArray();
                }),
                value("array with value", g -> {
                    g.writeStartArray(new Object());
                    g.writeEndArray();
                }),
                value("array with size", g -> {
                    g.writeStartArray(new Object(), 0);
                    g.writeEndArray();
                }),
                value("pojo", g -> g.writePOJO(Map.of("secret", List.of(1, 2)))),
                value("tree", g -> g.writeTree(MAPPER.readTree("{\"secret\":[1,2]}"))),
                value("copy structure", g -> {
                    try (var parser = MAPPER.createParser("{\"secret\":[1,2]}")) {
                        parser.nextToken();
                        g.copyCurrentStructure(parser);
                    }
                }));
    }

    private static Arguments value(String name, Consumer<JsonGenerator> writer) {
        return Arguments.of(name, writer);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("arrayValues")
    void masksWholeArrayElement(String name, Consumer<JsonGenerator> writer) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (JsonGenerator generator = maskingGenerator(output, "/1", "****")) {
            generator.writeStartArray();
            generator.writeString("before");
            writer.accept(generator);
            generator.writeString("after");
            generator.writeEndArray();
        }
        assertThat(output.toString(StandardCharsets.UTF_8)).isEqualTo("[\"before\",\"****\",\"after\"]");
    }

    @ParameterizedTest(name = "field {0}")
    @MethodSource("arrayValues")
    void preservesFieldMasking(String name, Consumer<JsonGenerator> writer) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (JsonGenerator generator = maskingGenerator(output, "/secret", "****")) {
            generator.writeStartObject();
            generator.writeName("secret");
            writer.accept(generator);
            generator.writeStringProperty("after", "visible");
            generator.writeEndObject();
        }
        assertThat(output.toString(StandardCharsets.UTF_8)).isEqualTo("{\"secret\":\"****\",\"after\":\"visible\"}");
    }

    static Stream<Arguments> bulkArrays() {
        return Stream.of(
                value("int", g -> g.writeArray(new int[] {0, 1, 2, 3, 4}, 1, 3)),
                value("long", g -> g.writeArray(new long[] {0, 1, 2, 3, 4}, 1, 3)),
                value("double", g -> g.writeArray(new double[] {0, 1, 2, 3, 4}, 1, 3)),
                value("string", g -> g.writeArray(new String[] {"0", "1", "2", "3", "4"}, 1, 3)));
    }

    @ParameterizedTest(name = "bulk {0}")
    @MethodSource("bulkArrays")
    void masksBulkArrayElements(String name, Consumer<JsonGenerator> writer) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (JsonGenerator generator = maskingGenerator(output, "/1", "****")) {
            writer.accept(generator);
        }
        var array = MAPPER.readTree(output.toByteArray());
        assertThat(array.size()).isEqualTo(3);
        assertThat(array.get(0).asString()).isEqualTo("double".equals(name) ? "1.0" : "1");
        assertThat(array.get(1).asString()).isEqualTo("****");
        assertThat(array.get(2).asString()).isEqualTo("double".equals(name) ? "3.0" : "3");
    }

    @ParameterizedTest(name = "masked bulk {0}")
    @MethodSource("bulkArrays")
    void masksWholeBulkArray(String name, Consumer<JsonGenerator> writer) {
        masksWholeArrayElement(name, writer);
        preservesFieldMasking(name, writer);
    }

    static Stream<Arguments> rawValues() {
        return Stream.of(
                value("string", g -> g.writeRawValue("42")),
                value("string slice", g -> g.writeRawValue("[42]", 1, 2)),
                value("chars", g -> g.writeRawValue("42".toCharArray(), 0, 2)),
                value("utf8", g -> g.writeRawUTF8String("secret".getBytes(StandardCharsets.UTF_8), 0, 6)));
    }

    @ParameterizedTest(name = "raw {0}")
    @MethodSource("rawValues")
    void rawValuesKeepExistingMaskingBehavior(String name, Consumer<JsonGenerator> writer) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (JsonGenerator generator = maskingGenerator(output, "/0", "****")) {
            generator.writeStartArray();
            writer.accept(generator);
            generator.writeString("after");
            generator.writeEndArray();
        }
        assertThat(output.toString(StandardCharsets.UTF_8))
                .isEqualTo("utf8".equals(name) ? "[\"secret\",\"after\"]" : "[42,\"after\"]");
        preservesFieldMasking(name, writer);
    }

    @Test
    void exposesUpcomingArrayIndexToCustomMasker() {
        Object arrayValue = new Object();
        int[] calls = {0};
        FieldMasker masker = context -> {
            calls[0]++;
            assertThat(context.inArray()).isTrue();
            assertThat(context.hasCurrentName()).isFalse();
            assertThat(context.hasCurrentIndex()).isTrue();
            assertThat(context.getCurrentIndex()).isEqualTo(calls[0] - 1);
            assertThat(context.getEntryCount()).isEqualTo(calls[0]);
            assertThat(context.getParent().inRoot()).isTrue();
            assertThat(context.getNestingDepth()).isEqualTo(1);
            assertThat(context.currentValue()).isSameAs(arrayValue);
            return context.getCurrentIndex() == 1 ? "****" : null;
        };
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (JsonGenerator generator = new MaskingJsonGenerator(MAPPER.createGenerator(output), List.of(masker), List.of())) {
            generator.writeStartArray(arrayValue);
            generator.writeNumber(1);
            generator.writePOJO(Map.of("secret", List.of(2, 3)));
            generator.writeNumber(4);
            generator.writeEndArray();
        }
        assertThat(calls[0]).isEqualTo(3);
        assertThat(output.toString(StandardCharsets.UTF_8)).isEqualTo("[1,\"****\",4]");
    }

    @Test
    void consumesMaskedBinaryStream() {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {1, 2, 3});
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (JsonGenerator generator = maskingGenerator(output, "/0", "****")) {
            generator.writeStartArray();
            assertThat(generator.writeBinary(input, -1)).isEqualTo(3);
            generator.writeNumber(4);
            generator.writeEndArray();
        }
        assertThat(input.available()).isZero();
        assertThat(output.toString(StandardCharsets.UTF_8)).isEqualTo("[\"****\",4]");
    }

    @Test
    void writesMaskWithoutMaskingItsContents() {
        for (Object mask : List.of(NullNode.instance, List.of("masked", Map.of("key", 1)), Map.of("key", true))) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (JsonGenerator generator = maskingGenerator(output, "/0", mask)) {
                generator.writePOJO(List.of(Map.of("secret", List.of(1, 2)), "after"));
            }
            assertThat(MAPPER.readTree(output.toByteArray())).isEqualTo(MAPPER.valueToTree(List.of(mask, "after")));
        }
    }

    private JsonGenerator maskingGenerator(ByteArrayOutputStream output, String path, Object mask) {
        return new MaskingJsonGenerator(MAPPER.createGenerator(output),
                List.of(new PathBasedFieldMasker(path, mask)), List.of());
    }
}
