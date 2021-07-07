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
package net.logstash.logback.argument;

import java.io.IOException;

import net.logstash.logback.composite.loggingevent.ArgumentsJsonProvider;

import com.fasterxml.jackson.core.JsonGenerator;
import org.slf4j.Logger;

/**
 * A wrapper for an argument passed to a log method (e.g. {@link Logger#info(String, Object...)})
 * that adds data to the JSON event (via {@link ArgumentsJsonProvider}).
 */
public interface StructuredArgument {

    /**
     * Writes the data associated with this argument to the given {@link JsonGenerator}.
     */
    void writeTo(JsonGenerator generator) throws IOException;
    
    /**
     * Writes the data associated with this argument to a {@link String} to be
     * included in a log event's formatted message (via parameter substitution).
     * <p>
     * Note that this will only be included in the log event's formatted
     * message if the message format includes a parameter for this argument.
     */
    String toString();

}


