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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.net.InetSocketAddress;
import java.util.List;

import net.logstash.logback.appender.destination.DestinationParser;

import org.junit.jupiter.api.Test;

import ch.qos.logback.core.CoreConstants;

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
    public void testParse_Single_UndefinedProperty() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            DestinationParser.parse("propertyName" + CoreConstants.UNDEFINED_PROPERTY_SUFFIX, 1);
        });
    }

    @Test
    public void testParse_Multiple() {
        List<InetSocketAddress> destinations = DestinationParser.parse(" localhost:2, localhost, localhost : 5 ", 1);
        
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
    public void testParse_Multiple_Empty() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            DestinationParser.parse("localhost:1000, , localhost:1001", 1);
        });
    }
}
