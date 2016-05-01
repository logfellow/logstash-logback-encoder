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

import net.logstash.logback.LogstashAccessFormatter;
import net.logstash.logback.composite.CompositeJsonFormatter;
import net.logstash.logback.composite.JsonProvider;
import net.logstash.logback.fieldnames.LogstashAccessFieldNames;
import ch.qos.logback.access.spi.IAccessEvent;

public class LogstashAccessLayout extends AccessEventCompositeJsonLayout {
    
    @Override
    protected CompositeJsonFormatter<IAccessEvent> createFormatter() {
        return new LogstashAccessFormatter(this);
    }
    
    @Override
    protected LogstashAccessFormatter getFormatter() {
        return (LogstashAccessFormatter) super.getFormatter();
    }
    
    public void addProvider(JsonProvider<IAccessEvent> provider) {
        getFormatter().addProvider(provider);
    }
    
    public LogstashAccessFieldNames getFieldNames() {
        return getFormatter().getFieldNames();
    }
    
    public void setFieldNames(LogstashAccessFieldNames fieldNames) {
        getFormatter().setFieldNames(fieldNames);
    }

    public String getTimeZone() {
        return getFormatter().getTimeZone();
    }

    public void setTimeZone(String timeZoneId) {
        getFormatter().setTimeZone(timeZoneId);
    }

    public void setCustomFields(String customFields) {
        getFormatter().setCustomFieldsFromString(customFields);
    }
    
    public String getCustomFields() {
        return getFormatter().getCustomFieldsAsString();
    }
    
    public boolean getLowerCaseHeaderNames() {
        return getFormatter().getLowerCaseHeaderNames();
    }

    /**
     * When true, names of headers will be written to JSON output in lowercase. 
     */
    public void setLowerCaseHeaderNames(boolean lowerCaseHeaderNames) {
        getFormatter().setLowerCaseHeaderNames(lowerCaseHeaderNames);
    }


}
