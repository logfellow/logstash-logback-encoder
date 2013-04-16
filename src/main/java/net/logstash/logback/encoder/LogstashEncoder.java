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
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Map.Entry;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObjectBuilder;

/**
 * Log encoder for logstash to produce json log event via layouter.
 */
public class LogstashEncoder extends EncoderBase<ILoggingEvent> {

    private static final JsonBuilderFactory BUILDER = Json.createBuilderFactory(null);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
    private boolean immediateFlush = true;
    private static String hostname;

    static {
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown-host";
        }
    }

    @Override
    public void doEncode(ILoggingEvent event) throws IOException {

        JsonObjectBuilder builder = BUILDER.createObjectBuilder();
        builder.add("@timestamp", DATE_FORMAT.format(event.getTimeStamp()));
        builder.add("@message", event.getFormattedMessage());
        builder.add("@source", event.getLoggerName());
        builder.add("@source_host", hostname);
        builder.add("@fields", createFields(event));

        outputStream.write(builder.build().toString().getBytes());
        outputStream.write(CoreConstants.LINE_SEPARATOR.getBytes());

        if (immediateFlush) {
            outputStream.flush();
        }

    }

    private JsonObjectBuilder createFields(ILoggingEvent event) {

        JsonObjectBuilder builder = BUILDER.createObjectBuilder();
        builder.add("logger_name", event.getLoggerName());
        builder.add("thread_name", event.getThreadName());
        builder.add("level", event.getLevel().toString());
        builder.add("level_value", event.getLevel().toInt());

        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            builder.add("stack_trace", ThrowableProxyUtil.asString(throwableProxy));
        }

        Map<String, String> mdc = event.getMDCPropertyMap();

        if (mdc != null) {
            for (Entry<String, String> entry : mdc.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                builder.add(key, value);
            }
        }

        return builder;

    }

    @Override
    public void close() throws IOException {
        outputStream.write(CoreConstants.LINE_SEPARATOR.getBytes());
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
