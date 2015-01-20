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

import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.JsonWritingUtils;
import ch.qos.logback.classic.spi.ILoggingEvent;

import com.fasterxml.jackson.core.JsonGenerator;

public class CallerDataJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> {
    private static final StackTraceElement DEFAULT_CALLER_DATA = new StackTraceElement("", "", "", 0);

    public static final String FIELD_CALLER_CLASS_NAME = "caller_class_name";
    public static final String FIELD_CALLER_METHOD_NAME = "caller_method_name";
    public static final String FIELD_CALLER_FILE_NAME = "caller_file_name";
    public static final String FIELD_CALLER_LINE_NUMBER = "caller_line_number";

    private String classFieldName = FIELD_CALLER_CLASS_NAME;
    private String methodFieldName = FIELD_CALLER_METHOD_NAME;
    private String fileFieldName = FIELD_CALLER_FILE_NAME;
    private String lineFieldName = FIELD_CALLER_LINE_NUMBER;
    
    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        StackTraceElement callerData = extractCallerData(event);
        if (getFieldName() != null) {
            generator.writeObjectFieldStart(getFieldName());
        }
        JsonWritingUtils.writeStringField(generator, classFieldName, callerData.getClassName());
        JsonWritingUtils.writeStringField(generator, methodFieldName, callerData.getMethodName());
        JsonWritingUtils.writeStringField(generator, fileFieldName, callerData.getFileName());
        JsonWritingUtils.writeNumberField(generator, lineFieldName, callerData.getLineNumber());
        if (getFieldName() != null) {
            generator.writeEndObject();
        }
    }
    
    @Override
    public void prepareForDeferredProcessing(ILoggingEvent event) {
        super.prepareForDeferredProcessing(event);
        event.getCallerData();
    }
    private StackTraceElement extractCallerData(final ILoggingEvent event) {
        final StackTraceElement[] ste = event.getCallerData();
        if (ste == null || ste.length == 0) {
            return DEFAULT_CALLER_DATA;
        }
        return ste[0];
    }
    
    public String getClassFieldName() {
        return classFieldName;
    }
    public void setClassFieldName(String callerClassFieldName) {
        this.classFieldName = callerClassFieldName;
    }
    public String getMethodFieldName() {
        return methodFieldName;
    }
    public void setMethodFieldName(String callerMethodFieldName) {
        this.methodFieldName = callerMethodFieldName;
    }
    public String getFileFieldName() {
        return fileFieldName;
    }
    public void setFileFieldName(String callerFileFieldName) {
        this.fileFieldName = callerFileFieldName;
    }
    public String getLineFieldName() {
        return lineFieldName;
    }
    public void setLineFieldName(String callerLineFieldName) {
        this.lineFieldName = callerLineFieldName;
    }
}
