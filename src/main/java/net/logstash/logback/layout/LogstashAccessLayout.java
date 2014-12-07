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

import java.io.IOException;

import net.logstash.logback.LogstashAccessFormatter;
import net.logstash.logback.decorate.JsonFactoryDecorator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;
import net.logstash.logback.fieldnames.LogstashAccessFieldNames;
import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.LayoutBase;

public class LogstashAccessLayout extends LayoutBase<IAccessEvent> {
    
    private final LogstashAccessFormatter formatter = new LogstashAccessFormatter(this);
    
    public String doLayout(IAccessEvent event) {
        try {
            return formatter.writeValueAsString(event, getContext());
        } catch (IOException e) {
            addWarn("Error formatting logging event", e);
            return null;
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
    
    public LogstashAccessFieldNames getFieldNames() {
        return formatter.getFieldNames();
    }
    
    public void setFieldNames(LogstashAccessFieldNames fieldNames) {
        formatter.setFieldNames(fieldNames);
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

}
