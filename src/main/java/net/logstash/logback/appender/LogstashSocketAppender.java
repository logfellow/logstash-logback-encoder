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
package net.logstash.logback.appender;

import java.net.SocketException;
import java.net.UnknownHostException;

import net.logstash.logback.decorate.JsonFactoryDecorator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;
import net.logstash.logback.fieldnames.LogstashFieldNames;
import net.logstash.logback.layout.LogstashLayout;
import net.logstash.logback.stacktrace.StackTraceFormatter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.net.SyslogAppenderBase;
import ch.qos.logback.core.net.SyslogOutputStream;

public class LogstashSocketAppender extends SyslogAppenderBase<ILoggingEvent> {
    
    private final LogstashLayout logstashLayout = new LogstashLayout();
    
    @Override
    public Layout<ILoggingEvent> buildLayout() {
        logstashLayout.setContext(getContext());
        return logstashLayout;
    }
    
    @Override
    public void start() {
        super.start();
        getLayout().start();
    }
    
    @Override
    public void stop() {
        super.stop();
        getLayout().stop();
    }
    
    public LogstashSocketAppender() {
        setFacility("NEWS"); // NOTE: this value is never used
    }
    
    @Override
    public int getSeverityForEvent(Object eventObject) {
        return 0; // NOTE: this value is never used
    }
    
    public String getHost() {
        return getSyslogHost();
    }
    
    /**
     * Just an alias for syslogHost (since that name doesn't make much sense here)
     * 
     * @param host
     */
    public void setHost(String host) {
        setSyslogHost(host);
    }
    
    public void setCustomFields(String customFields) {
        logstashLayout.setCustomFields(customFields);
    }
    
    public String getCustomFields() {
        return logstashLayout.getCustomFields().toString();
    }
    
    public boolean isIncludeCallerInfo() {
        return logstashLayout.isIncludeCallerInfo();
    }
    
    public void setIncludeCallerInfo(boolean includeCallerInfo) {
        logstashLayout.setIncludeCallerInfo(includeCallerInfo);
    }
    
    public LogstashFieldNames getFieldNames() {
        return logstashLayout.getFieldNames();
    }
    
    public void setFieldNames(LogstashFieldNames fieldNames) {
        logstashLayout.setFieldNames(fieldNames);
    }
    
    public boolean isIncludeMdc() {
        return logstashLayout.isIncludeMdc();
    }
    
    public void setIncludeMdc(boolean includeMdc) {
        logstashLayout.setIncludeMdc(includeMdc);
    }
    
    public boolean isIncludeContext() {
        return logstashLayout.isIncludeContext();
    }
    
    public void setIncludeContext(boolean includeContext) {
        logstashLayout.setIncludeContext(includeContext);
    }

    public int getShortenedLoggerNameLength() {
        return logstashLayout.getShortenedLoggerNameLength();
    }

    public void setShortenedLoggerNameLength(int length) {
        logstashLayout.setShortenedLoggerNameLength(length);
    }
    
    public JsonFactoryDecorator getJsonFactoryDecorator() {
        return logstashLayout.getJsonFactoryDecorator();
    }

    public void setJsonFactoryDecorator(JsonFactoryDecorator jsonFactoryDecorator) {
        logstashLayout.setJsonFactoryDecorator(jsonFactoryDecorator);
    }

    public JsonGeneratorDecorator getJsonGeneratorDecorator() {
        return logstashLayout.getJsonGeneratorDecorator();
    }

    public void setJsonGeneratorDecorator(JsonGeneratorDecorator jsonGeneratorDecorator) {
        logstashLayout.setJsonGeneratorDecorator(jsonGeneratorDecorator);
    }

    public StackTraceFormatter getStackTraceFormatter() {
        return logstashLayout.getStackTraceFormatter();
    }

    public void setStackTraceFormatter(StackTraceFormatter stackTraceFormatter) {
        logstashLayout.setStackTraceFormatter(stackTraceFormatter);
    }

    @Override
    public SyslogOutputStream createOutputStream() throws UnknownHostException, SocketException {
        return new SyslogOutputStream(this.getHost(), this.getPort());
    }
}
