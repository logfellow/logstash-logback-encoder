/*
 * Copyright 2013-2023 the original author or authors.
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
package net.logstash.logback.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;

public abstract class LogbackUtils {

    private LogbackUtils() {
        // utility class
    }

    public static void start(Object component) {
        if (component instanceof LifeCycle) {
            ((LifeCycle) component).start();
        }
    }
    
    public static void stop(Object component) {
        if (component instanceof LifeCycle) {
            ((LifeCycle) component).stop();
        }
    }
    
    public static void setContext(Context context, Object component) {
        if (component instanceof ContextAware) {
            ((ContextAware) component).setContext(context);
        }
    }
    
    public static void start(Context context, Object component) {
        setContext(context, component);
        start(component);
    }
}
