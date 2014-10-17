package net.logstash.logback.fieldnames;

public class LogstashAccessFieldNames extends LogstashCommonFieldNames {
    private String fieldsMethod = "@fields.method";
    private String fieldsProtocol = "@fields.protocol";
    private String fieldsStatusCode = "@fields.status_code";
    private String fieldsRequestedUrl = "@fields.requested_url";
    private String fieldsRequestedUri = "@fields.requested_uri";
    private String fieldsRemoteHost = "@fields.remote_host";
    private String fieldsHostname = "@fields.HOSTNAME";
    private String fieldsRemoteUser = "@fields.remote_user";
    private String fieldsContentLength = "@fields.content_length";
    private String fieldsElapsedTime = "@fields.elapsed_time";

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
}
