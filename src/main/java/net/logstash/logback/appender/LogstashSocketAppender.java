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

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import net.logstash.logback.appender.listener.AppenderListener;
import net.logstash.logback.composite.JsonProvider;
import net.logstash.logback.decorate.JsonFactoryDecorator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;
import net.logstash.logback.fieldnames.LogstashFieldNames;
import net.logstash.logback.layout.LogstashLayout;
import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.net.SyslogAppenderBase;
import ch.qos.logback.core.net.SyslogOutputStream;

public class LogstashSocketAppender extends SyslogAppenderBase<ILoggingEvent> {
    
    private final LogstashLayout logstashLayout = new LogstashLayout();
    
    /**
     * These listeners will be notified when certain events occur on this appender.
     */
    private final List<AppenderListener<ILoggingEvent>> listeners = new ArrayList<>();
    
    private SyslogOutputStream syslogOutputStream;  

    public LogstashSocketAppender() {
        setFacility("NEWS"); // NOTE: this value is never used
    }
    
    @Override
    public Layout<ILoggingEvent> buildLayout() {
        logstashLayout.setContext(getContext());
        return logstashLayout;
    }
    
    @Override
    public void start() {
        super.start();
        getLayout().start();
        fireAppenderStarted();
    }
    
    @Override
    public void stop() {
        super.stop();
        getLayout().stop();
        fireAppenderStopped();
    }
    
    @Override
    protected void append(ILoggingEvent eventObject) {
        if (!isStarted()) {
            return;
        }
        long startTime = System.nanoTime();
        try {
            /*
             * This is a cut-and-paste of SyslogAppenderBase.append(E)
             * so that we can fire the appropriate event.
             */
            String msg = getLayout().doLayout(eventObject);
            if (msg == null) {
                return;
            }
            if (msg.length() > getMaxMessageSize()) {
                msg = msg.substring(0, getMaxMessageSize());
            }
            syslogOutputStream.write(msg.getBytes(getCharset()));
            syslogOutputStream.flush();
            postProcess(eventObject, syslogOutputStream);
            
            fireEventAppended(eventObject, System.nanoTime() - startTime);
        } catch (IOException ioe) {
            addError("Failed to send diagram to " + getSyslogHost(), ioe);

            fireEventAppendFailed(eventObject, ioe);
        }
    }
    
    protected void fireAppenderStarted() {
        for (AppenderListener<ILoggingEvent> listener : listeners) {
            listener.appenderStarted(this);
        }
    }
    
    protected void fireAppenderStopped() {
        for (AppenderListener<ILoggingEvent> listener : listeners) {
            listener.appenderStopped(this);
        }
    }
    
    protected void fireEventAppended(ILoggingEvent event, long durationInNanos) {
        for (AppenderListener<ILoggingEvent> listener : listeners) {
            listener.eventAppended(this, event, durationInNanos);
        }
    }
    
    protected void fireEventAppendFailed(ILoggingEvent event, Throwable reason) {
        for (AppenderListener<ILoggingEvent> listener : listeners) {
            listener.eventAppendFailed(this, event, reason);
        }
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
    
    public void addProvider(JsonProvider<ILoggingEvent> provider) {
        logstashLayout.addProvider(provider);
    }
    
    public void setCustomFields(String customFields) {
        logstashLayout.setCustomFields(customFields);
    }
    
    public String getCustomFields() {
        return logstashLayout.getCustomFields().toString();
    }
    
    public boolean isIncludeCallerData() {
        return logstashLayout.isIncludeCallerData();
    }
    
    public void setIncludeCallerData(boolean includeCallerData) {
        logstashLayout.setIncludeCallerData(includeCallerData);
    }
    
    /**
     * @deprecated use {@link #isIncludeCallerData()} (to use the same name that logback uses) 
     */
    @Deprecated
    public boolean isIncludeCallerInfo() {
        return logstashLayout.isIncludeCallerInfo();
    }
    
    /**
     * @deprecated use {@link #setIncludeCallerData(boolean)} (to use the same name that logback uses)
     */
    @Deprecated
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
    
    public List<String> getIncludeMdcKeyNames() {
        return logstashLayout.getIncludeMdcKeyNames();
    }

    public void addIncludeMdcKeyName(String includedMdcKeyName) {
        logstashLayout.addIncludeMdcKeyName(includedMdcKeyName);
    }

    public void setIncludeMdcKeyNames(List<String> includeMdcKeyNames) {
        logstashLayout.setIncludeMdcKeyNames(includeMdcKeyNames);
    }

    public List<String> getExcludeMdcKeyNames() {
        return logstashLayout.getExcludeMdcKeyNames();
    }

    public void addExcludeMdcKeyName(String excludedMdcKeyName) {
        logstashLayout.addExcludeMdcKeyName(excludedMdcKeyName);
    }

    public void setExcludeMdcKeyNames(List<String> excludeMdcKeyNames) {
        logstashLayout.setExcludeMdcKeyNames(excludeMdcKeyNames);
    }
    
    public boolean isIncludeContext() {
        return logstashLayout.isIncludeContext();
    }
    
    public void setIncludeContext(boolean includeContext) {
        logstashLayout.setIncludeContext(includeContext);
    }

    public boolean isIncludeStructuredArguments() {
        return logstashLayout.isIncludeStructuredArguments();
    }

    public void setIncludeStructuredArguments(boolean includeStructuredArguments) {
        logstashLayout.setIncludeStructuredArguments(includeStructuredArguments);
    }
    
    public boolean isIncludeNonStructuredArguments() {
        return logstashLayout.isIncludeNonStructuredArguments();
    }

    public void setIncludeNonStructuredArguments(boolean includeNonStructuredArguments) {
        logstashLayout.setIncludeNonStructuredArguments(includeNonStructuredArguments);
    }
    
    public String getNonStructuredArgumentsFieldPrefix() {
        return logstashLayout.getNonStructuredArgumentsFieldPrefix();
    }

    public void setNonStructuredArgumentsFieldPrefix(String nonStructuredArgumentsFieldPrefix) {
        logstashLayout.setNonStructuredArgumentsFieldPrefix(nonStructuredArgumentsFieldPrefix);
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

    public void setFindAndRegisterJacksonModules(boolean findAndRegisterJacksonModules) {
        logstashLayout.setFindAndRegisterJacksonModules(findAndRegisterJacksonModules);
    }

    public String getTimeZone() {
        return logstashLayout.getTimeZone();
    }

    public void setTimeZone(String timeZoneId) {
        logstashLayout.setTimeZone(timeZoneId);
    }

    public ThrowableHandlingConverter getThrowableConverter() {
        return logstashLayout.getThrowableConverter();
    }

    public void setThrowableConverter(ThrowableHandlingConverter throwableConverter) {
        logstashLayout.setThrowableConverter(throwableConverter);
    }
    
    public Layout<ILoggingEvent> getPrefix() {
        return logstashLayout.getPrefix();
    }
    public void setPrefix(Layout<ILoggingEvent> prefix) {
        logstashLayout.setPrefix(prefix);
    }

    public Layout<ILoggingEvent> getSuffix() {
        return logstashLayout.getSuffix();
    }
    public void setSuffix(Layout<ILoggingEvent> suffix) {
        logstashLayout.setSuffix(suffix);
    }

    public void addListener(AppenderListener<ILoggingEvent> listener) {
        this.listeners.add(listener);
    }
    public void removeListener(AppenderListener<ILoggingEvent> listener) {
        this.listeners.remove(listener);
    }

    @Override
    public SyslogOutputStream createOutputStream() throws UnknownHostException, SocketException {
        this.syslogOutputStream = new SyslogOutputStream(this.getHost(), this.getPort());
        return this.syslogOutputStream;
    }
}
