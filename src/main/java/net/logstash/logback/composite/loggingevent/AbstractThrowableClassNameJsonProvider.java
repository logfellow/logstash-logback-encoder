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

import com.fasterxml.jackson.core.JsonGenerator;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.JsonWritingUtils;

public abstract class AbstractThrowableClassNameJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> {
    static final boolean DEFAULT_USE_SIMPLE_CLASS_NAME = true;

    private boolean useSimpleClassName = DEFAULT_USE_SIMPLE_CLASS_NAME;

    public AbstractThrowableClassNameJsonProvider(String fieldName) {
        setFieldName(fieldName);
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        IThrowableProxy throwable = getThrowable(event.getThrowableProxy());
        if (throwable != null) {
            String throwableClassName = determineClassName(throwable);
            JsonWritingUtils.writeStringField(generator, getFieldName(), throwableClassName);
        }
    }

    private String determineClassName(IThrowableProxy throwable) {
        if (!useSimpleClassName) {
            return throwable.getClassName();
        }
        if (throwable instanceof ThrowableProxy) {
            return ((ThrowableProxy) throwable).getThrowable().getClass().getSimpleName();
        }
        String className = throwable.getClassName();
        return className.substring(className.lastIndexOf('.') + 1);
    }

    /**
     * @return null if no appropriate throwable
     */
    abstract IThrowableProxy getThrowable(IThrowableProxy throwable);

    public void setUseSimpleClassName(boolean useSimpleClassName) {
        this.useSimpleClassName = useSimpleClassName;
    }
}
