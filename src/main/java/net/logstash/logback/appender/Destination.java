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
package net.logstash.logback.appender;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * An immutable representation of a destination to which to attempt to send logs.
 */
public class Destination {
    
    private final String host;
    private final int port;

    public Destination(String host, int port) {
        if (port <= 0) {
            throw new IllegalArgumentException("Invalid destination '" + host + ":" + port + "': port must be greater than 0 (was " + port + ").");
        }
        this.host = host;
        this.port = port;
    }
    
    public String getHost() {
        return host;
    }
    
    public int getPort() {
        return port;
    }
    
    public String toString() {
        return host + ":" + port;
    }
    
    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (object.getClass() != this.getClass()) {
            return false;
        }
        Destination that = (Destination) object;
        
        return new EqualsBuilder()
            .append(this.host, that.host)
            .append(this.host, that.host)
            .isEquals();
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.host)
            .append(this.port)
            .toHashCode();
    }
}