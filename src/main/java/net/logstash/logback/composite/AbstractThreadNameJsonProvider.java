/*
 * Copyright 2013-2022 the original author or authors.
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
package net.logstash.logback.composite;

import java.io.IOException;

import net.logstash.logback.fieldnames.LogstashCommonFieldNames;

import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.core.JsonGenerator;

public abstract class AbstractThreadNameJsonProvider<Event extends DeferredProcessingAware> extends AbstractFieldJsonProvider<Event> implements FieldNamesAware<LogstashCommonFieldNames> {

    public static final String FIELD_THREAD_NAME = "thread_name";
    
    public AbstractThreadNameJsonProvider() {
        setFieldName(FIELD_THREAD_NAME);
    }
    
    @Override
    public void writeTo(JsonGenerator generator, Event event) throws IOException {
        JsonWritingUtils.writeStringField(generator, getFieldName(), getThreadName(event));
    }
    
    @Override
    public void setFieldNames(LogstashCommonFieldNames fieldNames) {
        setFieldName(fieldNames.getThread());
    }

    protected abstract String getThreadName(Event event);
}
