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
package net.logstash.logback.argument;

/**
 * Allow to associate a key to an argument value.
 * Thus, argument can be outputted as json object or formatted as string.
 */
public interface NamedArgument {

    String getKey();

    Object getValue();

    /**
     * This method must format the {@link #getValue()} as a {@link String}. <br/>
     * It should follow at least the slf4j behaviour. See {@link NamedArguments#toString(Object)}.
     * @return
     */
    String toString();
}
