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
package net.logstash.logback.composite.loggingevent;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.composite.ContextJsonProvider;
import net.logstash.logback.composite.GlobalCustomFieldsJsonProvider;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.composite.LogstashVersionJsonProvider;

/**
 * Used to make it make it more convenient to create well-known
 * {@link JsonProviders} via xml configuration.
 * <p>
 * For example, instead of:
 * {@code
 *     <provider class="net.logstash.logback.composite.loggingevent.LoggingEventFormattedTimestampJsonProvider"/> 
 * }
 * you can just use:
 * {@code
 *     <timestamp/> 
 * }
 */
public class LoggingEventJsonProviders extends JsonProviders<ILoggingEvent> {

    public void addTimestamp(LoggingEventFormattedTimestampJsonProvider provider) {
        addProvider(provider);
    }
    public void addVersion(LogstashVersionJsonProvider<ILoggingEvent> provider) {
        addProvider(provider);
    }
    public void addMessage(MessageJsonProvider provider) {
        addProvider(provider);
    }
    public void addRawMessage(RawMessageJsonProvider provider) {
        addProvider(provider);
    }
    public void addLoggerName(LoggerNameJsonProvider provider) {
        addProvider(provider);
    }
    public void addThreadName(ThreadNameJsonProvider provider) {
        addProvider(provider);
    }
    public void addLogLevel(LogLevelJsonProvider provider) {
        addProvider(provider);
    }
    public void addLogLevelValue(LogLevelValueJsonProvider provider) {
        addProvider(provider);
    }
    public void addCallerData(CallerDataJsonProvider provider) {
        addProvider(provider);
    }
    public void addStackTrace(StackTraceJsonProvider provider) {
        addProvider(provider);
    }
    public void addStackHash(StackHashJsonProvider provider) {
        addProvider(provider);
    }
    public void addContext(ContextJsonProvider<ILoggingEvent> provider) {
        addProvider(provider);
    }
    public void addContextName(ContextNameJsonProvider provider) {
        addProvider(provider);
    }
    public void addJsonMessage(JsonMessageJsonProvider provider) {
        addProvider(provider);
    }
    public void addMdc(MdcJsonProvider provider) {
        addProvider(provider);
    }
    public void addContextMap(ContextMapJsonProvider provider) {
        addProvider(provider);
    }
    public void addGlobalCustomFields(GlobalCustomFieldsJsonProvider<ILoggingEvent> provider) {
        addProvider(provider);
    }
    public void addTags(TagsJsonProvider provider) {
        addProvider(provider);
    }
    public void addLogstashMarkers(LogstashMarkersJsonProvider provider) {
        addProvider(provider);
    }
    public void addPattern(LoggingEventPatternJsonProvider provider) {
        addProvider(provider);
    }
    public void addArguments(ArgumentsJsonProvider provider) {
        addProvider(provider);
    }
    public void addNestedField(LoggingEventNestedJsonProvider provider) {
        addProvider(provider);
    }
    public void addUuid(UuidProvider provider) {
        addProvider(provider);
    }
    public void addThrowableClassName(ThrowableClassNameJsonProvider provider) {
        addProvider(provider);
    }
    public void addThrowableRootCauseClassName(ThrowableRootCauseClassNameJsonProvider provider) {
        addProvider(provider);
    }
    public void addSequence(SequenceJsonProvider provider) { addProvider(provider); }

}
