# Logback JSON encoder for Logstash

First, add it to your project as a dependency.

Maven style:

```xml
<dependency>
  <groupId>net.logstash.logback</groupId>
  <artifactId>logstash-logback-encoder</artifactId>
  <version>2.0</version>
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

The resulting information contains the caller info by default.
This can be costly to calculate and should be switched off for busy production environments.

To switch if off add the includeCallerInfo property to the configuration.
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="stash" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>info</level>
        </filter>
        <file>/some/path/to/your/file.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeCallerInfo>false</includeCallerInfo>
        </encoder>
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
    codec => "json"
  }
}
```

For logback access logs, use it in your `logback-access.xml` like this:

```xml
<appender name="stash" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>/some/path/to/your/file.log</file>
    <encoder class="net.logstash.logback.encoder.LogstashAccessEncoder" />
</appender>

<appender-ref ref="stash" />
```
