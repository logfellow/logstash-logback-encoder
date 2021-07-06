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
package net.logstash.logback.composite.accessevent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A simple {@link HeaderFilter} that determines whether or not a header is included based
 * on a set of included header names or excluded header names.
 * 
 * If both includes and excludes are empty, then all header names will be included.
 * 
 * If includes is not empty and excludes is empty, then only those headers in the includes will be included.
 * 
 * If includes is empty and excludes is not empty, then all headers except those in the excludes will be included.
 * 
 * If includes is not empty and excludes is not empty, then an exception will be thrown.
 * 
 * All comparisons are case-insensitive.
 */
public class IncludeExcludeHeaderFilter implements HeaderFilter {

    private Set<String> includes = new HashSet<>();
    private Set<String> excludes = new HashSet<>();

    @Override
    public boolean includeHeader(String headerName, String headerValue) {
        if (includes.isEmpty() && excludes.isEmpty()) {
            return true;
        } else if (excludes.isEmpty()) {
            return includes.contains(headerName.toLowerCase());
        } else if (includes.isEmpty()) {
            return !excludes.contains(headerName.toLowerCase());
        } else {
            throw new IllegalStateException(String.format("Both includes (%s) and excludes (%s) cannot be configured at the same time.  Only one or the other, or neither can be configured.", includes, excludes));
        }
    }
    
    public Set<String> getIncludes() {
        return Collections.unmodifiableSet(includes);
    }
    public Set<String> getExcludes() {
        return Collections.unmodifiableSet(excludes);
    }
    
    public void addInclude(String include) {
        this.includes.add(include.toLowerCase());
    }
    public void removeInclude(String include) {
        this.includes.remove(include.toLowerCase());
    }
    
    public void addExclude(String exclude) {
        this.excludes.add(exclude.toLowerCase());
    }
    public void removeExclude(String exclude) {
        this.excludes.remove(exclude.toLowerCase());
    }
    
}
