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

import net.logstash.logback.composite.JsonProviders;

import ch.qos.logback.access.common.spi.IAccessEvent;

/**
 * Used to make it make it more convenient to create well-known
 * {@link JsonProviders} via xml configuration.
 * <p>
 * For example, instead of:
 * {@code
 *     <provider class="net.logstash.logback.composite.accessevent.AccessEventFormattedTimestampJsonProvider"/>
 * }
 * you can just use:
 * {@code
 *     <timestamp/>
 * }
 */
public class AccessEventJsonProviders extends JsonProviders<IAccessEvent> {

    public void addTimestamp(AccessEventFormattedTimestampJsonProvider provider) {
        addProvider(provider);
    }
    
    /**
     * @deprecated Use {@link #addMessage(AccessMessageJsonProvider)} instead.
     * @param provider the provider to add
     */
    @Deprecated
    public void addAccessMessage(AccessMessageJsonProvider provider) {
        addProvider(provider);
    }
    public void addMessage(AccessMessageJsonProvider provider) {
        addProvider(provider);
    }
    
    public void addMethod(MethodJsonProvider provider) {
        addProvider(provider);
    }
    public void addProtocol(ProtocolJsonProvider provider) {
        addProvider(provider);
    }
    public void addStatusCode(StatusCodeJsonProvider provider) {
        addProvider(provider);
    }
    public void addRequestedUrl(RequestedUrlJsonProvider provider) {
        addProvider(provider);
    }
    public void addRequestedUri(RequestedUriJsonProvider provider) {
        addProvider(provider);
    }
    public void addRemoteHost(RemoteHostJsonProvider provider) {
        addProvider(provider);
    }
    public void addRemoteUser(RemoteUserJsonProvider provider) {
        addProvider(provider);
    }
    public void addContentLength(ContentLengthJsonProvider provider) {
        addProvider(provider);
    }
    public void addElapsedTime(ElapsedTimeJsonProvider provider) {
        addProvider(provider);
    }
    public void addRequestHeaders(RequestHeadersJsonProvider provider) {
        addProvider(provider);
    }
    public void addResponseHeaders(ResponseHeadersJsonProvider provider) {
        addProvider(provider);
    }
    public void addPattern(AccessEventPatternJsonProvider provider) {
        addProvider(provider);
    }
    public void addNestedField(AccessEventNestedJsonProvider provider) {
        addProvider(provider);
    }
    public void addThreadName(AccessEventThreadNameJsonProvider provider) {
        addProvider(provider);
    }
    public void addSequence(SequenceJsonProvider provider) {
        addProvider(provider);
    }
}
