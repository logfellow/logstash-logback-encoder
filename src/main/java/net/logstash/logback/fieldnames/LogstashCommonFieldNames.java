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

import net.logstash.logback.composite.FormattedTimestampJsonProvider;
import net.logstash.logback.composite.LogstashVersionJsonProvider;
import net.logstash.logback.composite.loggingevent.MessageJsonProvider;

/**
 * Common field names between the regular {@link net.logstash.logback.LogstashFormatter}
 * and the {@link net.logstash.logback.LogstashAccessFormatter}. 
 */
public abstract class LogstashCommonFieldNames {
    /**
     * Field name to use in logback configuration files
     * if you want the field to be ignored (not output).
     * 
     * Unfortunately, logback does not provide a way to set a
     * field value to null via xml config,
     * so we have to fall back to using this magic string.
     * 
     * Note that if you're programmatically configuring the field names,
     * then you can just set the field name to null in the
     * FieldNamesType.  
     */
    public static final String IGNORE_FIELD_INDICATOR = "[ignore]";
  
    private String timestamp = FormattedTimestampJsonProvider.FIELD_TIMESTAMP;
    private String version = LogstashVersionJsonProvider.FIELD_VERSION;
    private String message = MessageJsonProvider.FIELD_MESSAGE;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
