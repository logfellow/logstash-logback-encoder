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
package net.logstash.logback.marker;

import java.io.IOException;

import net.logstash.logback.argument.StructuredArgument;

import com.fasterxml.jackson.core.JsonGenerator;
import org.slf4j.Marker;

/**
 * An empty marker that does nothing itself, but can be used as a base marker when you want to conditionally chain other markers with {@link #and(Marker)}.
 * For example:
 *
 * <pre>
 *     LogstashMarker marker = Markers.empty();
 *     if (condition1) {
 *         marker = marker.and(Markers.append("fieldName1", value1);
 *     }
 *     if (condition2) {
 *         marker = marker.and(Markers.append("fieldName2", value2);
 *     }
 * </pre>
 */
public class EmptyLogstashMarker extends LogstashMarker implements StructuredArgument {

    public static final String EMPTY_MARKER_NAME = "EMPTY";

    public EmptyLogstashMarker() {
        super(EMPTY_MARKER_NAME);
    }

    @Override
    public void writeTo(JsonGenerator generator) throws IOException {
        // no-op
    }

    @Override
    protected String toStringSelf() {
        return "";
    }
}
