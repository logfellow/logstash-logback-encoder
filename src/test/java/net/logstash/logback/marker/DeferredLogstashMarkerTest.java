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
package net.logstash.logback.marker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class DeferredLogstashMarkerTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(JsonWriteFeature.ESCAPE_NON_ASCII)
            .build();

    public static class MyClass {
        private String myField;

        public MyClass(String myField) {
            this.myField = myField;
        }

        public String getMyField() {
            return myField;
        }

        public void setMyField(String myField) {
            this.myField = myField;
        }
    }

    @Test
    public void testWriteTo() {

        @SuppressWarnings("unchecked")
        Supplier<MyClass> supplier = mock(Supplier.class);

        when(supplier.get()).thenReturn(new MyClass("value"));

        StringWriter writer = new StringWriter();
        JsonGenerator generator = MAPPER.createGenerator(writer);

        LogstashMarker marker = Markers.defer(() -> Markers.append("myObject", supplier.get()));

        verify(supplier, never()).get();

        generator.writeStartObject();
        marker.writeTo(generator);
        generator.writeEndObject();
        generator.flush();

        verify(supplier).get();

        assertThat(writer).hasToString("{\"myObject\":{\"myField\":\"value\"}}");

        // execute again, to ensure that supplier is not invoked again
        generator.writeStartObject();
        marker.writeTo(generator);
        generator.writeEndObject();
        generator.flush();

        verify(supplier).get();

        assertThat(writer).hasToString("{\"myObject\":{\"myField\":\"value\"}} {\"myObject\":{\"myField\":\"value\"}}");

    }

    @Test
    public void testEquals() {
        MyClass myObject = new MyClass("value");

        LogstashMarker marker = Markers.defer(() -> Markers.append("myObject", myObject));

        assertThat(marker)
            .isNotEqualTo(Markers.defer(() -> Markers.append("myObject", myObject)));

    }

    @Test
    public void testHashCode() {
        MyClass myObject = new MyClass("value");

        assertThat(Markers.defer(() -> Markers.append("myObject", myObject)))
            .doesNotHaveSameHashCodeAs(Markers.defer(() -> Markers.append("myObject", myObject)));
    }
}
