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

public interface MdcEntryWriter {

    /**
     * Writes the given MDC entry allowing to manipulate the output of the field name and field value.
     *
     * @param generator the generator to write the entry to.
     * @param fieldName the field name to use when writing the entry.
     * @param mdcKey    the key of the MDC map entry.
     * @param mdcValue  the value of the MDC map entry.
     * @return Return true if the {@link MdcEntryWriter} handled the output of the entry, otherwise return false.
     */
    boolean writeMdcEntry(JsonGenerator generator, String fieldName, String mdcKey, String mdcValue) throws IOException;

}
