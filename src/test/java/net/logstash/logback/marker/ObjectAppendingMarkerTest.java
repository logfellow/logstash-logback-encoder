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

import java.io.IOException;
import java.io.StringWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import org.junit.jupiter.api.Test;

public class ObjectAppendingMarkerTest {
    
    private static final JsonFactory FACTORY = new MappingJsonFactory().enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);
    
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
    public void testWriteTo() throws IOException {
        
        MyClass myObject = new MyClass("value");
        
        StringWriter writer = new StringWriter();
        JsonGenerator generator = FACTORY.createGenerator(writer);
        
        LogstashMarker marker = Markers.append("myObject", myObject);
        generator.writeStartObject();
        marker.writeTo(generator);
        generator.writeEndObject();
        generator.flush();
        
        assertThat(writer).hasToString("{\"myObject\":{\"myField\":\"value\"}}");
    }
    
    @Test
    public void testEquals() {
        MyClass myObject = new MyClass("value");
        
        assertThat(Markers.append("myObject", myObject)).isEqualTo(Markers.append("myObject", myObject));
        
        assertThat(Markers.append("myObject", myObject)).isNotEqualTo(Markers.append("myObject", new MyClass("value1")));
        
        assertThat(Markers.append("myObject", myObject)).isNotEqualTo(Markers.append("myDifferentObject", myObject));
    }
    
    @Test
    public void testHashCode() {
        MyClass myObject = new MyClass("value");
        
        assertThat(Markers.append("myObject", myObject)).hasSameHashCodeAs(Markers.append("myObject", myObject));
        
        assertThat(Markers.append("myObject", myObject)).doesNotHaveSameHashCodeAs(Markers.append("myObject", new MyClass("value1")));
        
        assertThat(Markers.append("myObject", myObject)).doesNotHaveSameHashCodeAs(Markers.append("myDifferentObject", myObject));
    }
    
}
