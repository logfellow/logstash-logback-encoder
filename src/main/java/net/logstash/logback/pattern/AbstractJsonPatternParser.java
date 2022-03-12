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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.logstash.logback.composite.JsonReadingUtils;
import net.logstash.logback.util.StringUtils;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonPointer;
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
     * An operation starts with a #, followed by a name and a pair of {} with possible arguments in between.
     */
    public static final Pattern OPERATION_PATTERN = Pattern.compile("\\# (\\w+) (?: \\{ (.*) \\} )", Pattern.COMMENTS);

    private final Context context;
    private final JsonFactory jsonFactory;

    private final Map<String, Operation<?>> operations = new HashMap<>();


    /**
     * When true, fields whose values are considered empty
     * will be omitted from JSON output.
     */
    private boolean omitEmptyFields;

    AbstractJsonPatternParser(final Context context, final JsonFactory jsonFactory) {
        this.context = Objects.requireNonNull(context);
        this.jsonFactory = Objects.requireNonNull(jsonFactory);
        addOperation("asLong", new AsLongOperation());
        addOperation("asDouble", new AsDoubleOperation());
        addOperation("asBoolean", new AsBooleanOperation());
        addOperation("asJson", new AsJsonOperation());
        addOperation("tryJson", new TryJsonOperation());
        addOperation("asNullIfEmpty", new AsNullIfEmptyOperation());
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

    protected interface Operation<T> extends Function<String, T> {
    }

    protected static class AsLongOperation implements Operation<Long> {
        @Override
        public Long apply(String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Failed to convert '" + value + "' into a Long numeric value");
            }
        }
    }

    protected static class AsDoubleOperation implements Operation<Double> {
        @Override
        public Double apply(String value) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Failed to convert '" + value + "' into a Double numeric value");
            }
        }
    }

    protected static class AsBooleanOperation implements Operation<Boolean> {
        @Override
        public Boolean apply(String value) {
            if (StringUtils.isEmpty(value)) {
                return null;
            }
            return Boolean.valueOf(
                       "true".equalsIgnoreCase(value)
                    || "1".equals(value)
                    || "yes".equalsIgnoreCase(value)
                    || "y".equalsIgnoreCase(value));
        }
    }
    
    protected class AsJsonOperation implements Operation<JsonNode> {
        @Override
        public JsonNode apply(final String value) {
            try {
                return JsonReadingUtils.readFully(jsonFactory, value);
            } catch (JsonParseException e) {
                throw new IllegalArgumentException("Failed to convert '" + value + "' into a JSON object", e);
            } catch (IOException e) {
                throw new IllegalStateException("Unexpected IOException when reading JSON value (was '" + value + "')", e);
            }
        }
    }

    protected class TryJsonOperation implements Operation<Object> {
        @Override
        public Object apply(final String value) {
            try {
                return JsonReadingUtils.readFully(jsonFactory, value);
            } catch (JsonParseException e) {
                return value;
            } catch (IOException e) {
                throw new IllegalStateException("Unexpected IOException when reading JSON value (was '" + value + "')", e);
            }
        }
    }
    
    protected class AsNullIfEmptyOperation implements Operation<Object> {
        @Override
        public Object apply(String t) {
            return StringUtils.isEmpty(t) ? null : t;
        }
    }

    
    private ValueGetter<Event, ?> makeComputableValueGetter(String pattern) {
        Matcher matcher = OPERATION_PATTERN.matcher(pattern);

        if (matcher.matches()) {
            String operationName = matcher.group(1);
            String operationData = matcher.groupCount() > 1
                    ? matcher.group(2)
                    : null;

            Operation<?> operation = this.operations.get(operationName);
            if (operation == null) {
                throw new IllegalArgumentException("Unknown operation '#" + operationName + "{}'");
            }

            final ValueGetter<Event, String> layoutValueGetter = makeLayoutValueGetter(operationData);
            return event -> operation.apply(layoutValueGetter.getValue(event));
            
        } else {
            // Unescape pattern if needed
            if (pattern != null && pattern.startsWith("\\#")) {
                pattern = pattern.substring(1);
            }
            return makeLayoutValueGetter(pattern);
        }
    }

    protected ValueGetter<Event, String> makeLayoutValueGetter(final String data) {
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
    
    
    /**
     * Initialize a PatternLayout with the supplied format and throw an {@link IllegalArgumentException}
     * if the format is invalid.
     * 
     * @param format the pattern layout format
     * @return a configured and started {@link PatternLayoutAdapter} instance around the supplied format
     * @throws IllegalArgumentException if the supplied format is not a valid PatternLayout
     */
    protected PatternLayoutAdapter<Event> buildLayout(String format) {
        PatternLayoutAdapter<Event> adapter = new PatternLayoutAdapter<>(createLayout());
        adapter.setPattern(format);
        adapter.setContext(context);
        adapter.start();
        
        return adapter;
    }

    
    /**
     * Create a PatternLayout instance of the appropriate type. The returned instance
     * will be further configured with the context and appropriate pattern then started.
     * 
     * @return an unstarted {@link PatternLayoutBase} instance
     */
    protected abstract PatternLayoutBase<Event> createLayout();
    
    
    protected static class LayoutValueGetter<Event> implements ValueGetter<Event, String> {
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
        private static final int MAX_RECYCLABLE_SIZE = 1024;
        
        
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
     * @throws JsonPatternException denotes an invalid pattern
     */
    public NodeWriter<Event> parse(String pattern) throws JsonPatternException {
        if (StringUtils.isEmpty(pattern)) {
            return null;
        }

        final ObjectNode node;
        try (JsonParser jsonParser = jsonFactory.createParser(pattern)) {
            node = JsonReadingUtils.readFullyAsObjectNode(jsonFactory, pattern);
        } catch (IOException e) {
            throw new JsonPatternException("pattern is not a valid JSON object", e);
        }

        NodeWriter<Event> nodeWriter = new RootWriter<>(parseObject(JsonPointer.compile("/"), node));
        if (omitEmptyFields) {
            nodeWriter = new OmitEmptyFieldWriter<>(nodeWriter);
        }
        return nodeWriter;
    }

    /**
     * Parse a {@link JsonNode} and produce the corresponding {@link NodeWriter}.
     * 
     * @param node the {@link JsonNode} to parse.
     * @return a {@link NodeWriter} corresponding to the given JSON node
     * @throws JsonPatternException denotes an invalid pattern
     */
    private NodeWriter<Event> parseNode(JsonPointer location, JsonNode node) throws JsonPatternException {
        if (node.isTextual()) {
            try {
                ValueGetter<Event, ?> getter = makeComputableValueGetter(node.asText());
                return new ValueWriter<>(getter);
            } catch (RuntimeException e) {
                String msg = "Invalid JSON property '" + location + "' (was '" + node.asText() + "'): " + e.getMessage();
                throw new JsonPatternException(msg, e);
            }
        }
        if (node.isArray()) {
            return parseArray(location, (ArrayNode) node);
        }
        if (node.isObject()) {
            return parseObject(location, (ObjectNode) node);
        }

        // Anything else, we will be just writing as is (nulls, numbers, booleans and whatnot)
        return new ValueWriter<>(g -> node);
    }
    
    
    /**
     * Parse a JSON array.
     * 
     * @param node the {@link ArrayNode} to parse
     * @return a {@link ArrayWriter}
     * @throws JsonPatternException denotes an invalid pattern
     */
    private ArrayWriter<Event> parseArray(JsonPointer location, ArrayNode node) throws JsonPatternException {
        List<NodeWriter<Event>> children = new ArrayList<>();
        
        int index = 0;
        for (JsonNode item : node) {
            children.add(parseNode(appendPath(location, Integer.toString(index++)), item));
        }

        return new ArrayWriter<>(children);
    }
    
    
    /**
     * Parse an JSON object node
     * 
     * @param node the {@link ObjectNode} to parse
     * @return a {@link ObjectWriter}
     * @throws JsonPatternException denotes an invalid pattern
     */
    private ObjectWriter<Event> parseObject(JsonPointer location, ObjectNode node) throws JsonPatternException {
        ObjectWriter<Event> writer = new ObjectWriter<>();

        for (Iterator<Map.Entry<String, JsonNode>> nodeFields = node.fields(); nodeFields.hasNext();) {
            Map.Entry<String, JsonNode> field = nodeFields.next();

            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();

            NodeWriter<Event> fieldWriter = parseNode(appendPath(location, fieldName), fieldValue);
            writer.addField(fieldName, fieldWriter);
        }
        
        return writer;
    }
    
    
    /**
     * Append a path to an existing {@link JsonPointer}
     * 
     * @param ptr the pointer to add the path
     * @param path the path to add
     * @return a new {@link JsonPointer}
     */
    private static JsonPointer appendPath(JsonPointer ptr, String path) {
        return ptr.append(JsonPointer.compile("/" + path));
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
        private final ValueGetter<Event, ?> getter;

        ValueWriter(final ValueGetter<Event, ?> getter) {
            this.getter = getter;
        }

        public void write(JsonGenerator generator, Event event) throws IOException {
            generator.writeObject(getValue(event));
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
    
    
    @SuppressWarnings("serial")
    public static class JsonPatternException extends Exception {
        public JsonPatternException(String message, Throwable cause) {
            super(message, cause);
        }

        public JsonPatternException(String message) {
            super(message);
        }
    }
}
