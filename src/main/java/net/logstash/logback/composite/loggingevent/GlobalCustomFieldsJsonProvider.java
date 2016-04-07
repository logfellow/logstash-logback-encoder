/**
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
package net.logstash.logback.composite.loggingevent;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import net.logstash.logback.composite.AbstractJsonProvider;
import net.logstash.logback.composite.JsonFactoryAware;
import ch.qos.logback.core.spi.DeferredProcessingAware;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;

public class GlobalCustomFieldsJsonProvider<Event extends DeferredProcessingAware> extends AbstractJsonProvider<Event> implements JsonFactoryAware {
    
    /**
     * The un-parsed custom fields string to use to initialize customFields
     * when the formatter is started.
     */
    private String customFields;

    /**
     * When non-null, the fields in this JsonNode will be embedded in the logstash json.
     */
    private JsonNode customFieldsNode;
    
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
    
    @Override
    public void start() {
        initializeCustomFields();
        super.start();
    }
    
    private void initializeCustomFields() {
        if (this.customFields != null && jsonFactory != null) {
            try {
                this.customFieldsNode = this.jsonFactory
                    .createParser(customFields).readValueAsTree();
            } catch (IOException e) {
                addError("Failed to parse custom fields [" + customFields + "]", e);
            }
        }
    }

    public void setCustomFields(String customFields) {
        this.customFields = customFields;
        if (isStarted()) {
            initializeCustomFields();
        }
    }
    
    public String getCustomFields() {
        return customFields;
    }
    
    public JsonNode getCustomFieldsNode() {
        return this.customFieldsNode;
    }
    
    public void setCustomFieldsNode(JsonNode customFields) {
        this.customFieldsNode = customFields;
        if (this.customFieldsNode != null && customFields == null) {
            this.customFields = this.customFieldsNode.toString();
        }
    }
    
    @Override
    public void setJsonFactory(JsonFactory jsonFactory) {
        this.jsonFactory = jsonFactory;
    }
}
