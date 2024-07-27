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
import java.util.HashMap;
import java.util.Map;

import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.fieldnames.LogstashAccessFieldNames;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.joran.spi.DefaultClass;
import com.fasterxml.jackson.core.JsonGenerator;

public class ResponseHeadersJsonProvider extends AbstractFieldJsonProvider<IAccessEvent> implements FieldNamesAware<LogstashAccessFieldNames> {

    /**
     * When true, names of headers will be written to JSON output in lowercase.
     */
    private boolean lowerCaseHeaderNames = true;

    private HeaderFilter filter;

    @Override
    public void writeTo(JsonGenerator generator, IAccessEvent event) throws IOException {
        Map<String, String> headers;
        if (filter == null) {
            headers = event.getResponseHeaderMap();
        } else {
            headers = new HashMap<>(event.getResponseHeaderMap().size());
            for (Map.Entry<String, String> header : event.getResponseHeaderMap().entrySet()) {
                if (filter.includeHeader(header.getKey(), header.getValue())) {
                    headers.put(header.getKey(), header.getValue());
                }
            }
        }
        JsonWritingUtils.writeMapStringFields(generator, getFieldName(), headers, lowerCaseHeaderNames);
    }

    @Override
    public void setFieldNames(LogstashAccessFieldNames fieldNames) {
        setFieldName(fieldNames.getResponseHeaders());
    }

    public boolean getLowerCaseHeaderNames() {
        return lowerCaseHeaderNames;
    }

    public void setLowerCaseHeaderNames(boolean lowerCaseHeaderNames) {
        this.lowerCaseHeaderNames = lowerCaseHeaderNames;
    }

    public HeaderFilter getFilter() {
        return filter;
    }

    @DefaultClass(IncludeExcludeHeaderFilter.class)
    public void setFilter(HeaderFilter filter) {
        this.filter = filter;
    }

}
