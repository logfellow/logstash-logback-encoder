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
package net.logstash.logback.composite.loggingevent;

import static org.mockito.Mockito.when;

import java.io.IOException;

import net.logstash.logback.argument.StructuredArguments;
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ch.qos.logback.classic.spi.ILoggingEvent;

@RunWith(MockitoJUnitRunner.class)
public class LoggingEventCompositeJsonFormatterTest {
    
    private LoggingEventCompositeJsonFormatter formatter = new LoggingEventCompositeJsonFormatter(new LoggingEventCompositeJsonEncoder());
    
    @Mock
    private ILoggingEvent event;

    public static class EmptyBean {
        
    }

    @Test
    public void testDoesNotFailOnEmptyBeans() throws IOException {
        when(event.getArgumentArray()).thenReturn(new Object[] {StructuredArguments.keyValue("empty", new EmptyBean())});
        ((LoggingEventJsonProviders) formatter.getProviders()).addArguments(new ArgumentsJsonProvider());
        formatter.start();
        
        /*
         * This should not throw an exception, since SerializationFeature.FAIL_ON_EMPTY_BEANS is disabled
         */
        formatter.writeEventAsString(event);
    }

}
