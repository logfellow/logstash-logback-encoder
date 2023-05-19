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
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonGenerator;

public class DoubleMdcEntryWriter implements MdcEntryWriter {

    protected static final Pattern PATTERN_DOUBLE = Pattern.compile("[-+]?\\d+(\\.\\d+)?([Ee][-+]?\\d+)?");

    @Override
    public boolean writeMdcEntry(JsonGenerator generator, String fieldName, String mdcKey, String mdcValue) throws IOException {
        if (canHandle(mdcValue)) {
            try {
                double parsedValue = Double.parseDouble(mdcValue);
                generator.writeFieldName(fieldName);
                generator.writeNumber(parsedValue);
                return true;
            } catch (NumberFormatException ignore) {
            }
        }

        return false;
    }

    protected boolean canHandle(String value) {
        return value != null && !value.isEmpty() && PATTERN_DOUBLE.matcher(value).matches();
    }
}
