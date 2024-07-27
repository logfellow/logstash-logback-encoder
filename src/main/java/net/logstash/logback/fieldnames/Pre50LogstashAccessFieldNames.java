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
package net.logstash.logback.fieldnames;

import ch.qos.logback.access.common.spi.AccessEvent;


/**
 * These are the default JSON field names that were used to output {@link AccessEvent} details
 * in logstash-logback-encoder versions prior to 5.0.
 * This class exists mainly to provide an easy way to revert to the pre-5.0 defaults.
 *
 * After logstash-logback-encoder 5.0, the defaults are specified in {@link LogstashAccessFieldNames}.
 */
public class Pre50LogstashAccessFieldNames extends LogstashAccessFieldNames {
    public Pre50LogstashAccessFieldNames() {
        setMessage("@message");
        setFieldsMethod("@fields.method");
        setFieldsProtocol("@fields.protocol");
        setFieldsStatusCode("@fields.status_code");
        setFieldsRequestedUrl("@fields.requested_url");
        setFieldsRequestedUri("@fields.requested_uri");
        setFieldsRemoteHost("@fields.remote_host");
        setFieldsRemoteUser("@fields.remote_user");
        setFieldsContentLength("@fields.content_length");
        setFieldsElapsedTime("@fields.elapsed_time");

    }

    public String getFieldsMethod() {
        return getMethod();
    }

    public void setFieldsMethod(String fieldsMethod) {
        setMethod(fieldsMethod);
    }

    public String getFieldsProtocol() {
        return getProtocol();
    }

    public void setFieldsProtocol(String fieldsProtocol) {
        setProtocol(fieldsProtocol);
    }

    public String getFieldsStatusCode() {
        return getStatusCode();
    }

    public void setFieldsStatusCode(String fieldsStatusCode) {
        setStatusCode(fieldsStatusCode);
    }

    public String getFieldsRequestedUrl() {
        return getRequestedUrl();
    }

    public void setFieldsRequestedUrl(String fieldsRequestedUrl) {
        setRequestedUrl(fieldsRequestedUrl);
    }

    public String getFieldsRequestedUri() {
        return getRequestedUri();
    }

    public void setFieldsRequestedUri(String fieldsRequestedUri) {
        setRequestedUri(fieldsRequestedUri);
    }

    public String getFieldsRemoteHost() {
        return getRemoteHost();
    }

    public void setFieldsRemoteHost(String fieldsRemoteHost) {
        setRemoteHost(fieldsRemoteHost);
    }

    public String getFieldsRemoteUser() {
        return getRemoteUser();
    }

    public void setFieldsRemoteUser(String fieldsRemoteUser) {
        setRemoteUser(fieldsRemoteUser);
    }

    public String getFieldsContentLength() {
        return getContentLength();
    }

    public void setFieldsContentLength(String fieldsContentLength) {
        setContentLength(fieldsContentLength);
    }

    public String getFieldsElapsedTime() {
        return getElapsedTime();
    }

    public void setFieldsElapsedTime(String fieldsElapsedTime) {
        setElapsedTime(fieldsElapsedTime);
    }

    public String getFieldsRequestHeaders() {
        return getRequestHeaders();
    }

    public void setFieldsRequestHeaders(String fieldsRequestHeaders) {
        setRequestHeaders(fieldsRequestHeaders);
    }

    public String getFieldsResponseHeaders() {
        return getResponseHeaders();
    }

    public void setFieldsResponseHeaders(String fieldsResponseHeaders) {
        setResponseHeaders(fieldsResponseHeaders);
    }
}
