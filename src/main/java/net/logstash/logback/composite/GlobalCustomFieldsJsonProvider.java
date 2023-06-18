/*
 * Copyright 2013-2023 the original author or authors.
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
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;

import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GlobalCustomFieldsJsonProvider<Event extends DeferredProcessingAware> extends AbstractJsonProvider<Event> implements JsonFactoryAware {
    
    /**
     * The un-parsed custom fields string to use to initialize customFields
     * when the formatter is started.
     */
    private String customFields;

    /**
     * When non-null, the fields in this JsonNode will be embedded in the logstash json.
     */
    private ObjectNode customFieldsNode;
    
    /**
     * The factory used to convert the JSON string into a valid {@link ObjectNode} when custom
     * fields are set as text instead of a pre-parsed Jackson ObjectNode.
     */
    private JsonFactory jsonFactory;

    @Override
    public void writeTo(JsonGenerator generator, Event event) throws IOException {
        writeFieldsOfNode(generator, customFieldsNode);
    }

    /**
     * Writes the fields of the given node into the generator.
     */
    private void writeFieldsOfNode(JsonGenerator generator, JsonNode node) throws IOException {
        if (node != null) {
            for (Iterator<Entry<String, JsonNode>> fields = node.fields(); fields.hasNext();) {
                Entry<String, JsonNode> field = fields.next();
                generator.writeFieldName(field.getKey());
                generator.writeTree(field.getValue());
            }
        }
    }
    
    /**
     * Start the provider.
     * 
     * <p>The provider is started even when it fails to parse the {@link #customFields} JSON string.
     * An ERROR status is emitted instead and no exception is thrown.
     */
    @Override
    public void start() {
        initializeCustomFields();
        super.start();
    }
    
    private void initializeCustomFields() {
        if (customFieldsNode != null || customFields == null) {
            return;
        }
        if (jsonFactory == null) {
            throw new IllegalStateException("JsonFactory has not been set");
        }
        
        try {
            this.customFieldsNode = JsonReadingUtils.readFullyAsObjectNode(this.jsonFactory, this.customFields);
        } catch (IOException e) {
            addError("[customFields] is not a valid JSON object", e);
        }
    }

    /**
     * Set the custom fields as a JSON string.
     * The string will be parsed when the provider is {@link #start()}.
     * 
     * @param customFields the custom fields as JSON string.
     */
    public void setCustomFields(String customFields) {
        if (isStarted()) {
            throw new IllegalStateException("Configuration cannot be changed while the provider is started");
        }
        
        this.customFields = customFields;
        this.customFieldsNode = null;
    }
    
    public String getCustomFields() {
        return customFields;
    }
    
    public ObjectNode getCustomFieldsNode() {
        return this.customFieldsNode;
    }

    /**
     * Set the custom JSON fields.
     * Must be a valid JsonNode that maps to a JSON object structure, i.e. an {@link ObjectNode}.
     * 
     * @param customFields a {@link JsonNode} whose properties must be added as custom fields.
     * @deprecated use {@link #setCustomFieldsNode(ObjectNode)} instead.
     * @throws IllegalArgumentException if the argument is not a {@link ObjectNode}.
     */
    @Deprecated
    public void setCustomFieldsNode(JsonNode customFields) {
        if (customFields != null && !(customFields instanceof ObjectNode)) {
            throw new IllegalArgumentException("Must be an ObjectNode");
        }
        setCustomFieldsNode((ObjectNode) customFields);
    }
    
    
    /**
     * Use the fields of the given {@link ObjectNode} (may be empty).
     * 
     * @param customFields the JSON object whose fields as added as custom fields
     */
    public void setCustomFieldsNode(ObjectNode customFields) {
        if (isStarted()) {
            throw new IllegalStateException("Configuration cannot be changed while the provider is started");
        }
        
        this.customFieldsNode = customFields;
        this.customFields = null;
    }
    
    
    @Override
    public void setJsonFactory(JsonFactory jsonFactory) {
        this.jsonFactory = Objects.requireNonNull(jsonFactory);
    }
}
