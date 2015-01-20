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
package net.logstash.logback.composite;

import java.io.IOException;

import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.spi.LifeCycle;

import com.fasterxml.jackson.core.JsonGenerator;

public interface JsonProvider<Event extends DeferredProcessingAware> extends LifeCycle, ContextAware {
    
    void writeTo(JsonGenerator generator, Event event) throws IOException;
    
    void prepareForDeferredProcessing(Event event);
    
}
