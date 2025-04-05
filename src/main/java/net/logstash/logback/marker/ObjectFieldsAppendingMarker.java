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

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import net.logstash.logback.argument.StructuredArgument;
import net.logstash.logback.argument.StructuredArguments;
import net.logstash.logback.composite.loggingevent.ArgumentsJsonProvider;
import net.logstash.logback.composite.loggingevent.LogstashMarkersJsonProvider;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.util.NameTransformer;
import org.slf4j.Marker;

/**
 * A {@link Marker} OR {@link StructuredArgument} that
 * that "unwraps" the given object into the logstash event.
 * <p>
 *
 * When writing to the JSON data (via {@link ArgumentsJsonProvider} or {@link LogstashMarkersJsonProvider}),
 * the fields of the object are written inline into the JSON event
 * similar to how the {@link com.fasterxml.jackson.annotation.JsonUnwrapped} annotation works.
 * <p>
 *
 * When writing to a String (when used as a {@link StructuredArgument} to the event's formatted message),
 * {@link StructuredArguments#toString(Object)} is used to convert the object to a string.
 * <p>
 *
 * For example, if the message is "mymessage {}", and the object argument is:
 *
 * <pre>
 * {
 *     name1 : "value1",
 *     name2 : 5,
 *     name3 : [1, 2, 3],
 *     name4 : {
 *         name5 : 6
 *     }
 * }
 * </pre>
 * <p>
 * Then the message, name1, name2, name3, name4 fields will be added to the json for the logstash event.
 * <p>
 * For example:
 *
 * <pre>
 * {
 *     "message" : "mymessage objectsToStringValue",
 *     "name1" : "value1",
 *     "name2" : 5,
 *     "name3" : [1, 2, 3],
 *     "name4" : { "name5" : 6 }
 * }
 * </pre>
 *
 * Note that if the object cannot be unwrapped, then nothing will be written.
 */
@SuppressWarnings("serial")
public class ObjectFieldsAppendingMarker extends LogstashMarker implements StructuredArgument {

    public static final String MARKER_NAME = LogstashMarker.MARKER_NAME_PREFIX + "OBJECT_FIELDS";

    private final Object object;

    /*
     * Would really like to use Guava's Cache for these two, with expiring entries, soft reference, etc.
     * But didn't want to introduce a dependency on Guava.
     *
     * Since apps will typically serialize the same types of objects repeatedly, they shouldn't grow too much.
     */
    private static final Map<ObjectMapper, SerializerHelper> serializerHelper = new ConcurrentHashMap<>();

    public ObjectFieldsAppendingMarker(Object object) {
        super(MARKER_NAME);
        this.object = object;
    }

    @Override
    public void writeTo(JsonGenerator generator) throws IOException {
        if (this.object != null) {
            SerializerHelper helper = getSerializerHelper(generator);
            helper.serialize(generator, this.object);
        }
    }
    
    
    @Override
    public String toStringSelf() {
        return StructuredArguments.toString(object);
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof ObjectFieldsAppendingMarker)) {
            return false;
        }

        ObjectFieldsAppendingMarker other = (ObjectFieldsAppendingMarker) obj;
        return Objects.equals(this.object, other.object);
    }

    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + super.hashCode();
        result = prime * result + (this.object == null ? 0 : this.object.hashCode());
        return result;
    }
    
    
    /**
     * Get a {@link SerializerHelper} suitable for use with the given {@link JsonGenerator}.
     * 
     * @param gen the {@link JsonGenerator} for which an helper should be returned
     * @return a {@link SerializerHelper}
     */
    private static SerializerHelper getSerializerHelper(JsonGenerator gen) {
        ObjectMapper mapper = (ObjectMapper) gen.getCodec();
        return serializerHelper.computeIfAbsent(mapper, SerializerHelper::new);
    }
    
    private static class SerializerHelper {
        private final SerializerProvider serializers;
        private final ConcurrentHashMap<Class<?>, JsonSerializer<Object>> cache = new ConcurrentHashMap<>();
        
        SerializerHelper(ObjectMapper mapper) {
            this.serializers = mapper.getSerializerProviderInstance();
        }
        
        /**
         * Serialize the given value using the supplied generator
         * 
         * @param gen the {@link JsonGenerator} to use to serialize the value
         * @param value the value to serialize
         * @throws IOException thrown when the underlying {@link JsonGenerator} could not be created
         *                     or when it has problems to serialize the given value
         */
        public void serialize(JsonGenerator gen, Object value) throws IOException {
            if (value != null) {
                JsonSerializer<Object> unwrappingSerializer = getUnwrappingSerializer(value.getClass());
                
                /*
                 * Make sure the serializer accepts to serialize in an "unwrapped" fashion.
                 * This may not be the case for serializer for Number types for instance.
                 */
                if (unwrappingSerializer.isUnwrappingSerializer()) {
                    unwrappingSerializer.serialize(value, gen, serializers);
                }
            }
        }
        
        private JsonSerializer<Object> getUnwrappingSerializer(Class<?> type) throws JsonMappingException {
            JsonSerializer<Object> serializer = cache.get(type);
            if (serializer == null) {
                serializer = createUnwrappingSerializer(type);
                cache.put(type, serializer);
            }
            return serializer;
        }
        
        private JsonSerializer<Object> createUnwrappingSerializer(Class<?> type) throws JsonMappingException {
            JsonSerializer<Object> serializer = serializers.findValueSerializer(type);
            return serializer.unwrappingSerializer(NameTransformer.NOP);
        }
    }
}
