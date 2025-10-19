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
package net.logstash.logback.composite.loggingevent.mdc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import tools.jackson.core.JsonGenerator;

/**
 * Writes MDC entries by delegating to other instances of {@link MdcEntryWriter} if MDC key matches the given include
 * and exclude pattern.
 *
 * <ol>
 * <li>An MDC entry is written if the MDC key does match the {@link #includeMdcKeyPattern} AND does not match
 *     the {@link #excludeMdcKeyPattern}.</li>
 * <li>Omitting a {@link #includeMdcKeyPattern} means to include all MDC keys.</li>
 * <li>Omitting a {@link #excludeMdcKeyPattern} means to exclude no MDC keys.</li>
 * </ol>
 */
public class RegexFilteringMdcEntryWriter implements MdcEntryWriter {

    private Pattern includeMdcKeyPattern;
    private Pattern excludeMdcKeyPattern;
    private final List<MdcEntryWriter> mdcEntryWriters = new ArrayList<>();

    @Override
    public boolean writeMdcEntry(JsonGenerator generator, String fieldName, String mdcKey, String mdcValue) {
        if (shouldWrite(mdcKey)) {
            for (MdcEntryWriter mdcEntryWriter : this.mdcEntryWriters) {
                if (mdcEntryWriter.writeMdcEntry(generator, fieldName, mdcKey, mdcValue)) {
                    return true;
                }
            }
        }

        return false;
    }

    public Pattern getIncludeMdcKeyPattern() {
        return includeMdcKeyPattern;
    }
    public void setIncludeMdcKeyPattern(String includeMdcKeyPattern) {
        this.includeMdcKeyPattern = Pattern.compile(includeMdcKeyPattern);
    }

    public Pattern getExcludeMdcKeyPattern() {
        return excludeMdcKeyPattern;
    }
    public void setExcludeMdcKeyPattern(String excludeMdcKeyPattern) {
        this.excludeMdcKeyPattern = Pattern.compile(excludeMdcKeyPattern);
    }

    public List<MdcEntryWriter> getMdcEntryWriters() {
        return Collections.unmodifiableList(mdcEntryWriters);
    }
    public void addMdcEntryWriter(MdcEntryWriter mdcEntryWriter) {
        this.mdcEntryWriters.add(mdcEntryWriter);
    }

    /** Returns true if passed MDC key should be written to the JSON output. */
    private boolean shouldWrite(String key) {
        if (this.mdcEntryWriters.isEmpty()) {
            return false;
        }
        boolean includeKey = includeMdcKeyPattern == null || includeMdcKeyPattern.matcher(key).matches();
        boolean excludeKey = excludeMdcKeyPattern != null && excludeMdcKeyPattern.matcher(key).matches();
        return includeKey && !excludeKey;
    }

}
