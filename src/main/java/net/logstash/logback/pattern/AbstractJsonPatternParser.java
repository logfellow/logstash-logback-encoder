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
package net.logstash.logback.pattern;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.logstash.logback.composite.JsonReadingUtils;
import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.util.StringUtils;

import ch.qos.logback.core.pattern.PatternLayoutBase;
import ch.qos.logback.core.spi.ContextAware;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.filter.FilteringGeneratorDelegate;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.core.filter.TokenFilter.Inclusion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Parser that takes a JSON pattern, resolves all the conversion specifiers and returns an instance
 * of NodeWriter that, when its write() method is invoked, produces JSON defined by the parsed pattern.
 *
 * @param <Event> - type of the event (ILoggingEvent, IAccessEvent)
 *
 * @author <a href="mailto:dimas@dataart.com">Dmitry Andrianov</a>
 */
public abstract class AbstractJsonPatternParser<Event> {

    /**
     * Pattern used to parse and detect {@link AbstractJsonPatternParser.Operation} in a string.
     */
    public static final Pattern OPERATION_PATTERN = Pattern.compile("\\# (\\w+) (?: \\{ (.*) \\} )?", Pattern.COMMENTS);

    private final ContextAware contextAware;
    private final JsonFactory jsonFactory;

    private final Map<String, Operation<?>> operations = new HashMap<>();


    /**
     * When true, fields whose values are considered empty
     * will be omitted from JSON output.
     */
    private boolean omitEmptyFields;

    public AbstractJsonPatternParser(final ContextAware contextAware, final JsonFactory jsonFactory) {
        this.contextAware = Objects.requireNonNull(contextAware);
        this.jsonFactory = Objects.requireNonNull(jsonFactory);
        addOperation("asLong", new AsLongOperation());
        addOperation("asDouble", new AsDoubleOperation());
        addOperation("asJson", new AsJsonOperation());
        addOperation("tryJson", new TryJsonOperation());
    }

    /**
     * Register a new {@link Operation} and bind it to the given {@code name}.
     * 
     * @param name the name of the operation
     * @param operation the {@link Operation} instance
     */
    protected void addOperation(String name, Operation<?> operation) {
        this.operations.put(name, operation);
    }

    protected abstract class Operation<T> {
        private final boolean requiresData;

        Operation(boolean requiresData) {
            this.requiresData = requiresData;
        }

        public boolean requiresData() {
            return requiresData;
        }

        public abstract ValueGetter<T, Event> createValueGetter(String data);
    }

    protected class AsLongOperation extends Operation<Long> {
        public AsLongOperation() {
            super(true);
        }

        @Override
        public ValueGetter<Long, Event> createValueGetter(String data) {
            return makeLayoutValueGetter(data).andThen(Long::parseLong);
        }
    }

    protected class AsDoubleOperation extends Operation<Double> {
        public AsDoubleOperation() {
            super(true);
        }

        @Override
        public ValueGetter<Double, Event> createValueGetter(String data) {
            return makeLayoutValueGetter(data).andThen(Double::parseDouble);
        }
    }

    protected class AsJsonOperation extends Operation<JsonNode> {
        public AsJsonOperation() {
            super(true);
        }

        @Override
        public ValueGetter<JsonNode, Event> createValueGetter(String data) {
            return makeLayoutValueGetter(data).andThen(this::convert);
        }
        
        private JsonNode convert(final String value) {
            try {
                return JsonReadingUtils.readFully(jsonFactory, value);
            } catch (IOException e) {
                throw new IllegalStateException("Unexpected IOException when reading JSON value (was '" + value + "')", e);
            }
        }
    }

    protected class TryJsonOperation extends Operation<Object> {
        public TryJsonOperation() {
            super(true);
        }

        @Override
        public ValueGetter<Object, Event> createValueGetter(String data) {
            return makeLayoutValueGetter(data).andThen(this::convert);
        }
        
        private Object convert(final String value) {
            final String trimmedValue = StringUtils.trimToEmpty(value);
            
            try (JsonParser parser = jsonFactory.createParser(trimmedValue)) {
                final TreeNode tree = parser.readValueAsTree();
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
            } catch (IOException e) {
                throw new IllegalStateException("Unexpected IOException when reading JSON value (was '" + value + "')", e);
            }
        }
    }

    
    private ValueGetter<?, Event> makeComputableValueGetter(String pattern) {
        Matcher matcher = OPERATION_PATTERN.matcher(pattern);

        if (matcher.matches()) {
            String operationName = matcher.group(1);
            String operationData = matcher.groupCount() > 1
                    ? matcher.group(2)
                    : null;

            Operation<?> operation = this.operations.get(operationName);
            if (operation != null) {
                if (operation.requiresData() && operationData == null) {
                    contextAware.addError("No parameter provided to operation: " + operationName);
                } else {
                    return operation.createValueGetter(operationData);
                }
            }
        }
        return makeLayoutValueGetter(pattern);
    }

    protected ValueGetter<String, Event> makeLayoutValueGetter(final String data) {
        /*
         * PatternLayout emits an ERROR status when pattern is null or empty and
         * defaults to an empty string. Better to handle it here to avoid the error
         * status.
         */
        if (StringUtils.isEmpty(data)) {
            return g -> "";
        }
        
        PatternLayoutAdapter<Event> layout = buildLayout(data);
        
        /*
         * If layout is constant, get the constant value immediately to avoid rendering it into
         * a StringBuilder at runtime every time an event is serialized
         */
        if (layout.isConstant()) {
            final String constantValue = layout.getConstantValue();
            return g -> constantValue;
        } else {
            return new LayoutValueGetter<>(layout);
        }
    }
    
    
    protected PatternLayoutAdapter<Event> buildLayout(String format) {
        PatternLayoutBase<Event> layout = createLayout();
        
        if (layout.isStarted()) {
            throw new IllegalStateException("PatternLayout should not be started");
        }
        
        layout.setContext(contextAware.getContext());
        layout.setPattern(format);
        layout.setPostCompileProcessor(null); // Remove EnsureLineSeparation which is there by default
       
        return new PatternLayoutAdapter<>(layout);
    }

    
    /**
     * Create a PatternLayout instance of the appropriate type. The returned instance
     * will further configured with the context and appropriate pattern then started.
     * 
     * @return an unstarted {@link PatternLayoutBase} instance
     */
    protected abstract PatternLayoutBase<Event> createLayout();
    
    
    protected static class LayoutValueGetter<Event> implements ValueGetter<String, Event> {
        /**
         * The PatternLayout from which the value is generated
         */
        private final PatternLayoutAdapter<Event> layout;

        /**
         * ThreadLocal reusable StringBuilder instances
         */
        private static final ThreadLocal<StringBuilder> STRING_BUILDERS = ThreadLocal.withInitial(StringBuilder::new);
        
        /**
         * StringBuilder whose length after use exceeds the maxRecylableSize will be
         * discarded instead of recycled.
         */
        private static final int MAX_RECYCLABLE_SIZE = 2048;
        
        
        LayoutValueGetter(final PatternLayoutAdapter<Event> layout) {
            this.layout = layout;
        }

        @Override
        public String getValue(final Event event) {
            StringBuilder strBuilder = STRING_BUILDERS.get();
            try {
                layout.writeTo(strBuilder, event);
                return strBuilder.toString();
                
            } finally {
                if (strBuilder.length() <= MAX_RECYCLABLE_SIZE) {
                    strBuilder.setLength(0);
                } else {
                    STRING_BUILDERS.remove();
                }
            }
        }
    }
    

    /**
     * Parse a JSON pattern and produce the corresponding {@link NodeWriter}.
     * Returns <em>null</em> if the pattern is invalid, null or empty. An error status is
     * logged when the pattern is invalid and parsing failed.
     * 
     * @param pattern the JSON pattern to parse
     * @return a {@link NodeWriter} configured according to the pattern
     */
    public NodeWriter<Event> parse(String pattern) {
        if (StringUtils.isEmpty(pattern)) {
            return null;
        }

        final ObjectNode node;
        try (JsonParser jsonParser = jsonFactory.createParser(pattern)) {
            node = JsonReadingUtils.readFullyAsObjectNode(jsonFactory, pattern);
        } catch (IOException e) {
            contextAware.addError("[pattern] is not a valid JSON object", e);
            return null;
        }

        NodeWriter<Event> nodeWriter = new RootWriter<>(parseObject(node));
        if (omitEmptyFields) {
            nodeWriter = new OmitEmptyFieldWriter<>(nodeWriter);
        }
        return nodeWriter;
    }

    /**
     * Parse a {@link JsonNode} and produce the corresponding {@link NodeWriter}.
     * 
     * @param node the {@link JsonNode} to parse.
     * @return a {@link NodeWriter} corresponding to the given json node
     */
    private NodeWriter<Event> parseNode(JsonNode node) {
        if (node.isTextual()) {
            ValueGetter<?, Event> getter = makeComputableValueGetter(node.asText());
            return new ValueWriter<>(getter);
        }
        if (node.isArray()) {
            return parseArray((ArrayNode) node);
        }
        if (node.isObject()) {
            return parseObject((ObjectNode) node);
        }
        // Anything else, we will be just writing as is (nulls, numbers, booleans and whatnot)
        return new ValueWriter<>(g -> node);
    }
    
    
    /**
     * Parse a JSON array.
     * 
     * @param node the {@link ArrayNode} to parse
     * @return a {@link ArrayWriter}
     */
    private ArrayWriter<Event> parseArray(ArrayNode node) {
        List<NodeWriter<Event>> children = new ArrayList<>();
        for (JsonNode item : node) {
            children.add(parseNode(item));
        }

        return new ArrayWriter<>(children);
    }
    
    
    /**
     * Parse an OBJECT json node
     * 
     * @param node the {@link ObjectNode} to parse
     * @return a {@link ObjectWriter}
     */
    private ObjectWriter<Event> parseObject(ObjectNode node) {
        ObjectWriter<Event> writer = new ObjectWriter<>();

        for (Iterator<Map.Entry<String, JsonNode>> nodeFields = node.fields(); nodeFields.hasNext();) {
            Map.Entry<String, JsonNode> field = nodeFields.next();

            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();

            NodeWriter<Event> fieldWriter = parseNode(fieldValue);
            writer.addField(fieldName, fieldWriter);
        }
        
        return writer;
    }
    
    
    //
    // -- NodeWriters -----------------------------------------------------------------------------
    //
    
    protected static class ObjectWriter<Event> implements NodeWriter<Event> {
        private final List<Field<Event>> fields = new ArrayList<>();
        
        public void addField(String fieldName, NodeWriter<Event> fieldValue) {
            this.fields.add(new Field<>(fieldName, fieldValue));
        }
        
        @Override
        public void write(JsonGenerator generator, Event event) throws IOException {
            generator.writeStartObject();
            writeFields(generator, event);
            generator.writeEndObject();
        }
        
        protected void writeFields(JsonGenerator generator, Event event) throws IOException {
            for (Field<Event> field: this.fields) {
                field.write(generator, event);
            }
        }
        
        private static class Field<E> {
            private final String name;
            private final NodeWriter<E> writer;
            
            Field(String name, NodeWriter<E> writer) {
                this.name = name;
                this.writer = writer;
            }
            
            public void write(JsonGenerator generator, E event) throws IOException {
                generator.writeFieldName(name);
                writer.write(generator, event);
            }
        }
    }
    
    
    protected static class ArrayWriter<Event> implements NodeWriter<Event> {
        private final List<NodeWriter<Event>> items;

        ArrayWriter(final List<NodeWriter<Event>> items) {
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
    
    
    protected static class ValueWriter<Event> implements NodeWriter<Event> {
        private final ValueGetter<?, Event> getter;

        ValueWriter(final ValueGetter<?, Event> getter) {
            this.getter = getter;
        }

        public void write(JsonGenerator generator, Event event) throws IOException {
            JsonWritingUtils.writeSimpleObject(generator, getValue(event));
        }
        
        private Object getValue(Event event) {
            try {
                return this.getter.getValue(event);
            } catch (RuntimeException e) {
                return null;
            }
        }
    }
    
    
    private static class RootWriter<Event> implements NodeWriter<Event> {
        private final ObjectWriter<Event> delegate;
        
        RootWriter(ObjectWriter<Event> delegate) {
            this.delegate = Objects.requireNonNull(delegate);
        }
        
        @Override
        public void write(JsonGenerator generator, Event event) throws IOException {
            delegate.writeFields(generator, event);
        }
    }
    
    
    private static class OmitEmptyFieldWriter<Event> implements NodeWriter<Event> {
        private static final ThreadLocal<ReusableFilteringGenerator> filteringGenerators = ThreadLocal.withInitial(ReusableFilteringGenerator::new);
        private final NodeWriter<Event> delegate;

        OmitEmptyFieldWriter(NodeWriter<Event> delegate) {
            this.delegate = Objects.requireNonNull(delegate);
        }
        
        @Override
        public void write(JsonGenerator generator, Event event) throws IOException {
            ReusableFilteringGenerator filteringGenerator = filteringGenerators.get();
            try {
                filteringGenerator.connect(generator);
                delegate.write(filteringGenerator, event);
                
            } catch (RuntimeException | IOException e) {
                filteringGenerators.remove();
                throw e;
                
            } finally {
                filteringGenerator.disconnect();
            }
        }
    }
    
    
    private static class ReusableFilteringGenerator extends FilteringGeneratorDelegate {
        ReusableFilteringGenerator() {
            super(null, NullExcludingTokenFilter.INSTANCE, Inclusion.INCLUDE_ALL_AND_PATH, true /* multiple matches */);
        }
        
        public void connect(JsonGenerator generator) {
            this.delegate = generator;
        }
        
        public void disconnect() {
            this.delegate = null;
        }
    }
    
    
    private static class NullExcludingTokenFilter extends TokenFilter {
        private static final NullExcludingTokenFilter INSTANCE = new NullExcludingTokenFilter();

        @Override
        public boolean includeNull() {
            return false;
        }
        
        @Override
        public boolean includeString(String value) {
            return !StringUtils.isEmpty(value);
        }
    }
    
    
    //
    // -- Public API ------------------------------------------------------------------------------
    //
    
    /**
     * When {@code true}, fields whose values are considered empty will be omitted from JSON output.
     * 
     * @return {@code true} if fields with empty values are omitted from JSON output
     */
    public boolean isOmitEmptyFields() {
        return omitEmptyFields;
    }

    /**
     * When {@code true}, fields whose values are considered empty will be omitted from JSON output.
     * 
     * @param omitEmptyFields whether fields with empty value should be omitted or not
     */
    public void setOmitEmptyFields(boolean omitEmptyFields) {
        this.omitEmptyFields = omitEmptyFields;
    }
}
