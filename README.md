# Logback JSON encoder for Logstash

First, add it to your project as a dependency.

Maven style:

```xml
<dependency>
  <groupId>net.logstash.logback</groupId>
  <artifactId>logstash-logback-encoder</artifactId>
  <version>1.2</version>
</dependency>
```

Use it in your `logback.xml` like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="stash" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>info</level>
        </filter>
        <file>/some/path/to/your/file.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
    </appender>
    <root level="all">
        <appender-ref ref="stash" />
    </root>
</configuration>
```

Use it in your logstash configuration like this:

```
input {
  file {
    type => "your-log-type"
    path => "/some/path/to/your/file.log"
    format => "json_event"
  }
}
```
