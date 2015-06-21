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

import net.logstash.logback.argument.NamedArgument;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.fieldnames.LogstashFieldNames;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Include event {@link ILoggingEvent#getArgumentArray()} in the JSON output
 *
 * <p>
 *     Arguments that are an instance of {@link NamedArgument} will be outputted as JSON object.
 *     {@link NamedArgument#getKey()} as the fieldname and {@link NamedArgument#getValue()} as the value.
 *
 *     Other arguments will be omitted until {@link #includeArgumentWithNoKey} is set. They will be included
 *     in the JSON output as JSON object.
 *     {@link #argumentWithNoKeyPrefix} prepend to the argument index as the field name and the argument as the value.
 *
 * </p>
 *
 * If the fieldName is set, then the properties will be written
 * to that field as a subobject.
 * Otherwise, the properties are written inline.
 *
 */
public class ArgumentsJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> implements FieldNamesAware<LogstashFieldNames> {

    private boolean includeArgumentWithNoKey;
    private String argumentWithNoKeyPrefix = "arg";

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {

        Object[] args = event.getArgumentArray();

        if (args == null || args.length == 0) {
            return;
        }

        boolean hasKv = false;

        for (int argIte = 0; argIte < args.length; argIte++) {

            Object arg = args[argIte];

            String key = null;
            Object value = null;

            if (arg instanceof NamedArgument) {
                NamedArgument namedArgument = (NamedArgument) arg;
                key = namedArgument.getKey();
                value = namedArgument.getValue();
            } else if (includeArgumentWithNoKey) {
                key = argumentWithNoKeyPrefix + (argIte+1);
                value = arg;
            }

            if (key != null) {
                //Only write an object field if there is a match and the fieldName is set
                if (!hasKv && getFieldName() != null) {
                    generator.writeObjectFieldStart(getFieldName());
                }
                hasKv = true;
                generator.writeObjectField(key, value);
            }
        }

        if (hasKv && getFieldName() != null) {
            generator.writeEndObject();
        }
    }

    @Override
    public void setFieldNames(LogstashFieldNames fieldNames) {
        setFieldName(fieldNames.getArguments());
    }


    public boolean isIncludeArgumentWithNoKey() {
        return includeArgumentWithNoKey;
    }

    public void setIncludeArgumentWithNoKey(boolean includeArgumentWithNoKey) {
        this.includeArgumentWithNoKey = includeArgumentWithNoKey;
    }

    public String getArgumentWithNoKeyPrefix() {
        return argumentWithNoKeyPrefix;
    }

    public void setArgumentWithNoKeyPrefix(String argumentWithNoKeyPrefix) {
        this.argumentWithNoKeyPrefix = argumentWithNoKeyPrefix;
    }
}
