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
package net.logstash.logback.encoder;


/**
 * 
 * @deprecated The functionality from this encoder has been merged into {@link CompositeJsonEncoder} (a superclass).
 *             There is no reason to use this class directly anymore.
 *             It remains here for backwards compatibility.
 */
@Deprecated
public class LogstashTcpEncoder extends LogstashEncoder {

    /**
     * @see CompositeJsonEncoder#setLineSeparator(String)
     */
    public void setNewLine(String newLine) {
        setLineSeparator(newLine);
    }
    
    /**
     * @see CompositeJsonEncoder#getLineSeparator()
     */
    public String getNewLine() {
        return getLineSeparator();
    }
    
}