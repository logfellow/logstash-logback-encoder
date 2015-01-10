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
package net.logstash.logback.encoder;

import java.io.IOException;
import java.util.Map;

import net.logstash.logback.LogstashFormatter;
import net.logstash.logback.decorate.JsonFactoryDecorator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;
import net.logstash.logback.fieldnames.LogstashFieldNames;
import net.logstash.logback.marker.Markers;
import net.logstash.logback.stacktrace.StackTraceFormatter;

import org.apache.commons.io.IOUtils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.encoder.EncoderBase;

public class LogstashEncoder extends EncoderBase<ILoggingEvent> {
    
    private boolean immediateFlush = true;
    
    private final LogstashFormatter formatter = new LogstashFormatter(this);
    
    @Override
    public void doEncode(ILoggingEvent event) throws IOException {
        
        formatter.writeValueToOutputStream(event, context, outputStream);
        IOUtils.write(CoreConstants.LINE_SEPARATOR, outputStream);
        
        if (immediateFlush) {
            outputStream.flush();
        }
        
    }
    
    @Override
    public void start() {
        super.start();
        formatter.start();
    }
    
    @Override
    public void stop() {
        super.stop();
        formatter.stop();
    }
    
    @Override
    public void close() throws IOException {
        IOUtils.write(CoreConstants.LINE_SEPARATOR, outputStream);
    }
    
    public boolean isImmediateFlush() {
        return immediateFlush;
    }
    
    public void setImmediateFlush(boolean immediateFlush) {
        this.immediateFlush = immediateFlush;
    }
    
    public boolean isIncludeCallerInfo() {
        return formatter.isIncludeCallerInfo();
    }
    
    public void setIncludeCallerInfo(boolean includeCallerInfo) {
        formatter.setIncludeCallerInfo(includeCallerInfo);
    }
    
    public void setCustomFields(String customFields) {
        formatter.setCustomFieldsFromString(customFields);
    }
    
    public String getCustomFields() {
        return formatter.getCustomFields().toString();
    }
    
    public LogstashFieldNames getFieldNames() {
        return formatter.getFieldNames();
    }
    
    public void setFieldNames(LogstashFieldNames fieldNames) {
        formatter.setFieldNames(fieldNames);
    }

    public int getShortenedLoggerNameLength() {
        return formatter.getShortenedLoggerNameLength();
    }
    
    public void setShortenedLoggerNameLength(int length) {
        formatter.setShortenedLoggerNameLength(length);
    }
    
    public boolean isIncludeMdc() {
        return formatter.isIncludeMdc();
    }
    
    public void setIncludeMdc(boolean includeMdc) {
        formatter.setIncludeMdc(includeMdc);
    }
    
    public boolean isIncludeContext() {
        return formatter.isIncludeContext();
    }
    
    public void setIncludeContext(boolean includeContext) {
        formatter.setIncludeContext(includeContext);
    }
    
    /**
     * <p>
     * If set to true the encoder will search logging event array and if the last item is a Map, entries will be included in the message.
     * </p>
     * <p>
     * Example:
     * 
     * <pre>
     * log.info(&quot;Service started in {} seconds&quot;, duration / 1000, Collections.singletonMap(&quot;duration&quot;, duration))
     * </pre>
     * 
     * Will produce:
     * 
     * <pre>
     * {
     *     "@timestamp": "2014-06-04T15:26:14.464+02:00",
     *     "message": "Service started in 8 seconds",
     *     "level": "INFO",
     *     "duration": 8496
     *     ...
     * </pre>
     * 
     * </p>
     * 
     * @param enableContextMap <code>true</code> to enable context map
     * @deprecated When logging, prefer using a {@link Markers#appendEntries(Map)} marker instead.
     */
    @Deprecated
    public void setEnableContextMap(boolean enableContextMap) {
        formatter.setEnableContextMap(enableContextMap);
    }
    
    /**
     * @deprecated When logging, prefer using a {@link Markers#appendEntries(Map)} marker instead.
     */
    @Deprecated
    public boolean isEnableContextMap() {
        return formatter.isEnableContextMap();
    }
    
    public JsonFactoryDecorator getJsonFactoryDecorator() {
        return formatter.getJsonFactoryDecorator();
    }

    public void setJsonFactoryDecorator(JsonFactoryDecorator jsonFactoryDecorator) {
        formatter.setJsonFactoryDecorator(jsonFactoryDecorator);
    }

    public JsonGeneratorDecorator getJsonGeneratorDecorator() {
        return formatter.getJsonGeneratorDecorator();
    }

    public void setJsonGeneratorDecorator(JsonGeneratorDecorator jsonGeneratorDecorator) {
        formatter.setJsonGeneratorDecorator(jsonGeneratorDecorator);
    }

    public StackTraceFormatter getStackTraceFormatter() {
        return formatter.getStackTraceFormatter();
    }

    public void setStackTraceFormatter(StackTraceFormatter stackTraceFormatter) {
        formatter.setStackTraceFormatter(stackTraceFormatter);
    }

    protected LogstashFormatter getFormatter() {
        return formatter;
    }
}
