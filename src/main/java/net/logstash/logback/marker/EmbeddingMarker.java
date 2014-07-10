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
package net.logstash.logback.marker;

/**
 * A marker that embeds the contents of another object into the logstash json event
 * (as opposed to appending the whole object itself).
 */
@SuppressWarnings("serial")
public abstract class EmbeddingMarker extends LogstashMarker {
    
    public static final String MARKER_NAME_PREFIX = LogstashMarker.MARKER_NAME_PREFIX + "EMBED_";
    
    public EmbeddingMarker(String markerName) {
        super(markerName);
    }
}
