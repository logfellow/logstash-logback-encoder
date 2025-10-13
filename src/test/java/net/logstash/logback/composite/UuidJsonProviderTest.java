/*
 * Copyright 2013-2025 the original author or authors.
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
package net.logstash.logback.composite;

import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JsonGenerator;

@ExtendWith(MockitoExtension.class)
public class UuidJsonProviderTest {
    public static final String UUID = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$";

    private final UuidJsonProvider<ILoggingEvent> provider = new UuidJsonProvider<>();

    @Mock
    private JsonGenerator generator;

    @Mock
    private ILoggingEvent event;

    @Test
    public void testDefaultName() {
        provider.writeTo(generator, event);

        verify(generator).writeName(UuidJsonProvider.FIELD_UUID);
        verify(generator).writeString(matches(UUID));
    }

    @Test
    public void testFieldName() {
        provider.setFieldName("newFieldName");

        provider.writeTo(generator, event);

        verify(generator).writeName("newFieldName");
        verify(generator).writeString(matches(UUID));
    }

    @Test
    public void testStrategy() {
        provider.setStrategy(UuidJsonProvider.STRATEGY_TIME);

        provider.writeTo(generator, event);

        verify(generator).writeName("uuid");
        verify(generator).writeString(matches(UUID));
    }

    @Test
    public void testEthernet() {
        provider.setStrategy(UuidJsonProvider.STRATEGY_TIME);
        provider.setEthernet("00:C0:F0:3D:5B:7C");

        provider.writeTo(generator, event);

        verify(generator).writeName("uuid");
        verify(generator).writeString(matches(UUID));
    }
}
