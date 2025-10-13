/*
 * Copyright 2013-2025 the original author or authors.
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
package net.logstash.logback.composite.loggingevent;

import java.util.Iterator;
import java.util.List;

import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.fieldnames.LogstashFieldNames;
import net.logstash.logback.marker.LogstashMarker;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.Marker;
import tools.jackson.core.JsonGenerator;

/**
 * Writes {@link Marker} names as an array to the 'tags' field.
 * 
 * <p>Does not write any special {@link LogstashMarker}s
 * (Those are handled by {@link LogstashMarkersJsonProvider}).</p>
 */
public class TagsJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> implements FieldNamesAware<LogstashFieldNames> {

    public static final String FIELD_TAGS = "tags";
    
    public TagsJsonProvider() {
        setFieldName(FIELD_TAGS);
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) {
        /*
         * Don't write the tags field unless we actually have a tag to write.
         */
        boolean hasWrittenStart = false;

        hasWrittenStart = writeTagIfNecessary(generator, hasWrittenStart, event.getMarkerList());

        if (hasWrittenStart) {
            generator.writeEndArray();
        }
    }

    private boolean writeTagIfNecessary(JsonGenerator generator, boolean hasWrittenStart, final List<Marker> markers) {
        if (markers != null) {
            for (Marker marker: markers) {
                hasWrittenStart |= writeTagIfNecessary(generator, hasWrittenStart, marker);
            }
        }
        
        return hasWrittenStart;
    }
    
    private boolean writeTagIfNecessary(JsonGenerator generator, boolean hasWrittenStart, final Marker marker) {
        if (marker != null) {
            if (!LogstashMarkersJsonProvider.isLogstashMarker(marker)) {
                if (!hasWrittenStart) {
                    generator.writeArrayPropertyStart(getFieldName());
                    hasWrittenStart = true;
                }
                generator.writeString(marker.getName());
            }

            if (marker.hasReferences()) {
                for (Iterator<?> i = marker.iterator(); i.hasNext();) {
                    Marker next = (Marker) i.next();
                    
                    hasWrittenStart |= writeTagIfNecessary(generator, hasWrittenStart, next);
                }
            }
        }
        return hasWrittenStart;
    }
    
    @Override
    public void setFieldNames(LogstashFieldNames fieldNames) {
        setFieldName(fieldNames.getTags());
    }
    
}
