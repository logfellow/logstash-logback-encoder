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

import ch.qos.logback.classic.spi.IThrowableProxy;

public class ThrowableRootCauseClassNameJsonProvider extends AbstractThrowableClassNameJsonProvider {
    static final String FIELD_NAME = "throwable_root_cause_class";

    public ThrowableRootCauseClassNameJsonProvider() {
        super(FIELD_NAME);
    }

    @Override
    IThrowableProxy getThrowable(IThrowableProxy throwable) {
        return getCause(throwable);
    }

    /**
     * @return given throwable if t does not contain any cause; null if given throwable is null
     */
    private static IThrowableProxy getCause(IThrowableProxy t) {
        return (t != null && t.getCause() != null) ? getCause(t.getCause()) : t;
    }
}
