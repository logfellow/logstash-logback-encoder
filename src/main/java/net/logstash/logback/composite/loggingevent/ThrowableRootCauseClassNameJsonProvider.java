/*
 * Copyright 2013-2022 the original author or authors.
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
package net.logstash.logback.composite.loggingevent;

import ch.qos.logback.classic.spi.IThrowableProxy;

/**
 * Logs the class name of the innermost cause of the throwable associated with a
 * given logging event, if any. The root cause may be the throwable itself, if
 * it has no cause.
 */
public class ThrowableRootCauseClassNameJsonProvider extends AbstractThrowableClassNameJsonProvider {
    static final String FIELD_NAME = "throwable_root_cause_class";

    public ThrowableRootCauseClassNameJsonProvider() {
        super(FIELD_NAME);
    }

    @Override
    IThrowableProxy getThrowable(IThrowableProxy throwable) {
        return throwable == null ? null : ThrowableSelectors.rootCause(throwable);
    }
}
