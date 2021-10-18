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
package net.logstash.logback.composite;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author brenuart
 *
 */
public class JsonReadingUtils {

    private JsonReadingUtils() {
        // utility class - prevent instantiation
    }
    
    
    /**
     * Read a JSON string into the equivalent {@link JsonNode}.
     * 
     * <p>May be instructed to throw a {@link JsonParseException} if the string is not fully read
     * after a first valid JsonNode is found. This may happen for input like <em>10 foobar</em> that
     * would otherwise return a NumericNode with value {@code 10} leaving <em>foobar</em> unread.
     * 
     * @param jsonFactory the {@link JsonFactory} from which to obtain a {@link JsonParser} to read the JSON string.
     * @param json the JSON string to read
     * @param readFully whether to throw a {@link JsonParseException} when the input is not fully read.
     * @return the {@link JsonNode} corresponding to the input string or {@code null} if the string is null or empty.
     * @throws IOException if there is either an underlying I/O problem or decoding issue
     */
    public static JsonNode read(JsonFactory jsonFactory, String json, boolean readFully) throws IOException {
        if (json == null) {
            return null;
        }
        
        final String trimmedJson = json.trim();
        try (JsonParser parser = jsonFactory.createParser(trimmedJson)) {
            final JsonNode tree = parser.readValueAsTree();
            
            if (readFully && parser.getCurrentLocation().getCharOffset() < trimmedJson.length()) {
                /*
                 * If the full trimmed string was not read, then the full trimmed string contains a json value plus other text.
                 * For example, trimmedValue = '10 foobar', or 'true foobar', or '{"foo","bar"} baz'.
                 * In these cases readTree will only read the first part, and will not read the remaining text.
                 */
                throw new JsonParseException(parser, "unexpected character");
            }
            
            return tree;
        }
    }
    
    
    /**
     * Fully read the supplied JSON string into the equivalent {@link JsonNode} throwing a {@link JsonParseException}
     * if some trailing characters remain after a first valid JsonNode is found.
     * 
     * @param jsonFactory the {@link JsonFactory} from which to obtain a {@link JsonParser} to read the JSON string.
     * @param json the JSON string to read
     * @return the {@link JsonNode} corresponding to the input string or {@code null} if the string is null or empty.
     * @throws IOException if there is either an underlying I/O problem or decoding issue
     * 
     * @see JsonReadingUtils#readAsObjectNode(JsonFactory, String, boolean)
     */
    public static JsonNode readFully(JsonFactory jsonFactory, String json) throws IOException {
        return read(jsonFactory, json, true);
    }
    
    
    /**
     * Read a JSON string into an {@link ObjectNode}, throwing a {@link JsonParseException} if the supplied string is not
     * a valid JSON object representation.
     * 
     * @param jsonFactory the {@link JsonFactory} from which to obtain a {@link JsonParser} to read the JSON string.
     * @param json the JSON string to read
     * @param readFully whether to throw a {@link JsonParseException} when the input is not fully read.
     * @return the {@link JsonNode} corresponding to the input string or {@code null} if the string is null or empty.
     * @throws IOException if there is either an underlying I/O problem or decoding issue
     * 
     * @see JsonReadingUtils#readAsObjectNode(JsonFactory, String, boolean)
     */
    public static ObjectNode readAsObjectNode(JsonFactory jsonFactory, String json, boolean readFully) throws IOException {
        final JsonNode node = read(jsonFactory, json, readFully);
        
        if (node != null && !(node instanceof ObjectNode)) {
            throw new JsonParseException(null, "expected a JSON object representation");
        }
        
        return (ObjectNode) node;
    }

    
    /**
     * Fully read a JSON string into an {@link ObjectNode}, throwing a {@link JsonParseException} if the supplied string
     * is not a valid JSON object representation.
     * 
     * @param jsonFactory the {@link JsonFactory} from which to obtain a {@link JsonParser} to read the JSON string.
     * @param json the JSON string to read
     * @return the {@link JsonNode} corresponding to the input string or {@code null} if the string is null or empty.
     * @throws IOException if there is either an underlying I/O problem or decoding issue
     * 
     * @see JsonReadingUtils#readAsObjectNode(JsonFactory, String, boolean)
     */
    public static ObjectNode readFullyAsObjectNode(JsonFactory jsonFactory, String json) throws IOException {
        return readAsObjectNode(jsonFactory, json, true);
    }
}
