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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import net.logstash.logback.pattern.AbstractJsonPatternParser;
import net.logstash.logback.pattern.AbstractJsonPatternParser.JsonPatternException;
import net.logstash.logback.pattern.NodeWriter;
import net.logstash.logback.test.AbstractLogbackTest;

import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.status.Status;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;


public class AbstractPatternJsonProviderTest extends AbstractLogbackTest {

    @Mock
    private DeferredProcessingAware event;

    @Mock
    private JsonFactory jsonFactory;
    
    @Mock
    private AbstractJsonPatternParser<DeferredProcessingAware> parser;

    private AbstractPatternJsonProvider<DeferredProcessingAware> provider;

    
    @BeforeEach
    public void setup() throws Exception {
        super.setup();
        
        provider = new AbstractPatternJsonProvider<DeferredProcessingAware>() {
            @Override
            protected AbstractJsonPatternParser<DeferredProcessingAware> createParser(JsonFactory jsonFactory) {
                return parser;
            }
        };
        provider.setContext(context);
        provider.setJsonFactory(jsonFactory);
    }

    
    /*
     * Verify that AbstractPatternJsonProvider delegates all the work to Parser.
     */
    @Test
    public void shouldDelegateToParser() throws Exception {
        /*
         * Configure the Parser to returned a mocked NodeWriter
         */
        @SuppressWarnings("unchecked")
        NodeWriter<DeferredProcessingAware> nodeWriter = mock(NodeWriter.class);
        doReturn(nodeWriter).when(this.parser).parse(anyString());
        
        JsonGenerator generator = mock(JsonGenerator.class);
        
        /*
         * Configure and start the provider
         */
        final String pattern = "does not matter"; // pattern does not actually matter since we mocked the Parser...
        provider.setPattern(pattern);
        provider.setOmitEmptyFields(true);
        provider.start();

        // should actually invoke parser with the pattern requested
        verify(parser).setOmitEmptyFields(true);
        verify(parser).parse(pattern);
        
        provider.writeTo(generator, event);
        
        // and the end result should be what NodeWriter returned by the parser produces
        verify(nodeWriter).write(generator, event);
    }
    
    
    /*
     * Test behavior when provider is configured with an invalid pattern.
     * Expected:
     * - logs an ERROR
     * - does not output anything at runtime
     */
    @Test
    public void invalidConfiguration() throws Exception {
        /*
         * Make the Parser throw a JsonPatternException to simulate an invalid pattern
         */
        doThrow(new JsonPatternException("Invalid pattern")).when(parser).parse(anyString());
        
        /*
         * Configure and start the provider
         */
        final String pattern = "does not matter"; // pattern does not actually matter since we mocked the Parser...
        provider.setPattern(pattern);
        provider.start();
        
        // Provider is started even when pattern is invalid
        assertThat(provider.isStarted()).isTrue();
        
        // An ERROR status describing the problem is emitted
        assertThat(statusManager.getCopyOfStatusList()).anyMatch(status ->
                   status.getLevel() == Status.ERROR
                && status.getMessage().startsWith("Invalid [pattern]")
                && status.getOrigin() == provider);

        // Provider output "nothing" after a configuration error
        verifyNoInteractions(jsonFactory);
    }
}
