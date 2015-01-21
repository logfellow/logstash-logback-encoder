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
package net.logstash.logback.composite.loggingevent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.MDC;

import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.fieldnames.LogstashFieldNames;
import ch.qos.logback.classic.spi.ILoggingEvent;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Includes {@link MDC} properties in the JSON output according to
 * {@link #includeMdcKeyNames} and {@link #excludeMdcKeyNames}.
 * <p>
 * There are three valid combinations of {@link #includeMdcKeyNames}
 * and {@link #excludeMdcKeyNames}:
 * 
 * <ol>
 * <li>When {@link #includeMdcKeyNames} and {@link #excludeMdcKeyNames}
 *     are both empty, then all entries will be included.</li>
 * <li>When {@link #includeMdcKeyNames} is not empty and
 *     {@link #excludeMdcKeyNames} is empty, then only those entries
 *     with key names in {@link #includeMdcKeyNames} will be included.</li> 
 * <li>When {@link #includeMdcKeyNames} is empty and
 *     {@link #excludeMdcKeyNames} is not empty, then all entries except those
 *     with key names in {@link #excludeMdcKeyNames} will be included.</li>
 * </ol>
 * 
 *  It is a configuration error for both {@link #includeMdcKeyNames}
 *  and {@link #excludeMdcKeyNames} to be not empty.
 */
public class MdcJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> implements FieldNamesAware<LogstashFieldNames> {

    /**
     * See {@link #includeMdc}.
     */
    private List<String> includeMdcKeyNames = new ArrayList<String>();
    
    /**
     * See {@link #includeMdc}.
     */
    private List<String> excludeMdcKeyNames = new ArrayList<String>();
    
    @Override
    public void start() {
        if (!this.includeMdcKeyNames.isEmpty() && !this.excludeMdcKeyNames.isEmpty()) {
            addError("Both includeMdcKeyNames and excludeMdcKeyNames are not empty.  Only one is allowed to be not empty.");
        }
        super.start();
    }
    
    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        Map<String, String> mdcProperties = event.getMDCPropertyMap();
        if (mdcProperties != null && !mdcProperties.isEmpty()) {
            if (getFieldName() != null) {
                generator.writeObjectFieldStart(getFieldName());
            }
            if (!includeMdcKeyNames.isEmpty()) {
                mdcProperties = new HashMap<String, String>(mdcProperties);
                mdcProperties.keySet().retainAll(includeMdcKeyNames);
            }
            if (!excludeMdcKeyNames.isEmpty()) {
                mdcProperties = new HashMap<String, String>(mdcProperties);
                mdcProperties.keySet().removeAll(excludeMdcKeyNames);
            }
            JsonWritingUtils.writeMapEntries(generator, mdcProperties);
            if (getFieldName() != null) {
                generator.writeEndObject();
            }
        }
    }
    
    @Override
    public void setFieldNames(LogstashFieldNames fieldNames) {
        setFieldName(fieldNames.getMdc());
    }

    public List<String> getIncludeMdcKeyNames() {
        return Collections.unmodifiableList(includeMdcKeyNames);
    }
    public void addIncludeMdcKeyName(String includedMdcKeyName) {
        this.includeMdcKeyNames.add(includedMdcKeyName);
    }
    public void setIncludeMdcKeyNames(List<String> includeMdcKeyNames) {
        this.includeMdcKeyNames = new ArrayList<String>(includeMdcKeyNames);
    }
    
    public List<String> getExcludeMdcKeyNames() {
        return Collections.unmodifiableList(excludeMdcKeyNames);
    }
    public void addExcludeMdcKeyName(String excludedMdcKeyName) {
        this.excludeMdcKeyNames.add(excludedMdcKeyName);
    }
    public void setExcludeMdcKeyNames(List<String> excludeMdcKeyNames) {
        this.excludeMdcKeyNames = new ArrayList<String>(excludeMdcKeyNames);
    }
    
}
