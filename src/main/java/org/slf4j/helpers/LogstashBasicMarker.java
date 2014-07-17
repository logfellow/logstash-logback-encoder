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
package org.slf4j.helpers;

/**
 * A hack to make {@link BasicMarker} extendable since {@link BasicMarker}'s constructor is package-private.
 * 
 * The alternative is to re-implement the logic in {@link BasicMarker}.
 */
@SuppressWarnings("serial")
public class LogstashBasicMarker extends BasicMarker {
    
    public LogstashBasicMarker(String name) {
        super(name);
    }
}
