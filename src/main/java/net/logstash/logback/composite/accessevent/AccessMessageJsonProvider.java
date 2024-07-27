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
package net.logstash.logback.composite.accessevent;

import java.io.IOException;

import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.composite.loggingevent.MessageJsonProvider;
import net.logstash.logback.fieldnames.LogstashAccessFieldNames;

import ch.qos.logback.access.common.spi.IAccessEvent;
import com.fasterxml.jackson.core.JsonGenerator;

public class AccessMessageJsonProvider extends AccessEventFormattedTimestampJsonProvider {

    public static final String FIELD_MESSAGE = MessageJsonProvider.FIELD_MESSAGE;

    public AccessMessageJsonProvider() {
        setFieldName(FIELD_MESSAGE);
    }

    @Override
    public void writeTo(JsonGenerator generator, IAccessEvent event) throws IOException {
        JsonWritingUtils.writeStringField(generator,
                getFieldName(),
                String.format("%s - %s [%s] \"%s\" %s %s",
                        event.getRemoteHost(),
                        event.getRemoteUser() == null ? "-" : event.getRemoteUser(),
                        getFormattedTimestamp(event),
                        event.getRequestURL(),
                        event.getStatusCode(),
                        event.getContentLength()));
    }

    @Override
    public void setFieldNames(LogstashAccessFieldNames fieldNames) {
        setFieldName(fieldNames.getMessage());
    }

}
