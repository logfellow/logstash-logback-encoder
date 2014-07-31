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

import net.logstash.logback.fieldnames.LogstashFieldNames;
import net.logstash.logback.layout.LogstashLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.net.SyslogAppenderBase;
import ch.qos.logback.core.net.SyslogOutputStream;

public class LogstashSocketAppender extends SyslogAppenderBase<ILoggingEvent> {
    
    private String customFields = "{}";
    private boolean includeCallerInfo;
    private boolean includeContext = true;
    private boolean includeMdc = true;
    private LogstashFieldNames fieldNames;
    
    @Override
    public Layout<ILoggingEvent> buildLayout() {
        LogstashLayout layout = new LogstashLayout();
        layout.setContext(getContext());
        layout.setCustomFields(customFields);
        layout.setIncludeContext(includeContext);
        layout.setIncludeMdc(includeMdc);
        layout.setIncludeCallerInfo(includeCallerInfo);
        if (fieldNames != null) {
            layout.setFieldNames(fieldNames);
        }
        return layout;
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
        this.customFields = customFields;
    }
    
    public String getCustomFields() {
        return this.customFields;
    }
    
    public boolean isIncludeCallerInfo() {
        return this.includeCallerInfo;
    }
    
    public void setIncludeCallerInfo(boolean includeCallerInfo) {
        this.includeCallerInfo = includeCallerInfo;
    }
    
    public LogstashFieldNames getFieldNames() {
        return fieldNames;
    }
    
    public void setFieldNames(LogstashFieldNames fieldNames) {
        this.fieldNames = fieldNames;
    }
    
    public boolean isIncludeMdc() {
        return includeMdc;
    }
    public void setIncludeMdc(boolean includeMdc) {
        this.includeMdc = includeMdc;
    }
    
    public boolean isIncludeContext() {
        return includeContext;
    }
    public void setIncludeContext(boolean includeContext) {
        this.includeContext = includeContext;
    }
    
    @Override
    public SyslogOutputStream createOutputStream() throws UnknownHostException, SocketException {
        return new SyslogOutputStream(this.getHost(), this.getPort());
    }
}
