package net.logstash.logback.fieldnames;

public abstract class LogstashCommonFieldNames {
    private String timestamp = "@timestamp";
    private String version = "@version";
    private String message = "@message";

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
