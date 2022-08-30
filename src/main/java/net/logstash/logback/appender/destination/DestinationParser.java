/*
 * Copyright 2013-2022 the original author or authors.
 *
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

    private static final Pattern DESTINATION_PATTERN = Pattern.compile("^([^:]+)(:(.+))?$");
    private static final Pattern DESTINATION_IPV6_PATTERN = Pattern.compile("^\\[(.+)\\](:(.+))?$");

    private static final int HOSTNAME_GROUP = 1;
    private static final int PORT_GROUP = 3;

    private DestinationParser() {
        // utility class
    }
    
    /**
     * Constructs {@link InetSocketAddress}es by parsing the given {@link String} value.
     * <p>
     * The string is a comma separated list of destinations in the form of hostName[:portNumber] where:
     * <ul>
     * <li>{@code hostName} can be a hostname (eg. <i>localhost</i>), an IPv4 (eg. <i>192.168.1.1</i>) or
     *     an IPv6 enclosed between brackets (eg. <i>[2001:db8::1]</i>)
     *
     * <li>{@code portNumber} is optional and, if specified, must be prefixed by a colon. Must be a valid
     *     integer between 0 and 65535. If {@code portNumber} is not provided, then the given {@code defaultPort}
     *     will be used.
     * </ul>
     * <p>
     *
     * For example, "host1.domain.com,host2.domain.com:5560"
     * <p>
     *
     * @param destinations comma-separated list of destinations in the form of {@code hostName[:portNumber]}
     * @param defaultPort the port number to use when a destination does not specify one explicitly
     * @return ordered list of {@link InetSocketAddress} instances
     */
    public static List<InetSocketAddress> parse(String destinations, int defaultPort) {

        /*
         * Multiple destinations can be specified on one single line, separated by comma
         */
        String[] destinationStrings = (destinations == null ? "" : destinations.replace(" ", "")).split(",");
        
        List<InetSocketAddress> destinationList = new ArrayList<>(destinationStrings.length);

        for (String entry: destinationStrings) {

            /*
             * For #134, check to ensure properties are defined when destinations
             * are set using properties.
             */
            if (entry.contains(CoreConstants.UNDEFINED_PROPERTY_SUFFIX)) {
                throw new IllegalArgumentException("Invalid destination '" + entry + "': unparseable value (expected format 'host[:port]').");
            }

            Matcher matcher = DESTINATION_IPV6_PATTERN.matcher(entry);
            if (!matcher.matches()) {
                matcher = DESTINATION_PATTERN.matcher(entry);
            }
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
