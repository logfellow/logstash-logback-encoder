# Logback JSON encoder for Logstash

First, add it to your project as a dependency.

Maven style:

```xml
<dependency>
  <groupId>net.logstash.logback</groupId>
  <artifactId>logstash-logback-encoder</artifactId>
  <version>2.1</version>
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

The resulting information does not contains the caller info by default. 
This can be costly to calculate and should be switched off for busy production environments.

To switch it on add the includeCallerInfo property to the configuration.
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="stash" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>info</level>
        </filter>
        <file>/some/path/to/your/file.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeCallerInfo>true</includeCallerInfo>
        </encoder>
    </appender>
    <root level="all">
        <appender-ref ref="stash" />
    </root>
</configuration>
```

Add custom json fields to your json events like this : 
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="stash" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>info</level>
        </filter>
        <file>/some/path/to/your/file.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"appname":"damnGodWebservice","roles":["customerorder","auth"],"buildinfo":{"version":"Version 0.1.0-SNAPSHOT","lastcommit":"75473700d5befa953c45f630c6d9105413c16fe1"}}</customFields>
        </encoder>
    </appender>
    <root level="all">
        <appender-ref ref="stash" />
    </root>
</configuration>
```

You can send your json events by syslog channel like this : 
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="stash" class="net.logstash.logback.appender.LogstashSocketAppender">
        <syslogHost>MyAwsomeSyslogServer</syslogHost>
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
