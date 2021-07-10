/*
 * Copyright 2013-2021 the original author or authors.
 *
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

import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.fieldnames.LogstashFieldNames;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * A JSON provider that, for any log event with a stack trace,
 * adds a {@code root_stack_trace_element} JSON object field
 * containing the class name and method name where the outer-most exception was thrown.
 *
 * @author Daniel Albuquerque
 */
public class RootStackTraceElementJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> implements FieldNamesAware<LogstashFieldNames> {

    public static final String FIELD_CLASS_NAME = "class_name";
    public static final String FIELD_METHOD_NAME = "method_name";
    public static final String FIELD_STACKTRACE_ELEMENT = "root_stack_trace_element";

    private String classFieldName = FIELD_CLASS_NAME;
    private String methodFieldName = FIELD_METHOD_NAME;

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
                JsonWritingUtils.writeStringField(generator, classFieldName, stackTraceElement.getClassName());
                JsonWritingUtils.writeStringField(generator, methodFieldName, stackTraceElement.getMethodName());
                generator.writeEndObject();
            }
        }
    }

    @Override
    public void setFieldNames(LogstashFieldNames fieldNames) {
        setFieldName(fieldNames.getRootStackTraceElement());
        setClassFieldName(fieldNames.getRootStackTraceElementClass());
        setMethodFieldName(fieldNames.getRootStackTraceElementMethod());
    }

    public void setClassFieldName(String classFieldName) {
        this.classFieldName = classFieldName;
    }

    public void setMethodFieldName(String methodFieldName) {
        this.methodFieldName = methodFieldName;
    }
}
