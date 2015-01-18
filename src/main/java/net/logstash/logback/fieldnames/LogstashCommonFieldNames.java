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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Common field names between the regular {@link net.logstash.logback.LogstashFormatter}
 * and the {@link net.logstash.logback.LogstashAccessFormatter}. 
 */
public abstract class LogstashCommonFieldNames {
    private String timestamp = "@timestamp";
    private String version = "@version";
    private String message = "message";
    private String mdcFieldNames = null;
    private List<String> mdcFieldNamesList = Collections.emptyList();

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

    public String getMdcFieldNames() {
        return this.mdcFieldNames;
    }

    public void setMdcFieldNames(String commaSeparatedFieldNames) {
        this.mdcFieldNames = commaSeparatedFieldNames;
        if (this.mdcFieldNames != null) {
            this.mdcFieldNamesList = Collections.unmodifiableList(Arrays.asList(mdcFieldNames.split(",")));
        }
    }

    public List<String> getMdcFieldNamesList() {
        return this.mdcFieldNamesList;
    }
}
