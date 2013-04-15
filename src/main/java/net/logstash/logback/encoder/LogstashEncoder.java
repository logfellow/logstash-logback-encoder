/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package net.logstash.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.encoder.EncoderBase;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Map.Entry;
import static org.apache.commons.io.IOUtils.*;

/**
 * Log encoder for logstash to produce json log event via layouter.
 */
public class LogstashEncoder extends EncoderBase<ILoggingEvent> {

    private static final ObjectMapper MAPPER = new ObjectMapper().configure(Feature.ESCAPE_NON_ASCII, true);
    private boolean immediateFlush = true;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

    @Override
    public void doEncode(ILoggingEvent event) throws IOException {

        ObjectNode eventNode = MAPPER.createObjectNode();
        eventNode.put("@timestamp", DATE_FORMAT.format(event.getTimeStamp()));
        eventNode.put("@message", event.getFormattedMessage());
        eventNode.put("@fields", createFields(event));

        write(MAPPER.writeValueAsBytes(eventNode), outputStream);
        write(CoreConstants.LINE_SEPARATOR, outputStream);

        if (immediateFlush) {
            outputStream.flush();
        }

    }

    private ObjectNode createFields(ILoggingEvent event) {

        ObjectNode fieldsNode = MAPPER.createObjectNode();
        fieldsNode.put("logger_name", event.getLoggerName());
        fieldsNode.put("thread_name", event.getThreadName());
        fieldsNode.put("level", event.getLevel().toString());
        fieldsNode.put("level_value", event.getLevel().toInt());

        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            fieldsNode.put("stack_trace", ThrowableProxyUtil.asString(throwableProxy));
        }

        Map<String, String> mdc = event.getMDCPropertyMap();

        if (mdc != null) {
            for (Entry<String, String> entry : mdc.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                fieldsNode.put(key, value);
            }
        }

        return fieldsNode;

    }

    @Override
    public void close() throws IOException {
        write(LINE_SEPARATOR, outputStream);
    }

    /**
     * If true the outputstream will flushed immediatly.
     *
     * @return true if stream will be flushed immediatly
     */
    public boolean isImmediateFlush() {
        return immediateFlush;
    }

    /**
     * Set to true to flush outputstream immediatly.
     *
     * @param immediateFlush true or false
     */
    public void setImmediateFlush(boolean immediateFlush) {
        this.immediateFlush = immediateFlush;
    }
}
