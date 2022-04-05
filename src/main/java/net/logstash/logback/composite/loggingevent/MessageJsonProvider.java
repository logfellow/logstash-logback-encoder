/*
 * Copyright 2013-2022 the original author or authors.
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
package net.logstash.logback.composite.loggingevent;

import java.io.IOException;
import java.util.regex.Pattern;

import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.encoder.SeparatorParser;
import net.logstash.logback.fieldnames.LogstashFieldNames;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;

public class MessageJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> implements FieldNamesAware<LogstashFieldNames> {
    
    public static final String FIELD_MESSAGE = "message";

    private Pattern messageSplitPattern = null;

    public MessageJsonProvider() {
        setFieldName(FIELD_MESSAGE);
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        if (messageSplitPattern != null) {
            String[] multiLineMessage = messageSplitPattern.split(event.getFormattedMessage());
            JsonWritingUtils.writeStringArrayField(generator, getFieldName(), multiLineMessage);
        } else {
            JsonWritingUtils.writeStringField(generator, getFieldName(), event.getFormattedMessage());
        }
    }
    
    @Override
    public void setFieldNames(LogstashFieldNames fieldNames) {
        setFieldName(fieldNames.getMessage());
    }

    /**
     * Write the message as a JSON array by splitting the message text using the specified regex.
     *
     * @return The regex used to split the message text
     */
    public String getMessageSplitRegex() {
        return messageSplitPattern != null ? messageSplitPattern.pattern() : null;
    }

    /**
     * Write the message as a JSON array by splitting the message text using the specified regex.
     *
     * <p>The allowed values are:
     * <ul>
     *     <li>Null/Empty : Disable message splitting. This is also the default behavior.</li>
     *     <li>Any valid regex : Use the specified regex.</li>
     *     <li>{@code SYSTEM} : Use the system-default line separator.</li>
     *     <li>{@code UNIX} : Use {@code \n}.</li>
     *     <li>{@code WINDOWS} : Use {@code \r\n}.</li>
     * </ul>
     * 
     * For example, if this parameter is set to the regex {@code #+}, then the logging statement:
     * 
     * <pre>
     * log.info("First line##Second line###Third line")
     * </pre>
     * 
     * will produce:
     * <pre>
     * {
     *     ...
     *     "message": [
     *         "First line",
     *         "Second line",
     *         "Third line"
     *     ],
     *     ...
     * }
     * </pre>
     *
     * @param messageSplitRegex The regex used to split the message text
     */
    public void setMessageSplitRegex(String messageSplitRegex) {
        String parsedMessageSplitRegex = SeparatorParser.parseSeparator(messageSplitRegex);
        this.messageSplitPattern = parsedMessageSplitRegex != null ? Pattern.compile(parsedMessageSplitRegex) : null;
    }
}
