/**
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

import net.logstash.logback.composite.loggingevent.CallerDataJsonProvider;
import net.logstash.logback.composite.loggingevent.LogLevelJsonProvider;
import net.logstash.logback.composite.loggingevent.LogLevelValueJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggerNameJsonProvider;
import net.logstash.logback.composite.loggingevent.RootStackTraceElementJsonProvider;
import net.logstash.logback.composite.loggingevent.StackTraceJsonProvider;
import net.logstash.logback.composite.loggingevent.TagsJsonProvider;
import net.logstash.logback.composite.loggingevent.ThreadNameJsonProvider;
import net.logstash.logback.composite.loggingevent.UuidProvider;

/**
 * Names of standard fields that appear in the JSON output.
 */
public class LogstashFieldNames extends LogstashCommonFieldNames {
    
    private String logger = LoggerNameJsonProvider.FIELD_LOGGER_NAME;
    private String thread = ThreadNameJsonProvider.FIELD_THREAD_NAME;
    private String level = LogLevelJsonProvider.FIELD_LEVEL;
    private String levelValue = LogLevelValueJsonProvider.FIELD_LEVEL_VALUE;
    private String caller;
    private String callerClass = CallerDataJsonProvider.FIELD_CALLER_CLASS_NAME;
    private String callerMethod = CallerDataJsonProvider.FIELD_CALLER_METHOD_NAME;
    private String callerFile = CallerDataJsonProvider.FIELD_CALLER_FILE_NAME;
    private String callerLine = CallerDataJsonProvider.FIELD_CALLER_LINE_NUMBER;
    private String stackTrace = StackTraceJsonProvider.FIELD_STACK_TRACE;
    private String rootStackTraceElement = RootStackTraceElementJsonProvider.FIELD_STACKTRACE_ELEMENT;
    private String rootStackTraceElementClass = RootStackTraceElementJsonProvider.FIELD_CLASS_NAME;
    private String rootStackTraceElementMethod = RootStackTraceElementJsonProvider.FIELD_METHOD_NAME;
    private String tags = TagsJsonProvider.FIELD_TAGS;
    private String mdc;
    private String context;
    private String arguments;
    private String uuid = UuidProvider.FIELD_UUID;

    public String getLogger() {
        return logger;
    }
    
    public void setLogger(String logger) {
        this.logger = logger;
    }
    
    public String getThread() {
        return thread;
    }
    
    public void setThread(String thread) {
        this.thread = thread;
    }
    
    public String getLevel() {
        return level;
    }
    
    public void setLevel(String level) {
        this.level = level;
    }
    
    public String getLevelValue() {
        return levelValue;
    }
    
    public void setLevelValue(String levelValue) {
        this.levelValue = levelValue;
    }
    
    /**
     * The name of the caller object field.
     * <p>
     * If this returns null, then the caller data fields will be written inline at the root level of the JSON event output (e.g. as a sibling to all the other fields in this class).
     * <p>
     * If this returns non-null, then the caller data fields will be written inside an object with field name returned by this method
     *
     * @return The name of the caller object field.
     */
    public String getCaller() {
        return caller;
    }
    
    public void setCaller(String caller) {
        this.caller = caller;
    }
    
    public String getCallerClass() {
        return callerClass;
    }
    
    public void setCallerClass(String callerClass) {
        this.callerClass = callerClass;
    }
    
    public String getCallerMethod() {
        return callerMethod;
    }
    
    public void setCallerMethod(String callerMethod) {
        this.callerMethod = callerMethod;
    }
    
    public String getCallerFile() {
        return callerFile;
    }
    
    public void setCallerFile(String callerFile) {
        this.callerFile = callerFile;
    }
    
    public String getCallerLine() {
        return callerLine;
    }
    
    public void setCallerLine(String callerLine) {
        this.callerLine = callerLine;
    }
    
    public String getStackTrace() {
        return stackTrace;
    }
    
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }
    
    /**
     * The name of the mdc object field.
     * <p>
     * If this returns null, then the mdc fields will be written inline at the root level of the JSON event output (e.g. as a sibling to all the other fields in this class).
     * <p>
     * If this returns non-null, then the mdc fields will be written inside an object with field name returned by this method
     *
     * @return The name of the mdc object field.
     */
    public String getMdc() {
        return mdc;
    }
    
    public void setMdc(String mdc) {
        this.mdc = mdc;
    }
    
    /**
     * The name of the context object field.
     * <p>
     * If this returns null, then the context fields will be written inline at the root level of the JSON event output (e.g. as a sibling to all the other fields in this class).
     * <p>
     * If this returns non-null, then the context fields will be written inside an object with field name returned by this method
     * @return The name of the context object field.
     */
    public String getContext() {
        return context;
    }
    
    public void setContext(String context) {
        this.context = context;
    }
    
    /**
     * The name of the arguments object field.
     * <p>
     * If this returns null, then the arguments will be written inline at the root level of the JSON event output (e.g. as a sibling to all the other fields in this class).
     * <p>
     * If this returns non-null, then the arguments will be written inside an object with field name returned by this method
     * @return The name of the arguments object field.
     */
    public String getArguments() {
        return arguments;
    }
    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getRootStackTraceElement() {
        return rootStackTraceElement;
    }

    public void setRootStackTraceElement(String rootStackTraceElement) {
        this.rootStackTraceElement = rootStackTraceElement;
    }

    public String getRootStackTraceElementMethod() {
        return rootStackTraceElementMethod;
    }

    public void setRootStackTraceElementMethod(String rootStackTraceElementMethod) {
        this.rootStackTraceElementMethod = rootStackTraceElementMethod;
    }

    public String getRootStackTraceElementClass() {
        return rootStackTraceElementClass;
    }

    public void setRootStackTraceElementClass(String rootStackTraceElementClass) {
        this.rootStackTraceElementClass = rootStackTraceElementClass;
    }
}
