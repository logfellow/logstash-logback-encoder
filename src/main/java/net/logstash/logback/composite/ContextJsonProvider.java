/*
 * Copyright 2013-2023 the original author or authors.
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

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Writes properties from the {@link Context} into the JSON event.
 *
 * If the fieldName is set, then the properties will be written
 * to that field as a subobject.
 * Otherwise, the properties are written inline.
 *
 * @param <Event> type of event ({@link ILoggingEvent} or {@link IAccessEvent}).
 */
public class ContextJsonProvider<Event extends DeferredProcessingAware> extends AbstractFieldJsonProvider<Event> implements FieldNamesAware<LogstashCommonFieldNames> {

    @Override
    public void writeTo(JsonGenerator generator, Event event) throws IOException {
        if (getContext() != null) {
            if (getFieldName() != null) {
                generator.writeObjectFieldStart(getFieldName());
            }
            JsonWritingUtils.writeMapEntries(generator, context.getCopyOfPropertyMap());

            if (getFieldName() != null) {
                generator.writeEndObject();
            }
        }
    }

    @Override
    public void setFieldNames(LogstashCommonFieldNames fieldNames) {
        setFieldName(fieldNames.getContext());
    }

}
