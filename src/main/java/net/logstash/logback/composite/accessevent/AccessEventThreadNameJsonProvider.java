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
package net.logstash.logback.composite.accessevent;

import net.logstash.logback.composite.AbstractThreadNameJsonProvider;
import net.logstash.logback.composite.JsonProvider;

import ch.qos.logback.access.common.spi.IAccessEvent;

/**
 * {@link JsonProvider} producing a single JSON field with the {@link IAccessEvent#getThreadName()}.
 * 
 * @author brenuart
 */
public class AccessEventThreadNameJsonProvider extends AbstractThreadNameJsonProvider<IAccessEvent> {

    @Override
    protected String getThreadName(IAccessEvent event) {
        return event.getThreadName();
    }
}
