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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.fieldnames.LogstashFieldNames;

import java.io.IOException;

/**
 * A JSON provider that adds a {@code rootStackTraceElement} Json field on a log with a stack trace
 * <p>
 *
 * @author Daniel Albuquerque
 */
public class RootStackTraceElementJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> implements FieldNamesAware<LogstashFieldNames> {

    public static final String FIELD_CLASS_NAME = "class_name";
    public static final String FIELD_METHOD_NAME = "method_name";
    public static final String FIELD_STACKTRACE_ELEMENT = "root_stack_trace_element";
    private String fieldClassName = FIELD_CLASS_NAME;
    private String fieldMethodName = FIELD_METHOD_NAME;

    public RootStackTraceElementJsonProvider() {
        setFieldName(FIELD_STACKTRACE_ELEMENT);
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null && throwableProxy instanceof ThrowableProxy) {
            if (throwableProxy.getStackTraceElementProxyArray().length > 0) {
                StackTraceElement stackTraceElement = throwableProxy.getStackTraceElementProxyArray()[0].getStackTraceElement();

                generator.writeObjectFieldStart(getFieldName());
                JsonWritingUtils.writeStringField(generator, fieldClassName, stackTraceElement.getClassName());
                JsonWritingUtils.writeStringField(generator, fieldMethodName, stackTraceElement.getMethodName());
                generator.writeEndObject();
            }
        }
    }

    @Override
    public void setFieldNames(LogstashFieldNames fieldNames) {
        setFieldClassName(fieldNames.getRootStackTraceElementClass());
        setFieldMethodName(fieldNames.getRootStackTraceElementMethod());
    }

    private void setFieldClassName(String fieldClassName) {
        this.fieldClassName = fieldClassName;
    }

    private void setFieldMethodName(String fieldMethodName) {
        this.fieldMethodName = fieldMethodName;
    }
}
