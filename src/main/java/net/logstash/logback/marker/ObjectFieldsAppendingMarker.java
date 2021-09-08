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
package net.logstash.logback.marker;

import java.io.IOException;
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
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
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
    private static final ConcurrentHashMap<Class<?>, JsonSerializer<Object>> beanSerializers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<ObjectMapper, SerializerProvider> serializerProviders = new ConcurrentHashMap<>();

    public ObjectFieldsAppendingMarker(Object object) {
        super(MARKER_NAME);
        this.object = object;
    }

    @Override
    public void writeTo(JsonGenerator generator) throws IOException {
        if (object != null) {
            ObjectMapper mapper = (ObjectMapper) generator.getCodec();
            JsonSerializer<Object> serializer = getBeanSerializer(mapper);
            if (serializer.isUnwrappingSerializer()) {
                serializer.serialize(object, generator, getSerializerProvider(mapper));
            }
        }
    }

    @Override
    public String toStringSelf() {
        return StructuredArguments.toString(object);
    }

    /**
     * Gets a serializer that will write the {@link #object} unwrapped.
     */
    private JsonSerializer<Object> getBeanSerializer(ObjectMapper mapper) throws JsonMappingException {

        JsonSerializer<Object> jsonSerializer = beanSerializers.get(object.getClass());

        if (jsonSerializer == null) {
            SerializerProvider serializerProvider = getSerializerProvider(mapper);
            JsonSerializer<Object> newSerializer = mapper.getSerializerFactory().createSerializer(
                    serializerProvider,
                    mapper.getSerializationConfig().constructType(object.getClass()))
                .unwrappingSerializer(NameTransformer.NOP);

            if (newSerializer instanceof ResolvableSerializer) {
                ((ResolvableSerializer) newSerializer).resolve(serializerProvider);
            }

            JsonSerializer<Object> existingSerializer = beanSerializers.putIfAbsent(
                    object.getClass(),
                    newSerializer);

            jsonSerializer = (existingSerializer == null) ? newSerializer : existingSerializer;
        }
        return jsonSerializer;

    }

    /**
     * Gets a {@link SerializerProvider} configured with the {@link ObjectMapper}'s {@link SerializationConfig}
     * ({@link ObjectMapper#getSerializationConfig()}) to be used for serialization.
     * <p>
     * Note that the {@link ObjectMapper}'s {@link SerializerProvider} ({@link ObjectMapper#getSerializerProvider()})
     * cannot be used directly, because the {@link SerializerProvider}'s {@link SerializationConfig} ({@link SerializerProvider#getConfig()}) is null,
     * which causes NullPointerExceptions when it is used.
     */
    private SerializerProvider getSerializerProvider(ObjectMapper mapper) {

        SerializerProvider provider = serializerProviders.get(mapper);
        if (provider == null) {

            SerializerProvider newProvider = ((DefaultSerializerProvider) mapper.getSerializerProvider())
                    .createInstance(mapper.getSerializationConfig(), mapper.getSerializerFactory());

            SerializerProvider existingProvider = serializerProviders.putIfAbsent(mapper, newProvider);

            provider = (existingProvider == null) ? newProvider : existingProvider;
        }
        return provider;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ObjectFieldsAppendingMarker && super.equals(obj))) {
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
}
