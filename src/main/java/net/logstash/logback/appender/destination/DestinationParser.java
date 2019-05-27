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
package net.logstash.logback.appender.destination;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.qos.logback.core.CoreConstants;

/**
 * Constructs {@link InetSocketAddress}es by parsing {@link String} values.
 */
public class DestinationParser {
    
    private static final Pattern DESTINATION_PATTERN = Pattern.compile("^\\s*(\\S+?)\\s*(:\\s*(\\S+)\\s*)?$");
    private static final int HOSTNAME_GROUP = 1;
    private static final int PORT_GROUP = 3;
    
    /**
     * Constructs {@link InetSocketAddress}es by parsing the given {@link String} value.
     * <p>
     * The string is a comma separated list of destinations in the form of hostName[:portNumber].
     * <p>
     * 
     * For example, "host1.domain.com,host2.domain.com:5560"
     * <p>
     * 
     * If portNumber is not provided, then the given defaultPort will be used.
     */
    public static List<InetSocketAddress> parse(String destinations, int defaultPort) {
        
        /*
         * Multiple destinations can be specified on one single line, separated by comma
         */
        String[] destinationStrings = (destinations == null ? "" : destinations.trim()).split("\\s*,\\s*");
        
        List<InetSocketAddress> destinationList = new ArrayList<InetSocketAddress>(destinationStrings.length);
        
        for (String entry: destinationStrings) {
            
            /*
             * For #134, check to ensure properties are defined when destinations
             * are set using properties. 
             */
            if (entry.contains(CoreConstants.UNDEFINED_PROPERTY_SUFFIX)) {
                throw new IllegalArgumentException("Invalid destination '" + entry + "': unparseable value (expected format 'host[:port]').");
            }
            
            Matcher matcher = DESTINATION_PATTERN.matcher(entry);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid destination '" + entry + "': unparseable value (expected format 'host[:port]').");
            }
            String host = matcher.group(HOSTNAME_GROUP);
            String portString = matcher.group(PORT_GROUP);
            
            int port;
            try {
                port = (portString != null)
                        ? Integer.parseInt(portString)
                        : defaultPort;
                
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid destination '" + entry + "': unparseable port (was '" + portString + "').");
            }
            
            destinationList.add(InetSocketAddress.createUnresolved(host, port));
        }
        
        return destinationList;
    }

}
