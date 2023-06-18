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
package net.logstash.logback.composite.loggingevent.mdc;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Writes boolean values (instead of String values) for any MDC values that equal "true" or "false", ignoring case.
 */
public class BooleanMdcEntryWriter implements MdcEntryWriter {

    @Override
    public boolean writeMdcEntry(JsonGenerator generator, String fieldName, String mdcKey, String mdcValue) throws IOException {
        if ("true".equalsIgnoreCase(mdcValue)) {
            generator.writeFieldName(fieldName);
            generator.writeBoolean(true);
            return true;
        }
        if ("false".equalsIgnoreCase(mdcValue)) {
            generator.writeFieldName(fieldName);
            generator.writeBoolean(false);
            return true;
        }

        return false;
    }

}
