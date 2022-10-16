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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.net.InetSocketAddress;
import java.util.List;

import ch.qos.logback.core.CoreConstants;
import org.junit.jupiter.api.Test;

public class DestinationParserTest {
    
    @Test
    public void testParse_Single_WithPort() {
        List<InetSocketAddress> destinations = DestinationParser.parse(" localhost : 2 ", 1);
        
        assertThat(destinations).containsExactly(
                InetSocketAddress.createUnresolved("localhost", 2)
            );
    }

    @Test
    public void testParse_Single_DefaultPort() {
        List<InetSocketAddress> destinations = DestinationParser.parse(" localhost ", 1);
        
        assertThat(destinations).containsExactly(
                InetSocketAddress.createUnresolved("localhost", 1)
            );
    }

    @Test
    public void testParse_Single_Unresolvable() {
        List<InetSocketAddress> destinations = DestinationParser.parse("axfleoguasonteuhaodilaguoehsntuhas", 1);
        
        assertThat(destinations).containsExactly(
                InetSocketAddress.createUnresolved("axfleoguasonteuhaodilaguoehsntuhas", 1)
            );
    }

    @Test
    public void testParse_Single_AlphaPort() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            DestinationParser.parse("localhost:a", 1);
        });
    }
    
    @Test
    public void testParse_Single_NegativePort() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            DestinationParser.parse("localhost:-1", 1);
        });
    }

    @Test
    public void testParse_Single_MissingPortAfterColon() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            DestinationParser.parse(" localhost: ", 1);
        });
    }
    
    @Test
    public void testParse_Single_UndefinedProperty() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            DestinationParser.parse("propertyName" + CoreConstants.UNDEFINED_PROPERTY_SUFFIX, 1);
        });
    }

    @Test
    public void testParse_Multiple() {
        List<InetSocketAddress> destinations = DestinationParser.parse(" localhost:2, localhost, , localhost : 5 ", 1);
        
        assertThat(destinations).containsExactly(
                InetSocketAddress.createUnresolved("localhost", 2),
                InetSocketAddress.createUnresolved("localhost", 1),
                InetSocketAddress.createUnresolved("localhost", 5)
            );
    }

    @Test
    public void testParse_Multiple_AlphaPort() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            DestinationParser.parse("localhost:10000, localhost:a", 1);
        });
    }
    
    @Test
    public void testParse_Multiple_NegativePort() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            DestinationParser.parse("localhost:10000, localhost:-1", 1);
        });
    }
    
    @Test
    public void testIPv4_WithPort() {
        List<InetSocketAddress> destinations = DestinationParser.parse(" 192.168.1.1:8080 ", 1);

        assertThat(destinations).containsExactly(
                InetSocketAddress.createUnresolved("192.168.1.1", 8080)
            );
    }

    @Test
    public void testIPv4_DefaultPort() {
        List<InetSocketAddress> destinations = DestinationParser.parse(" 192.168.1.1 ", 1);

        assertThat(destinations).containsExactly(
                InetSocketAddress.createUnresolved("192.168.1.1", 1)
            );
    }
    
    @Test
    public void testIPv6_WithPort() {
        List<InetSocketAddress> destinations = DestinationParser.parse(" [2001:db8::1]:8080 ", 1);

        assertThat(destinations).containsExactly(
                InetSocketAddress.createUnresolved("2001:db8::1", 8080)
            );
    }

    @Test
    public void testIPv6_DefaultPort() {
        List<InetSocketAddress> destinations = DestinationParser.parse(" [2001:db8::1] ", 1);

        assertThat(destinations).containsExactly(
                InetSocketAddress.createUnresolved("2001:db8::1", 1)
            );
    }
}
