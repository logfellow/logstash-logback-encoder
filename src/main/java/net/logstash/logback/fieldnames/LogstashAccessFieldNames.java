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

import net.logstash.logback.composite.accessevent.AccessMessageJsonProvider;
import net.logstash.logback.composite.accessevent.ContentLengthJsonProvider;
import net.logstash.logback.composite.accessevent.ElapsedTimeJsonProvider;
import net.logstash.logback.composite.accessevent.MethodJsonProvider;
import net.logstash.logback.composite.accessevent.ProtocolJsonProvider;
import net.logstash.logback.composite.accessevent.RemoteHostJsonProvider;
import net.logstash.logback.composite.accessevent.RemoteUserJsonProvider;
import net.logstash.logback.composite.accessevent.RequestedUriJsonProvider;
import net.logstash.logback.composite.accessevent.RequestedUrlJsonProvider;
import net.logstash.logback.composite.accessevent.StatusCodeJsonProvider;

public class LogstashAccessFieldNames extends LogstashCommonFieldNames {
    
    private String method = MethodJsonProvider.FIELD_METHOD;
    private String protocol = ProtocolJsonProvider.FIELD_PROTOCOL;
    private String statusCode = StatusCodeJsonProvider.FIELD_STATUS_CODE;
    private String requestedUrl = RequestedUrlJsonProvider.FIELD_REQUESTED_URL;
    private String requestedUri = RequestedUriJsonProvider.FIELD_REQUESTED_URI;
    private String remoteHost = RemoteHostJsonProvider.FIELD_REMOTE_HOST;
    private String remoteUser = RemoteUserJsonProvider.FIELD_REMOTE_USER;
    private String contentLength = ContentLengthJsonProvider.FIELD_CONTENT_LENGTH;
    private String elapsedTime = ElapsedTimeJsonProvider.FIELD_ELAPSED_TIME;
    /*
     * By default:
     * requestHeaders and responseHeaders are ignored
     * because those fields can be quite big.
     */
    private String requestHeaders;
    private String responseHeaders;
    
    public LogstashAccessFieldNames() {
        setMessage(AccessMessageJsonProvider.FIELD_MESSAGE);
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getRequestedUrl() {
        return requestedUrl;
    }

    public void setRequestedUrl(String requestedUrl) {
        this.requestedUrl = requestedUrl;
    }

    public String getRequestedUri() {
        return requestedUri;
    }

    public void setRequestedUri(String requestedUri) {
        this.requestedUri = requestedUri;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public String getRemoteUser() {
        return remoteUser;
    }

    public void setRemoteUser(String remoteUser) {
        this.remoteUser = remoteUser;
    }

    public String getContentLength() {
        return contentLength;
    }

    public void setContentLength(String contentLength) {
        this.contentLength = contentLength;
    }

    public String getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(String elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public String getRequestHeaders() {
      return requestHeaders;
    }

    public void setRequestHeaders(String requestHeaders) {
      this.requestHeaders = requestHeaders;
    }

    public String getResponseHeaders() {
      return responseHeaders;
    }

    public void setResponseHeaders(String responseHeaders) {
      this.responseHeaders = responseHeaders;
    }
}
