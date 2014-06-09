# Logback JSON encoder for Logstash

First, add it to your project as a dependency.

Maven style:

```xml
<dependency>
  <groupId>net.logstash.logback</groupId>
  <artifactId>logstash-logback-encoder</artifactId>
  <version>2.8</version>
</dependency>
```

### File Output
To output logstash compatible JSON to a file, use the `LogstashEncoder` in your `logback.xml` like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <appender name="stash" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
      <level>info</level>
    </filter>
    <file>/some/path/to/your/file.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
      <fileNamePattern>/some/path/to/your/file.log.%d{yyyy-MM-dd}</fileNamePattern>
      <maxHistory>30</maxHistory>
    </rollingPolicy>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
  </appender>
  <root level="all">
    <appender-ref ref="stash" />
  </root>
</configuration>
```

Then use the `file` input in logstash like this:

```
input {
  file {
    type => "your-log-type"
    path => "/some/path/to/your/file.log"
    codec => "json"
  }
}
```

### Socket Output (via syslog channel)

To output logstash compatible JSON to a syslog channel, use the `LogstashSocketAppender` in your `logback.xml` like this:
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

Then use the `syslog` input in logstash like this:

```
input {
  syslog {
    type => "your-log-type"
    codec => "json"
  }
}
```


### Caller Info
The `LogstashEncoder` and `LogstashSocketAppender` do not contain caller info by default. 
This can be costly to calculate and should be switched off for busy production environments.

To switch it on, add the `includeCallerInfo` property to the configuration.
```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <includeCallerInfo>true</includeCallerInfo>
</encoder>
```

OR

```xml
<appender name="stash" class="net.logstash.logback.appender.LogstashSocketAppender">
  <includeCallerInfo>true</includeCallerInfo>
</appender>
```


### Custom JSON fields

Add custom json fields to your json events like this : 
```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <customFields>{"appname":"damnGodWebservice","roles":["customerorder","auth"],"buildinfo":{"version":"Version 0.1.0-SNAPSHOT","lastcommit":"75473700d5befa953c45f630c6d9105413c16fe1"}}</customFields>
</encoder>
```

OR

```xml
<appender name="stash" class="net.logstash.logback.appender.LogstashSocketAppender">
  <customFields>{"appname":"damnGodWebservice","roles":["customerorder","auth"],"buildinfo":{"version":"Version 0.1.0-SNAPSHOT","lastcommit":"75473700d5befa953c45f630c6d9105413c16fe1"}}</customFields>
</appender>
```

### JSON arguments
You can also send raw JSON in the arguments field if you include the marker "JSON" like this and it will be output under the 'json_message' field in the resulting JSON:

```java
logger.info(MarkerFactory.getMarker("JSON"), "Message {}", "<yourJSONhere>");
```

Example:

```java
Map<String, Object> map = new HashMap<String, Object>();
map.put("field1", "value1");
map.put("field2", "value2");
map.put("field3", Collections.singletonMap("subfield1", "subvalue1"));

logger.info(MarkerFactory.getMarker("JSON"), "Message {}", 12, map);
```

Results in the following in the Logstash JSON:

```json
{
  "@timestamp": "2014-06-04T15:26:14.464+02:00",
  "@version": 1,
  "message": "Message 12",
  "json_message": [
    12,
    {
      "field1": "value1",
      "field2": "value2",
      "field3": {
        "subfield1": "subvalue1"
      }
    }
  ]
}
```

### Custom Context Fields
For some situations you might want to directly pass fields to the JSON message so it doesn't need to be parsed from the message.
If this feature is enabled and the last argument in the argument list is a `Map` then it will be added to the JSON.

Configuration:
```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <enableContextMap>true</enableContextMap>
</encoder>
```

Example:
```java
    log.info("Service started in {} seconds", duration/1000, Collections.singletonMap("duration", duration));
```

Result:
```json
{
  "@timestamp": "2014-06-04T15:26:14.464+02:00",
  "@version": 1,
  "message": "Service started in 12 seconds",
  "logger_name": "com.acme.Tester",
  "thread_name": "main",
  "level": "INFO",
  "level_value": 20000,
  "duration": 12368,
  "tags": []
}
```

### Logback access logs
For logback access logs, use it in your `logback-access.xml` like this:

```xml
<appender name="stash" class="ch.qos.logback.core.rolling.RollingFileAppender">
  <file>/some/path/to/your/file.log</file>
  <encoder class="net.logstash.logback.encoder.LogstashAccessEncoder" />
</appender>

<appender-ref ref="stash" />
```
