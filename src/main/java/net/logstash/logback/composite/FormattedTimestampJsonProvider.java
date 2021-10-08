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
package net.logstash.logback.composite;

import net.logstash.logback.fieldnames.LogstashCommonFieldNames;

import ch.qos.logback.core.spi.DeferredProcessingAware;

/**
 * @deprecated use {@link AbstractFormattedTimestampJsonProvider} instead
 */
@Deprecated
public abstract class FormattedTimestampJsonProvider<Event extends DeferredProcessingAware, FieldNames extends LogstashCommonFieldNames> extends AbstractFormattedTimestampJsonProvider<Event, FieldNames> {

}
