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

import net.logstash.logback.pattern.AbstractJsonPatternParser;
import net.logstash.logback.pattern.NodeWriter;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Transforms an string containing patterns understood by PatternLayouts into JSON output.
 * Delegates most of the work to the {@link AbstractJsonPatternParser} that is to
 * parse the pattern specified.
 * Subclasses must implement {@link #createParser()} method so it returns parser valid for a specified event class.
 *
 * @param <Event> type of event ({@link ILoggingEvent} or {@link IAccessEvent}).
 *
 * @author <a href="mailto:dimas@dataart.com">Dmitry Andrianov</a>
 */
public abstract class AbstractPatternJsonProvider<Event extends DeferredProcessingAware>
        extends AbstractJsonProvider<Event> implements JsonFactoryAware {

    private NodeWriter<Event> nodeWriter;
    
    private String pattern;

    protected JsonFactory jsonFactory;

    /**
     * When true, fields whose values are considered empty ({@link AbstractJsonPatternParser#isEmptyValue(Object)}})
     * will be omitted from json output.
     */
    private boolean omitEmptyFields;

    @Override
    public void writeTo(JsonGenerator generator, Event event) throws IOException {
        if (nodeWriter != null) {
            nodeWriter.write(generator, event);
        }
    }
    
    protected abstract AbstractJsonPatternParser<Event> createParser();
    
    public String getPattern() {
        return pattern;
    }

    public void setPattern(final String pattern) {
        this.pattern = pattern;
        parse();
    }
    
    @Override
    public void setJsonFactory(JsonFactory jsonFactory) {
        this.jsonFactory = jsonFactory;
        parse();
    }

    /**
     * Parses the pattern into a {@link NodeWriter}.
     * We do this when the properties are set instead of on {@link #start()},
     * because {@link #start()} is called by logstash's xml parser
     * before the Formatter has had an opportunity to set the jsonFactory.
     */
    private void parse() {
        if (pattern != null && jsonFactory != null) {
            AbstractJsonPatternParser<Event> parser = createParser();
            parser.setOmitEmptyFields(omitEmptyFields);
            nodeWriter = parser.parse(pattern);
        }
    }

    /**
     * When true, fields whose values are considered empty ({@link AbstractJsonPatternParser#isEmptyValue(Object)}})
     * will be omitted from json output.
     */
    public boolean isOmitEmptyFields() {
        return omitEmptyFields;
    }

    /**
     * When true, fields whose values are considered empty ({@link AbstractJsonPatternParser#isEmptyValue(Object)}})
     * will be omitted from json output.
     */
    public void setOmitEmptyFields(boolean omitEmptyFields) {
        this.omitEmptyFields = omitEmptyFields;
    }


}
