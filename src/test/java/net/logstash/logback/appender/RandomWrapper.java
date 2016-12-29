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

import java.util.Random;

/**
 * RandomWrapper
 *
 * @author se.hyung@navercorp.com
 * @since 2016. 12. 22.
 */
public class RandomWrapper extends Random {
	int seq = 0;
	public RandomWrapper() {
		super();
	}

	@Override
	public int nextInt() {
		return seq++;
	}

	@Override
	public int nextInt(int bound) {
		int result = seq;
		if (result < bound) {
			seq++;
		} else {
			result = 0;
			seq = 1;
		}
		return result;
	}
}
