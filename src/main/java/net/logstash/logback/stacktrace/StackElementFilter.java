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
package net.logstash.logback.stacktrace;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Component in charge of accepting or rejecting {@link StackTraceElement elements} when computing a stack trace hash
 */
public abstract class StackElementFilter {
    /**
     * Tests whether or not the specified {@link StackTraceElement} should be
     * accepted when computing a stack hash.
     *
     * @param element The {@link StackTraceElement} to be tested
     * @return {@code true} if and only if {@code element} should be accepted
     */
    public abstract boolean accept(StackTraceElement element);

    /**
     * Creates a {@link StackElementFilter} that accepts any stack trace elements
     * 
     * @return the filter
     */
    public static final StackElementFilter any() {
        return new StackElementFilter() {
            @Override
            public boolean accept(StackTraceElement element) {
                return true;
            }
        };
    }

    /**
     * Creates a {@link StackElementFilter} that accepts all stack trace elements with a non {@code null}
     * {@code {@link StackTraceElement#getFileName()} filename} and positive {@link StackTraceElement#getLineNumber()} line number}
     * 
     * @return the filter
     */
    public static final StackElementFilter withSourceInfo() {
        return new StackElementFilter() {
            @Override
            public boolean accept(StackTraceElement element) {
                return element.getFileName() != null && element.getLineNumber() >= 0;
            }
        };
    }

    /**
     * Creates a {@link StackElementFilter} by exclusion {@link Pattern patterns}
     *
     * @param excludes regular expressions matching {@link StackTraceElement} to filter out
     * @return the filter
     */
    public static final StackElementFilter byPattern(final List<Pattern> excludes) {
        return new StackElementFilter() {
            @Override
            public boolean accept(StackTraceElement element) {
                if (!excludes.isEmpty()) {
                    String classNameAndMethod = element.getClassName() + "." + element.getMethodName();
                    for (Pattern exclusionPattern : excludes) {
                        if (exclusionPattern.matcher(classNameAndMethod).find()) {
                            return false;
                        }
                    }
                }
                return true;
            }
        };
    }
}
