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
package net.logstash.logback.composite;

import java.io.IOException;
import java.util.TimeZone;

import net.logstash.logback.fieldnames.LogstashCommonFieldNames;

import org.apache.commons.lang.time.FastDateFormat;

import ch.qos.logback.core.spi.DeferredProcessingAware;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Writes a formatted timestamp field.
 */
public abstract class FormattedTimestampJsonProvider<Event extends DeferredProcessingAware, FieldNames extends LogstashCommonFieldNames> extends AbstractFieldJsonProvider<Event> implements FieldNamesAware<FieldNames> {
    
    public static final String FIELD_TIMESTAMP = "@timestamp";
    
    private static final String DEFAULT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";
    private static final TimeZone DEFAULT_TIMEZONE = null;
    
    private String pattern = DEFAULT_PATTERN;
    private TimeZone timeZone = DEFAULT_TIMEZONE;

    private FastDateFormat formatter;
    
    public FormattedTimestampJsonProvider() {
        setFieldName(FIELD_TIMESTAMP);
    }
    
    @Override
    public void setFieldNames(FieldNames fieldNames) {
        setFieldName(fieldNames.getTimestamp());
    }
    
    @Override
    public void writeTo(JsonGenerator generator, Event event) throws IOException {
        JsonWritingUtils.writeStringField(generator, getFieldName(), getFormattedTimestamp(event));
    }

    protected String getFormattedTimestamp(Event event) {
        return formatter.format(getTimestampAsMillis(event));
    }

    protected abstract long getTimestampAsMillis(Event event);
    
    @Override
    public void start() {
        formatter = FastDateFormat.getInstance(pattern, timeZone);
        super.start();
    }
    
    public String getPattern() {
        return pattern;
    }
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
    public String getTimeZone() {
        return timeZone.getID();
    }
    public void setTimeZone(String timeZoneId) {
        this.timeZone = TimeZone.getTimeZone(timeZoneId);
    }
}
