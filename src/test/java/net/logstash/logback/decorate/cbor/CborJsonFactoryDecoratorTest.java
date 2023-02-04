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
package net.logstash.logback.decorate.cbor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import net.logstash.logback.composite.loggingevent.LoggingEventCompositeJsonFormatter;
import net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders;
import net.logstash.logback.composite.loggingevent.MessageJsonProvider;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.ContextAware;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CborJsonFactoryDecoratorTest {

    @Mock
    private ContextAware contextAware;

    @Mock
    private ILoggingEvent event;

    @Test
    void test() throws IOException {
        CborJsonFactoryDecorator decorator = new CborJsonFactoryDecorator();

        LoggingEventJsonProviders providers = new LoggingEventJsonProviders();
        providers.addMessage(new MessageJsonProvider());

        LoggingEventCompositeJsonFormatter formatter = new LoggingEventCompositeJsonFormatter(contextAware);
        formatter.setProviders(providers);
        formatter.setJsonFactoryDecorator(decorator);
        formatter.start();

        when(event.getFormattedMessage()).thenReturn("a message");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        formatter.writeEvent(event, baos);

        assertThat(baos.toByteArray()).asBase64Encoded().isEqualTo("v2dtZXNzYWdlaWEgbWVzc2FnZf8=");
    }
}
