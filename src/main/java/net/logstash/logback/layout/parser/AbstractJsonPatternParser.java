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
package net.logstash.logback.layout.parser;

import ch.qos.logback.core.pattern.PatternLayoutBase;
import ch.qos.logback.core.spi.ContextAware;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser that takes a JSON pattern, resolves all the conversion specifiers and returns an instance
 * of NodeWriter that, when its write() method is invoked, produces JSON defined by the parsed pattern.
 *
 * @param <E> - type of the event (ILoggingEvent, IAccessEvent)
 *
 * @author <a href="mailto:dimas@dataart.com">Dmitry Andrianov</a>
 */
public abstract class AbstractJsonPatternParser<E> {

    public static final Pattern OPERATION_PATTERN = Pattern.compile("\\# (\\w+) (?: \\{ (.*) \\} )?", Pattern.COMMENTS);

    private static final String TIMESTAMP_FIELD = "@timestamp";
    private static final String VERSION_FIELD = "@version";

    private ContextAware contextAware;

    public AbstractJsonPatternParser(final ContextAware contextAware) {
        this.contextAware = contextAware;
    }

    static class LayoutValueGetter<E> implements ValueGetter<String, E> {

        private final PatternLayoutBase<E> layout;

        LayoutValueGetter(final PatternLayoutBase<E> layout) {this.layout = layout;}

        @Override
        public String getValue(final E event) {
            return layout.doLayout(event);
        }
    }

    static abstract class AbstractAsNumberTransformer<T extends Number, E> implements ValueGetter<T, E> {

        private final ValueGetter<String, E> generator;

        AbstractAsNumberTransformer(final ValueGetter<String, E> generator) {
            this.generator = generator;
        }

        @Override
        public T getValue(final E event) {
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

    static class AsLongValueTransformer<E> extends AbstractAsNumberTransformer<Long, E> {
        public AsLongValueTransformer(final ValueGetter<String, E> generator) {
            super(generator);
        }

        protected Long transform(final String value) throws NumberFormatException {
            return Long.parseLong(value);
        }
    }

    static class AsDoubleValueTransformer<E> extends AbstractAsNumberTransformer<Double, E> {
        public AsDoubleValueTransformer(final ValueGetter<String, E> generator) {
            super(generator);
        }

        protected Double transform(final String value)  throws NumberFormatException {
            return Double.parseDouble(value);
        }
    }


    static interface FieldWriter<E> extends NodeWriter<E> {
    }

    public static class ConstantValueWriter<E> implements NodeWriter<E> {
        private Object value;

        public ConstantValueWriter(final Object value) {
            this.value = value;
        }

        public void write(JsonGenerator generator, E event) throws IOException {
            generator.writeObject(value);
        }
    }

    static class ListWriter<E> implements NodeWriter<E> {
        private List<NodeWriter<E>> items;

        public ListWriter(final List<NodeWriter<E>> items) {
            this.items = items;
        }

        public void write(JsonGenerator generator, E event) throws IOException {
            generator.writeStartArray();
            for (NodeWriter<E> item : items) {
                item.write(generator, event);
            }
            generator.writeEndArray();
        }
    }

    static class ComputableValueWriter<E> implements NodeWriter<E> {

        private ValueGetter<?, E> getter;

        public ComputableValueWriter(final ValueGetter<?, E> getter) {
            this.getter = getter;
        }

        public void write(JsonGenerator generator, E event) throws IOException {
            Object value = getter.getValue(event);
            generator.writeObject(value);
        }
    }

    static class DelegatingObjectFieldWriter<E> implements FieldWriter<E> {

        private String name;
        private NodeWriter<E> delegate;

        public DelegatingObjectFieldWriter(final String name, final NodeWriter<E> delegate) {
            this.name = name;
            this.delegate = delegate;
        }

        public void write(JsonGenerator generator, E event) throws IOException {
            generator.writeFieldName(name);
            delegate.write(generator, event);
        }
    }

    static class ComputableObjectFieldWriter<E> implements FieldWriter<E> {

        private String name;
        private ValueGetter<?, E> getter;

        public ComputableObjectFieldWriter(final String name,
                                           final ValueGetter<?, E> getter) {
            this.name = name;
            this.getter = getter;
        }

        public void write(JsonGenerator generator, E event) throws IOException {
            Object value = getter.getValue(event);
            generator.writeFieldName(name);
            generator.writeObject(value);
        }
    }

    static class ObjectWriter<E> implements NodeWriter<E> {

        private List<FieldWriter<E>> items;

        public ObjectWriter(final List<FieldWriter<E>> items) {
            this.items = items;
        }

        public void write(JsonGenerator generator, E event) throws IOException {
            generator.writeStartObject();
            for (FieldWriter<E> item : items) {
                item.write(generator, event);
            }
            generator.writeEndObject();
        }
    }

    protected PatternLayoutBase<E> buildLayout(String format) {
        PatternLayoutBase<E> layout = createLayout();
        layout.setContext(contextAware.getContext());
        layout.setPattern(format);
        layout.setPostCompileProcessor(null); // Remove EnsureLineSeparation which is there by default
        layout.start();

        return layout;
    }

    protected abstract PatternLayoutBase<E> createLayout();

    protected ValueGetter<?, E> operation(String operation, String data) {
        if ("asLong".equals(operation) && data != null)  {
            return new AsLongValueTransformer<E>(makeLayoutValueGetter(data));
        } else if ("asDouble".equals(operation) && data != null) {
            return new AsDoubleValueTransformer<E>(makeLayoutValueGetter(data));
        } else {
            return null;
        }
    }

    protected LayoutValueGetter<E> makeLayoutValueGetter(final String data) {
        return new LayoutValueGetter<E>(buildLayout(data));
    }

    private ValueGetter<?, E> makeComputableValueGetter(String pattern) {

        Matcher matcher = OPERATION_PATTERN.matcher(pattern);
        if (matcher.matches()) {
            // It is an operation
            String name = matcher.group(1);
            String data = matcher.groupCount() > 1 ? matcher.group(2) : null;

            ValueGetter<?, E> result = operation(name, data);
            if (result != null) {
                return result;
            }
        }

        return makeLayoutValueGetter(pattern);
    }

    private NodeWriter<E> parseValue(JsonNode node) {
        if (node.isTextual()) {
            ValueGetter<?, E> getter = makeComputableValueGetter(node.asText());
            return new ComputableValueWriter<E>(getter);
        } else if (node.isArray()) {
            return parseArray(node);
        } else if (node.isObject()) {
            return parseObject(node);
        } else {
            // Anything else, we will be just writing as is (nulls, numbers, booleans and whatnot)
            return new ConstantValueWriter<E>(node);
        }
    }

    private ListWriter<E> parseArray(JsonNode node) {

        List<NodeWriter<E>> children = new ArrayList<NodeWriter<E>>();
        for (JsonNode item : node) {
            children.add(parseValue(item));
        }

        return new ListWriter<E>(children);
    }

    private ObjectWriter<E> parseObject(JsonNode node) {

        List<FieldWriter<E>> children = new ArrayList<FieldWriter<E>>();
        for (Iterator<Map.Entry<String, JsonNode>> nodeFields = node.fields(); nodeFields.hasNext(); ) {
            Map.Entry<String, JsonNode> field = nodeFields.next();

            String key = field.getKey();
            JsonNode value = field.getValue();

            if (value.isTextual()) {
                ValueGetter<?, E> getter = makeComputableValueGetter(value.asText());
                children.add(new ComputableObjectFieldWriter<E>(key, getter));
            } else {
                children.add(new DelegatingObjectFieldWriter<E>(key, parseValue(value)));
            }
        }

        return new ObjectWriter<E>(children);
    }

    private ObjectWriter<E> parseRootObject(JsonNode node) {
        // This is copy&paste of parseObject with addition of default fields.
        // Sorry, did not want to introduce another abstraction layer here
        boolean seenVersion = false;
        boolean seenTimestamp = false;
        List<FieldWriter<E>> children = new ArrayList<FieldWriter<E>>();
        for (Iterator<Map.Entry<String, JsonNode>> nodeFields = node.fields(); nodeFields.hasNext(); ) {
            Map.Entry<String, JsonNode> field = nodeFields.next();

            String key = field.getKey();
            JsonNode value = field.getValue();

            if (value.isTextual()) {
                ValueGetter<?, E> getter = makeComputableValueGetter(value.asText());
                children.add(new ComputableObjectFieldWriter<E>(key, getter));
            } else {
                children.add(new DelegatingObjectFieldWriter<E>(key, parseValue(value)));
            }

            seenVersion |= VERSION_FIELD.equals(key);
            seenTimestamp |= TIMESTAMP_FIELD.equals(key);

        }

        if (!seenVersion) {
            children.add(0, new DelegatingObjectFieldWriter<E>(VERSION_FIELD, new ConstantValueWriter<E>(1)));
        }

        if (!seenTimestamp) {
            children.add(0, new ComputableObjectFieldWriter<E>(TIMESTAMP_FIELD,
                    makeLayoutValueGetter("%date{ISO8601}")));
        }

        return new ObjectWriter<E>(children);
    }

    public NodeWriter<E> parse(JsonFactory jsonFactory, String pattern) {

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

        return parseRootObject(node);
    }
}
