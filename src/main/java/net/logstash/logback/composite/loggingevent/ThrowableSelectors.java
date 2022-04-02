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
package net.logstash.logback.composite.loggingevent;

import ch.qos.logback.classic.spi.IThrowableProxy;

/**
 * Utilities to obtain {@code Throwables} from {@code IThrowableProxies}.
 */
public class ThrowableSelectors {

    /**
     * Returns the innermost cause of {@code throwable}.
     *
     * @param throwable the throwable for which to find the root cause
     * @return the innermost cause, which may be {@code throwable} itself if there
     *         is no cause, or {@code null} if there is a loop in the causal chain.
     *
     * @throws NullPointerException if {@code throwable} is {@code null}
     */
    public static IThrowableProxy rootCause(IThrowableProxy throwable) {
        // Keep a second pointer that slowly walks the causal chain.
        // If the fast pointer ever catches the slower pointer, then there's a loop.
        IThrowableProxy slowPointer = throwable;
        boolean advanceSlowPointer = false;

        IThrowableProxy cause;
        while ((cause = throwable.getCause()) != null) {
            throwable = cause;

            if (throwable == slowPointer) {
                // There's a cyclic reference, so no real root cause.
                return null;
            }

            if (advanceSlowPointer) {
                slowPointer = slowPointer.getCause();
            }

            advanceSlowPointer = !advanceSlowPointer; // only advance every other iteration
        }

        return throwable;
    }

}
