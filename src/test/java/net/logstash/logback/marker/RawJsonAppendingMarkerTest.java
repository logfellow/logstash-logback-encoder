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

import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class RawJsonAppendingMarkerTest {
    
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(JsonWriteFeature.ESCAPE_NON_ASCII)
            .build();
    
    @Test
    public void testWriteTo() {
        
        String rawJson = "{\"myField\":\"value\"}";
        
        StringWriter writer = new StringWriter();
        JsonGenerator generator = MAPPER.createGenerator(writer);
        
        LogstashMarker marker = Markers.appendRaw("rawJson", rawJson);
        generator.writeStartObject();
        marker.writeTo(generator);
        generator.writeEndObject();
        generator.flush();
        
        assertThat(writer).hasToString("{\"rawJson\":{\"myField\":\"value\"}}");
    }
    
    @Test
    public void testEquals() {
        String rawJson = "{\"myField\":\"value\"}";
        
        assertThat(Markers.appendRaw("rawJson", rawJson)).isEqualTo(Markers.appendRaw("rawJson", rawJson));
        
        assertThat(Markers.appendRaw("rawJson", rawJson)).isNotEqualTo(Markers.appendRaw("rawJson", ""));
        
        assertThat(Markers.appendRaw("rawJson", rawJson)).isNotEqualTo(Markers.appendRaw("myDifferentObject", rawJson));
    }
    
    @Test
    public void testHashCode() {
        String rawJson = "{\"myField\":\"value\"}";
        
        assertThat(Markers.appendRaw("rawJson", rawJson)).hasSameHashCodeAs(Markers.appendRaw("rawJson", rawJson));
        
        assertThat(Markers.appendRaw("rawJson", rawJson)).doesNotHaveSameHashCodeAs(Markers.appendRaw("rawJson", ""));
        
        assertThat(Markers.appendRaw("rawJson", rawJson)).doesNotHaveSameHashCodeAs(Markers.appendRaw("myDifferentObject", rawJson));
    }
    
}
