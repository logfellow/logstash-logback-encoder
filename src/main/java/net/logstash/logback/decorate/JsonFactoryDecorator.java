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
package net.logstash.logback.decorate;

import net.logstash.logback.LogstashAccessFormatter;
import net.logstash.logback.LogstashFormatter;

import com.fasterxml.jackson.databind.MappingJsonFactory;

/**
 * Decorates the {@link MappingJsonFactory} used by the
 * {@link LogstashFormatter}
 * or {@link LogstashAccessFormatter}.
 * <p>
 * This allows you to customize the factory used by the formatters.
 * <p>
 * Implementations must be idempotent.
 * The decorator configured on a formatter is called each time a formatter is started,
 * and there is no way to 'un-decorate' the factory when when formatter is stopped.
 * So, the factory could be decorated multiple times if the formatter is restarted.
 */
public interface JsonFactoryDecorator {
    
    MappingJsonFactory decorate(MappingJsonFactory factory);

}
