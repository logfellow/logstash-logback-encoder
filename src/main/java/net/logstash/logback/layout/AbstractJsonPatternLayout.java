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
package net.logstash.logback.layout;

import ch.qos.logback.core.LayoutBase;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SegmentedStringWriter;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import net.logstash.logback.decorate.JsonFactoryDecorator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;
import net.logstash.logback.decorate.NullJsonFactoryDecorator;
import net.logstash.logback.decorate.NullJsonGeneratorDecorator;
import net.logstash.logback.layout.parser.AbstractJsonPatternParser;
import net.logstash.logback.layout.parser.NodeWriter;

import java.io.IOException;
import java.io.Writer;
import java.lang.ref.SoftReference;

/**
 * Special layout (as per http://logback.qos.ch/manual/layouts.html) that transforms an event into a String
 * based on the pattern supplied. Delegates most of the work to the AbstractJsonPatternParser that is to
 * parse the pattern specified.
 * Subclasses must implement <code>createParser</code> method so it returns parser valid for a specified event class.
 *
 * @param <E> - type of the event (ILoggingEvent, IAccessEvent)
 *
 * @author <a href="mailto:dimas@dataart.com">Dmitry Andrianov</a>
 */
public abstract class AbstractJsonPatternLayout<E> extends LayoutBase<E> {

    private MappingJsonFactory jsonFactory;

    private JsonFactoryDecorator jsonFactoryDecorator = new NullJsonFactoryDecorator();
    private JsonGeneratorDecorator jsonGeneratorDecorator = new NullJsonGeneratorDecorator();

    private NodeWriter<E> nodeWriter;

    private String pattern;

    // The buffer recycling bit I copied from LogstashAbstractFormatter so see there for explanation.
    // I do not know if it gives any benefits really.
    private final ThreadLocal<SoftReference<BufferRecycler>> recycler =
            new ThreadLocal<SoftReference<BufferRecycler>>() {
                protected SoftReference<BufferRecycler> initialValue() {
                    final BufferRecycler bufferRecycler = new BufferRecycler();
                    return new SoftReference<BufferRecycler>(bufferRecycler);
                }
            };

    protected abstract AbstractJsonPatternParser<E> createParser();

    public String doLayout(E event) {

        if (nodeWriter == null) {
            // Just do not crash
            return null;
        }

        try {
            SegmentedStringWriter writer = new SegmentedStringWriter(getBufferRecycler());
            JsonGenerator generator = createGenerator(writer);
            nodeWriter.write(generator, event);
            generator.flush();
            return writer.getAndClear();
        } catch (Exception e) {
            addWarn("Error formatting logging event", e);
            return null;
        }
    }

    private BufferRecycler getBufferRecycler() {
        SoftReference<BufferRecycler> bufferRecyclerReference = recycler.get();
        BufferRecycler bufferRecycler = bufferRecyclerReference.get();
        if (bufferRecycler == null) {
            recycler.remove();
            return getBufferRecycler();
        }
        return bufferRecycler;
    }

    private JsonGenerator createGenerator(Writer writer) throws IOException {
        JsonGenerator generator = jsonFactory.createGenerator(writer);
        if (jsonGeneratorDecorator != null) {
            generator = jsonGeneratorDecorator.decorate(generator);
        }
        return generator;
    }


    @Override
    public void start() {
        super.start();

        jsonFactory = new MappingJsonFactory();
        jsonFactory.enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);

        if (jsonFactoryDecorator != null) {
            jsonFactory = jsonFactoryDecorator.decorate(jsonFactory);
        }

        AbstractJsonPatternParser<E> parser = createParser();
        nodeWriter = parser.parse(jsonFactory, pattern);
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(final String pattern) {
        this.pattern = pattern;
    }

    public JsonFactoryDecorator getJsonFactoryDecorator() {
        return jsonFactoryDecorator;
    }

    public void setJsonFactoryDecorator(final JsonFactoryDecorator jsonFactoryDecorator) {
        this.jsonFactoryDecorator = jsonFactoryDecorator;
    }

    public JsonGeneratorDecorator getJsonGeneratorDecorator() {
        return jsonGeneratorDecorator;
    }

    public void setJsonGeneratorDecorator(final JsonGeneratorDecorator jsonGeneratorDecorator) {
        this.jsonGeneratorDecorator = jsonGeneratorDecorator;
    }
}
