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

import java.io.IOException;
import java.util.Map;

import net.logstash.logback.composite.AbstractJsonProvider;
import net.logstash.logback.composite.JsonWritingUtils;
import ch.qos.logback.classic.spi.ILoggingEvent;

import com.fasterxml.jackson.core.JsonGenerator;

@Deprecated
public class ContextMapJsonProvider extends AbstractJsonProvider<ILoggingEvent> {

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        Object[] args = event.getArgumentArray();
        if (args != null && args.length > 0 && args[args.length - 1] instanceof Map) {
            Map<?, ?> contextMap = (Map<?, ?>) args[args.length - 1];
            JsonWritingUtils.writeMapEntries(generator, contextMap);
        }
    }

}
