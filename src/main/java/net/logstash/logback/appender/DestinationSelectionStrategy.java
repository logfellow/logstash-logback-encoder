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
package net.logstash.logback.appender;

import org.apache.commons.lang.StringUtils;

/**
 * DestinationSelectionStrategy
 *
 * @author withccm@gmail.com
 * @since 2017. 03. 21.
 */
public enum DestinationSelectionStrategy {
	/**
	 * PerferPrimary works in a primary-secondary. This is the default setting. Connect the server to the primary first.
	 *
	 * RoundRobin schedules by time. Destinations is circulated if round-robin occurs.
	 * When creating an object, you can assign to roundRobinConnectionTTL.
	 *
	 * The Random attribute is associated with any server when it is first connected.
	 * If the connection is lost, it is connected to the next server.
	 */
	PreferPrimary, RoundRobin, Random;

	public static DestinationSelectionStrategy findFromString(String strategy) {
		for(DestinationSelectionStrategy each : values()) {
			if ((StringUtils.isNotBlank(strategy)) && each.toString().equalsIgnoreCase(strategy)) {
				return each;
			}
		}
		return PreferPrimary;
	}
}
