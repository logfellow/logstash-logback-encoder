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
package net.logstash.logback.fieldnames;

/**
 * Slightly shortened versions of the {@link LogstashFieldNames}.
 * Specifically, no underscores are used,
 * the "_name" and "_number" suffixes have been removed, and
 * caller data is wrapped in a "caller" object.
 */
public class ShortenedFieldNames extends LogstashFieldNames {
    
    public static final String FIELD_LOGGER = "logger";
    public static final String FIELD_THREAD = "thread";
    public static final String FIELD_LEVEL_VAL = "levelVal";
    public static final String FIELD_CALLER = "caller";
    public static final String FIELD_CLASS = "class";
    public static final String FIELD_METHOD = "method";
    public static final String FIELD_FILE = "file";
    public static final String FIELD_LINE = "line";
    public static final String FIELD_STACKTRACE = "stacktrace";

    public ShortenedFieldNames() {
        setLogger(FIELD_LOGGER);
        setThread(FIELD_THREAD);
        setLevelValue(FIELD_LEVEL_VAL);
        setCaller(FIELD_CALLER);
        setCallerClass(FIELD_CLASS);
        setCallerMethod(FIELD_METHOD);
        setCallerFile(FIELD_FILE);
        setCallerLine(FIELD_LINE);
        setStackTrace(FIELD_STACKTRACE);
        setRootStackTraceElementClass(FIELD_CLASS);
        setRootStackTraceElementMethod(FIELD_METHOD);
    }
    
}
