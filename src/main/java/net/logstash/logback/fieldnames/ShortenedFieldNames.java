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

/**
 * Slightly shortened versions of the {@link LogstashFieldNames}.
 * Specifically, no underscores are used,
 * the "_name" and "_number" suffixes have been removed, and
 * caller data is wrapped in a "caller" object.
 */
public class ShortenedFieldNames extends LogstashFieldNames {
    
    public ShortenedFieldNames() {
        setLogger("logger");
        setThread("thread");
        setLevelValue("levelVal");
        setCaller("caller");
        setCallerClass("class");
        setCallerMethod("method");
        setCallerFile("file");
        setCallerLine("line");
        setStackTrace("stacktrace");
    }
    
}
