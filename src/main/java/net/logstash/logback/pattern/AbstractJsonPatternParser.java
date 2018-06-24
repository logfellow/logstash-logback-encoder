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
package net.logstash.logback.pattern;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.qos.logback.core.pattern.PatternLayoutBase;
import ch.qos.logback.core.spi.ContextAware;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Parser that takes a JSON pattern, resolves all the conversion specifiers and returns an instance
 * of NodeWriter that, when its write() method is invoked, produces JSON defined by the parsed pattern.
 *
 * @param <Event> - type of the event (ILoggingEvent, IAccessEvent)
 *
 * @author <a href="mailto:dimas@dataart.com">Dmitry Andrianov</a>
 */
public abstract class AbstractJsonPatternParser<Event> {

    public static final Pattern OPERATION_PATTERN = Pattern.compile("\\# (\\w+) (?: \\{ (.*) \\} )?", Pattern.COMMENTS);
    
    private final ContextAware contextAware;
    private final JsonFactory jsonFactory;
    
    private final Map<String, Operation> operations = new HashMap<String, Operation>();

    public AbstractJsonPatternParser(final ContextAware contextAware, final JsonFactory jsonFactory) {
        this.contextAware = contextAware;
        this.jsonFactory = jsonFactory;
        addOperation(new AsLongOperation());
        addOperation(new AsDoubleOperation());
        addOperation(new AsJsonOperation());
        addOperation(new TryJsonOperation());
    }
    
    protected void addOperation(Operation operation) {
        this.operations.put(operation.getName(), operation);
    }

    protected abstract class Operation {
        private final String name;
        private final boolean requiresData;

        public Operation(String name, boolean requiresData) {
            this.name = name;
            this.requiresData = requiresData;
        }
        
        public String getName() {
            return name;
        }
        
        public boolean requiresData() {
            return requiresData;
        }
        
        public abstract ValueGetter<?, Event> createValueGetter(String data);
        
    }
    
    protected class AsLongOperation extends Operation {

        public AsLongOperation() {
            super("asLong", true);
        }
        
        @Override
        public ValueGetter<?, Event> createValueGetter(String data) {
            return new AsLongValueTransformer<Event>(makeLayoutValueGetter(data));
        }
    }

    protected class AsDoubleOperation extends Operation {

        public AsDoubleOperation() {
            super("asDouble", true);
        }
        
        @Override
        public ValueGetter<?, Event> createValueGetter(String data) {
            return new AsDoubleValueTransformer<Event>(makeLayoutValueGetter(data));
        }
    }

    protected class AsJsonOperation extends Operation {

        public AsJsonOperation() {
            super("asJson", true);
        }

        @Override
        public ValueGetter<?, Event> createValueGetter(String data) {
            return new AsJsonValueTransformer(makeLayoutValueGetter(data));
        }
    }
    
    protected class TryJsonOperation extends Operation {

        public TryJsonOperation() {
            super("tryJson", true);
        }

        @Override
        public ValueGetter<?, Event> createValueGetter(String data) {
            return new TryJsonValueTransformer(makeLayoutValueGetter(data));
        }
    }

    protected static class LayoutValueGetter<Event> implements ValueGetter<String, Event> {

        private final PatternLayoutBase<Event> layout;

        LayoutValueGetter(final PatternLayoutBase<Event> layout) {
            this.layout = layout;
        }

        @Override
        public String getValue(final Event event) {
            return layout.doLayout(event);
        }
    }

    protected static abstract class AbstractAsObjectTransformer<T, Event> implements ValueGetter<T, Event> {

        private final ValueGetter<String, Event> generator;

        AbstractAsObjectTransformer(final ValueGetter<String, Event> generator) {
            this.generator = generator;
        }

        @Override
        public T getValue(final Event event) {
            final String value = generator.getValue(event);
            if (value == null || value.isEmpty()) {
                return null;
            }
            try {
                return transform(value);
            } catch (Exception e) {
                return null;
            }
        }

        abstract protected T transform(final String value) throws NumberFormatException, IOException;
    }

    protected static abstract class AbstractAsNumberTransformer<T extends Number, Event> implements ValueGetter<T, Event> {

        private final ValueGetter<String, Event> generator;

        AbstractAsNumberTransformer(final ValueGetter<String, Event> generator) {
            this.generator = generator;
        }

        @Override
        public T getValue(final Event event) {
            final String value = generator.getValue(event);
            if (value == null || value.isEmpty()) {
                return null;
            }
            try {
                return transform(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        abstract protected T transform(final String value) throws NumberFormatException;
    }

    protected static class AsLongValueTransformer<Event> extends AbstractAsNumberTransformer<Long, Event> {
        public AsLongValueTransformer(final ValueGetter<String, Event> generator) {
            super(generator);
        }

        protected Long transform(final String value) throws NumberFormatException {
            return Long.parseLong(value);
        }
    }

    protected static class AsDoubleValueTransformer<Event> extends AbstractAsNumberTransformer<Double, Event> {
        public AsDoubleValueTransformer(final ValueGetter<String, Event> generator) {
            super(generator);
        }

        protected Double transform(final String value)  throws NumberFormatException {
            return Double.parseDouble(value);
        }
    }

    protected class AsJsonValueTransformer extends AbstractAsObjectTransformer<JsonNode, Event> {

        public AsJsonValueTransformer(final ValueGetter<String, Event> generator) {
            super(generator);
        }

        protected JsonNode transform(final String value) throws IOException {
            return jsonFactory.getCodec().readTree(jsonFactory.createParser(value));
        }
    }
    
    protected class TryJsonValueTransformer extends AbstractAsObjectTransformer<Object, Event> {

        public TryJsonValueTransformer(final ValueGetter<String, Event> generator) {
            super(generator);
        }

        protected Object transform(final String value) throws IOException {
            try {
                final String trimmedValue = value.trim();
                final JsonParser parser = jsonFactory.createParser(trimmedValue);
                final TreeNode tree = jsonFactory.getCodec().readTree(parser);
                if (parser.getCurrentLocation().getCharOffset() < trimmedValue.length()) {
                    /*
                     * If the full trimmed string was not read, then the full trimmed string contains a json value plus other text.
                     * For example, trimmedValue = '10 foobar', or 'true foobar', or '{"foo","bar"} baz'.
                     * In these cases readTree will only read the first part, and will not read the remaining text.
                     */
                    return value;
                }
                return tree;
            } catch (JsonParseException e) {
                return value;
            }
        }
    }

    protected static interface FieldWriter<Event> extends NodeWriter<Event> {
    }

    protected static class ConstantValueWriter<Event> implements NodeWriter<Event> {
        private final Object value;

        public ConstantValueWriter(final Object value) {
            this.value = value;
        }

        public void write(JsonGenerator generator, Event event) throws IOException {
            generator.writeObject(value);
        }
    }

    protected static class ListWriter<Event> implements NodeWriter<Event> {
        private final List<NodeWriter<Event>> items;

        public ListWriter(final List<NodeWriter<Event>> items) {
            this.items = items;
        }

        public void write(JsonGenerator generator, Event event) throws IOException {
            generator.writeStartArray();
            for (NodeWriter<Event> item : items) {
                item.write(generator, event);
            }
            generator.writeEndArray();
        }
    }

    protected static class ComputableValueWriter<Event> implements NodeWriter<Event> {

        private final ValueGetter<?, Event> getter;

        public ComputableValueWriter(final ValueGetter<?, Event> getter) {
            this.getter = getter;
        }

        public void write(JsonGenerator generator, Event event) throws IOException {
            Object value = getter.getValue(event);
            generator.writeObject(value);
        }
    }

    protected static class DelegatingObjectFieldWriter<Event> implements FieldWriter<Event> {

        private final String name;
        private final NodeWriter<Event> delegate;

        public DelegatingObjectFieldWriter(final String name, final NodeWriter<Event> delegate) {
            this.name = name;
            this.delegate = delegate;
        }

        public void write(JsonGenerator generator, Event event) throws IOException {
            generator.writeFieldName(name);
            delegate.write(generator, event);
        }
    }

    protected static class ComputableObjectFieldWriter<Event> implements FieldWriter<Event> {

        private final String name;
        private final ValueGetter<?, Event> getter;

        public ComputableObjectFieldWriter(final String name, final ValueGetter<?, Event> getter) {
            this.name = name;
            this.getter = getter;
        }

        public void write(JsonGenerator generator, Event event) throws IOException {
            Object value = getter.getValue(event);
            generator.writeFieldName(name);
            generator.writeObject(value);
        }
    }

    protected static class ObjectWriter<Event> implements NodeWriter<Event> {

        private final ChildrenWriter<Event> childrenWriter;
        
        public ObjectWriter(ChildrenWriter<Event> childrenWriter) {
            this.childrenWriter = childrenWriter;
        }

        public void write(JsonGenerator generator, Event event) throws IOException {
            generator.writeStartObject();
            this.childrenWriter.write(generator, event);
            generator.writeEndObject();
        }
    }

    protected static class ChildrenWriter<Event> implements NodeWriter<Event> {

        private final List<FieldWriter<Event>> items;

        public ChildrenWriter(final List<FieldWriter<Event>> items) {
            this.items = items;
        }

        public void write(JsonGenerator generator, Event event) throws IOException {
            for (FieldWriter<Event> item : items) {
                item.write(generator, event);
            }
        }
    }

    protected PatternLayoutBase<Event> buildLayout(String format) {
        PatternLayoutBase<Event> layout = createLayout();
        layout.setContext(contextAware.getContext());
        layout.setPattern(format);
        layout.setPostCompileProcessor(null); // Remove EnsureLineSeparation which is there by default
        layout.start();

        return layout;
    }

    protected abstract PatternLayoutBase<Event> createLayout();

    private ValueGetter<?, Event> makeComputableValueGetter(String pattern) {

        Matcher matcher = OPERATION_PATTERN.matcher(pattern);

        if (matcher.matches()) {
            String operationName = matcher.group(1);
            String operationData = matcher.groupCount() > 1
                    ? matcher.group(2)
                    : null;
                    
            Operation operation = this.operations.get(operationName);
            if (operation != null) {
                if (operation.requiresData() && operationData == null) {
                    contextAware.addError("No parameter provided to operation: " + operation.getName());
                } else {
                    return operation.createValueGetter(operationData);
                }
            }
        } 
        return makeLayoutValueGetter(pattern);
    }

    protected LayoutValueGetter<Event> makeLayoutValueGetter(final String data) {
        return new LayoutValueGetter<Event>(buildLayout(data));
    }

    private NodeWriter<Event> parseValue(JsonNode node) {
        if (node.isTextual()) {
            ValueGetter<?, Event> getter = makeComputableValueGetter(node.asText());
            return new ComputableValueWriter<Event>(getter);
        } else if (node.isArray()) {
            return parseArray(node);
        } else if (node.isObject()) {
            return parseObject(node);
        } else {
            // Anything else, we will be just writing as is (nulls, numbers, booleans and whatnot)
            return new ConstantValueWriter<Event>(node);
        }
    }

    private ListWriter<Event> parseArray(JsonNode node) {

        List<NodeWriter<Event>> children = new ArrayList<NodeWriter<Event>>();
        for (JsonNode item : node) {
            children.add(parseValue(item));
        }

        return new ListWriter<Event>(children);
    }

    private ObjectWriter<Event> parseObject(JsonNode node) {

        return new ObjectWriter<Event>(parseChildren(node));
    }

    private ChildrenWriter<Event> parseChildren(JsonNode node) {
        List<FieldWriter<Event>> children = new ArrayList<FieldWriter<Event>>();
        for (Iterator<Map.Entry<String, JsonNode>> nodeFields = node.fields(); nodeFields.hasNext(); ) {
            Map.Entry<String, JsonNode> field = nodeFields.next();

            String key = field.getKey();
            JsonNode value = field.getValue();

            if (value.isTextual()) {
                ValueGetter<?, Event> getter = makeComputableValueGetter(value.asText());
                children.add(new ComputableObjectFieldWriter<Event>(key, getter));
            } else {
                children.add(new DelegatingObjectFieldWriter<Event>(key, parseValue(value)));
            }
        }
        return new ChildrenWriter<Event>(children);
    }

    public NodeWriter<Event> parse(String pattern) {

        if (pattern == null) {
            contextAware.addError("No pattern specified");
            return null;
        }

        JsonNode node;
        try {
            node = jsonFactory.createParser(pattern).readValueAsTree();
        } catch (IOException e) {
            contextAware.addError("Failed to parse pattern [" + pattern + "]", e);
            return null;
        }

        if (node == null) {
            contextAware.addError("Empty JSON pattern");
            return null;
        }

        if (!node.isObject()) {
            contextAware.addError("Invalid pattern JSON - must be an object");
            return null;
        }

        return parseChildren(node);
    }
}
