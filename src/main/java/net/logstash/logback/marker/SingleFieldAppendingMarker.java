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
package net.logstash.logback.marker;

import java.io.IOException;

import net.logstash.logback.argument.StructuredArgument;
import net.logstash.logback.argument.StructuredArguments;
import net.logstash.logback.composite.loggingevent.ArgumentsJsonProvider;
import net.logstash.logback.composite.loggingevent.LogstashMarkersJsonProvider;

import org.apache.commons.lang.Validate;
import org.slf4j.Marker;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * A {@link Marker} OR {@link StructuredArgument} that appends
 * a single field into the JSON event.
 * <p>
 * 
 * When writing to the JSON data (via {@link ArgumentsJsonProvider} or {@link LogstashMarkersJsonProvider}):
 * <ul>
 * <li>Field names are written via {@link #writeFieldName(JsonGenerator)},
 *     which just uses {@link #fieldName} is used as the field name</li>
 * <li>Values are written via {@link #writeFieldValue(JsonGenerator)},
 *     which subclasses must override</li>
 * </ul>
 * <p>
 * 
 * When writing to a String (when used as a {@link StructuredArgument} to the event's formatted message),
 * the {@link #messageFormatPattern} is used to construct the string output.
 * {@link #getFieldName()} will be substituted in {0} in the {@link #messageFormatPattern}. 
 * {@link #getFieldValue()} will be substituted in {1} in the {@link #messageFormatPattern}.
 * Subclasses must override {@link #getFieldValue()} to provide the field value to include.  
 */
@SuppressWarnings("serial")
public abstract class SingleFieldAppendingMarker extends LogstashMarker implements StructuredArgument{
    
    public static final String MARKER_NAME_PREFIX = LogstashMarker.MARKER_NAME_PREFIX + "APPEND_";
    
    /**
     * Name of the field to append.
     * 
     * Note that the value of the field is provided by subclasses via {@link #writeFieldValue(JsonGenerator)}.
     */
    private final String fieldName;

    /**
     * Pattern to use when appending the field/value in {@link #toString()}.
     * <p>
     * {@link #getFieldName()} will be substituted in {0}. 
     * {@link #getFieldValue()} will be substituted in {1}. 
     */
    private final String messageFormatPattern;
    
    public SingleFieldAppendingMarker(String markerName, String fieldName) {
        this(markerName, fieldName, StructuredArguments.DEFAULT_KEY_VALUE_MESSAGE_FORMAT_PATTERN);
    }
    
    public SingleFieldAppendingMarker(String markerName, String fieldName, String messageFormatPattern) {
        super(markerName);
        Validate.notNull(fieldName, "fieldName must not be null");
        Validate.notNull(messageFormatPattern, "messageFormatPattern must not be null");
        this.fieldName = fieldName;
        this.messageFormatPattern = messageFormatPattern;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public void writeTo(JsonGenerator generator) throws IOException {
        writeFieldName(generator);
        writeFieldValue(generator);
    }
    
    /**
     * Writes the field name to the generator.
     */
    protected void writeFieldName(JsonGenerator generator) throws IOException {
        generator.writeFieldName(getFieldName());
    }
    
    /**
     * Writes the field value to the generator.
     */
    protected abstract void writeFieldValue(JsonGenerator generator) throws IOException;
    
    @Override
    public String toString() {
        final String fieldValueString = StructuredArguments.toString(getFieldValue());
        /*
         * Optimize for commonly used messageFormatPattern
         */
        if (StructuredArguments.VALUE_ONLY_MESSAGE_FORMAT_PATTERN.equals(messageFormatPattern)) {
            return fieldValueString;
        }
        if (StructuredArguments.DEFAULT_KEY_VALUE_MESSAGE_FORMAT_PATTERN.equals(messageFormatPattern)) {
            return getFieldName()
                    + "="
                    + fieldValueString;
        }
        /*
         * Custom messageFormatPattern
         */
        return MessageFormatCache.INSTANCE.getMessageFormat(this.messageFormatPattern)
                .format(new Object[] {getFieldName(), fieldValueString});
    }

    /**
     * Return the value that should be included in the output of {@link #toString()}. 
     */
    protected abstract Object getFieldValue();

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof SingleFieldAppendingMarker)) {
            return false;
        }
        
        SingleFieldAppendingMarker other = (SingleFieldAppendingMarker) obj;
        return this.fieldName.equals(other.fieldName);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + super.hashCode();
        result = prime * result + this.fieldName.hashCode();
        return result;
    }
}
