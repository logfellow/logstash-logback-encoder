/*
 * Copyright 2013-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.io.StringWriter;

import net.logstash.logback.test.AbstractLogbackTest;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.status.Status;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class GlobalCustomFieldsJsonProviderTest extends AbstractLogbackTest {
    
    @InjectMocks
    private GlobalCustomFieldsJsonProvider<ILoggingEvent> provider = new GlobalCustomFieldsJsonProvider<ILoggingEvent>();
    
    @Mock
    private ILoggingEvent event;
    
    private StringWriter writer = new StringWriter();
    private JsonGenerator generator;
    
    
    @BeforeEach
    public void setup() throws Exception {
        super.setup();
              
        JsonFactory factory = new MappingJsonFactory();
        provider.setJsonFactory(factory);
        generator = factory.createGenerator(writer);
    }
    
    @AfterEach
    public void tearDown() {
        provider.stop();
        super.tearDown();
    }
    
    
    @Test
    public void test() throws IOException {
        provider.setCustomFields("{\"name\":\"value\"}");
        provider.start();
        
        generator.writeStartObject();
        provider.writeTo(generator, event);
        generator.writeEndObject();
        generator.flush();
        
        assertThat(writer).hasToString("{\"name\":\"value\"}");
    }

    @Test
    public void invalidCustomFields_notAnObject() {
        provider.setCustomFields("\"aNonObjectValue\"");
        provider.start();
        
        assertThat(statusManager.getCopyOfStatusList())
            .hasSize(1)
            .allMatch(s -> s.getLevel() == Status.ERROR && s.getOrigin() == provider);
        assertThat(provider.isStarted())
            .isTrue();
    }
    
    @Test
    public void invalidCustomFields_trailingChars() {
        provider.setCustomFields("{} trailingChars");
        provider.start();
        
        assertThat(statusManager.getCopyOfStatusList())
            .hasSize(1)
            .allMatch(s -> s.getLevel() == Status.ERROR && s.getOrigin() == provider);
        assertThat(provider.isStarted())
            .isTrue();
    }
    
    @Test
    @SuppressWarnings("deprecation")
    public void customFieldsNodeIsNotAnObject() {
        NumericNode node = JsonNodeFactory.instance.numberNode(0);
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> provider.setCustomFieldsNode(node));
    }
}
