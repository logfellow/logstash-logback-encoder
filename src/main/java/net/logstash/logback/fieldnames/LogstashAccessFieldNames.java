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
package net.logstash.logback.fieldnames;

import net.logstash.logback.composite.accessevent.AccessMessageJsonProvider;
import net.logstash.logback.composite.accessevent.ContentLengthJsonProvider;
import net.logstash.logback.composite.accessevent.ElapsedTimeJsonProvider;
import net.logstash.logback.composite.accessevent.HostnameJsonProvider;
import net.logstash.logback.composite.accessevent.MethodJsonProvider;
import net.logstash.logback.composite.accessevent.ProtocolJsonProvider;
import net.logstash.logback.composite.accessevent.RemoteHostJsonProvider;
import net.logstash.logback.composite.accessevent.RemoteUserJsonProvider;
import net.logstash.logback.composite.accessevent.RequestedUriJsonProvider;
import net.logstash.logback.composite.accessevent.RequestedUrlJsonProvider;
import net.logstash.logback.composite.accessevent.StatusCodeJsonProvider;

public class LogstashAccessFieldNames extends LogstashCommonFieldNames {
    
    private String fieldsMethod = MethodJsonProvider.FIELD_METHOD;
    private String fieldsProtocol = ProtocolJsonProvider.FIELD_PROTOCOL;
    private String fieldsStatusCode = StatusCodeJsonProvider.FIELD_STATUS_CODE;
    private String fieldsRequestedUrl = RequestedUrlJsonProvider.FIELD_REQUESTED_URL;
    private String fieldsRequestedUri = RequestedUriJsonProvider.FIELD_REQUESTED_URI;
    private String fieldsRemoteHost = RemoteHostJsonProvider.FIELD_REMOTE_HOST;
    private String fieldsHostname = HostnameJsonProvider.FIELD_HOSTNAME;
    private String fieldsRemoteUser = RemoteUserJsonProvider.FIELD_REMOTE_USER;
    private String fieldsContentLength = ContentLengthJsonProvider.FIELD_CONTENT_LENGTH;
    private String fieldsElapsedTime = ElapsedTimeJsonProvider.FIELD_ELAPSED_TIME;
    /*
     * By default:
     * fieldsRequestHeaders and fieldsResponseHeaders are ignored
     * because those fields can be quite big.
     */
    private String fieldsRequestHeaders;
    private String fieldsResponseHeaders;
    
    public LogstashAccessFieldNames() {
        /*
         * By default:
         * LogstashAccessEncoder uses '@message' for the message field.
         * LogstashEncoder uses 'message'.
         */
        setMessage(AccessMessageJsonProvider.FIELD_MESSAGE);
    }

    public String getFieldsMethod() {
        return fieldsMethod;
    }

    public void setFieldsMethod(String fieldsMethod) {
        this.fieldsMethod = fieldsMethod;
    }

    public String getFieldsProtocol() {
        return fieldsProtocol;
    }

    public void setFieldsProtocol(String fieldsProtocol) {
        this.fieldsProtocol = fieldsProtocol;
    }

    public String getFieldsStatusCode() {
        return fieldsStatusCode;
    }

    public void setFieldsStatusCode(String fieldsStatusCode) {
        this.fieldsStatusCode = fieldsStatusCode;
    }

    public String getFieldsRequestedUrl() {
        return fieldsRequestedUrl;
    }

    public void setFieldsRequestedUrl(String fieldsRequestedUrl) {
        this.fieldsRequestedUrl = fieldsRequestedUrl;
    }

    public String getFieldsRequestedUri() {
        return fieldsRequestedUri;
    }

    public void setFieldsRequestedUri(String fieldsRequestedUri) {
        this.fieldsRequestedUri = fieldsRequestedUri;
    }

    public String getFieldsRemoteHost() {
        return fieldsRemoteHost;
    }

    public void setFieldsRemoteHost(String fieldsRemoteHost) {
        this.fieldsRemoteHost = fieldsRemoteHost;
    }

    public String getFieldsHostname() {
        return fieldsHostname;
    }

    public void setFieldsHostname(String fieldsHostname) {
        this.fieldsHostname = fieldsHostname;
    }

    public String getFieldsRemoteUser() {
        return fieldsRemoteUser;
    }

    public void setFieldsRemoteUser(String fieldsRemoteUser) {
        this.fieldsRemoteUser = fieldsRemoteUser;
    }

    public String getFieldsContentLength() {
        return fieldsContentLength;
    }

    public void setFieldsContentLength(String fieldsContentLength) {
        this.fieldsContentLength = fieldsContentLength;
    }

    public String getFieldsElapsedTime() {
        return fieldsElapsedTime;
    }

    public void setFieldsElapsedTime(String fieldsElapsedTime) {
        this.fieldsElapsedTime = fieldsElapsedTime;
    }

    public String getFieldsRequestHeaders() {
      return fieldsRequestHeaders;
    }

    public void setFieldsRequestHeaders(String fieldsRequestHeaders) {
      this.fieldsRequestHeaders = fieldsRequestHeaders;
    }

    public String getFieldsResponseHeaders() {
      return fieldsResponseHeaders;
    }

    public void setFieldsResponseHeaders(String fieldsResponseHeaders) {
      this.fieldsResponseHeaders = fieldsResponseHeaders;
    }
}
