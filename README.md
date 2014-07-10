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
## Output Destinations

Two types of output destinations are supported
* File
* Socket (via syslog)

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

## Fields

The fields included in the logstash event are described in the sections below.

### Standard Fields

These fields will appear in every log event unless otherwise noted.

| Field         | Description
|---------------|------------
| `@timestamp`  | Time of the log event. (`yyyy-MM-dd'T'HH:mm:ss.SSSZZ`)
| `@version`    | Logstash format version (e.g. 1)
| `message`     | Formatted log message of the event 
| `logger_name` | Name of the logger that logged the event
| `thread_name` | Name of the thread that logged the event
| `level`       | String name of the level of the event
| `level_value` | Integer value of the level of the event
| `stack_trace` | (Only if a throwable was logged) The stacktrace of the throwable.  Stackframes are separated by line endings.
| `tags`        | (Only if tags are found) The names of any markers not explicitly handled.  (e.g. markers from `MarkerFactory.getMarker` will be included as tags, but the markers from [`Markers`](/src/main/java/net/logstash/logback/marker/Markers.java) will not.)


Additionally, every field in the Mapped Diagnostic Context (MDC) (`org.slf4j.MDC`) and properties of Logback's Context (`ch.qos.logback.core.Context`) will appear as a field in the log event.


### Caller Info Fields
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

When switched on, the following fields will be included in the log event:

| Field                | Description
|----------------------|------------
| `caller_class_name`  | Fully qualified class name of the class that logged the event
| `caller_method_name` | Name of the method that logged the event
| `caller_file_name`   | Name of the file that logged the event
| `caller_line_number` | Line number of the file where the event was logged


### Custom Fields

In addition to the fields above, you can add other fields to the log event either globally, or on an event-by-event basis.

#### Global Custom Fields

Add custom fields that will appear in every json event like this : 
```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <customFields>{"appname":"damnGoodWebservice","roles":["customerorder","auth"],"buildinfo":{"version":"Version 0.1.0-SNAPSHOT","lastcommit":"75473700d5befa953c45f630c6d9105413c16fe1"}}</customFields>
</encoder>
```

OR

```xml
<appender name="stash" class="net.logstash.logback.appender.LogstashSocketAppender">
  <customFields>{"appname":"damnGoodWebservice","roles":["customerorder","auth"],"buildinfo":{"version":"Version 0.1.0-SNAPSHOT","lastcommit":"75473700d5befa953c45f630c6d9105413c16fe1"}}</customFields>
</appender>
```

#### Event-specific Custom Fields

When logging a message, you can specify additional fields to add to the json event by using the markers provided by 
[`Markers`](/src/main/java/net/logstash/logback/marker/Markers.java).

For example:

```java
import static net.logstash.logback.marker.Markers.*

/*
 * Add "name":"value" to the json event.
 */
logger.info(append("name", "value"), "log message");

/*
 * Add "name1":"value1","name2":"value2" to the json event by using multiple markers.
 */
logger.info(append("name1", "value1").with(append("name2", "value2")), "log message");

/*
 * Add "name1":"value1","name2":"value2" to the json event by using a map.
 *
 * Note the values can be any object that can be serialized by Jackson's ObjectMapper
 * (e.g. other Maps, JsonNodes, numbers, arrays, etc)
 */
Map myMap = new HashMap();
myMap.put("name1", "value1");
myMap.put("name2", "value2");
logger.info(embed(myMap), "log message");

/*
 * Add "array":[1,2,3] to the json event
 */
logger.info(appendArray("array", 1, 2, 3), "log message");

/*
 * Add "array":[1,2,3] to the json event by using raw json.
 * This allows you to use your own json seralization routine to construct the json output
 */
logger.info(appendRaw("array", "[1,2,3]"), "log message");

/*
 * Add any object that can be serialized by Jackson's ObjectMapper
 * (e.g. Maps, JsonNodes, numbers, arrays, etc)
 */
logger.info(append("object", myobject), "log message");

```

#### Old *deprecated* way of adding an event-specific `json_message` field

The old deprecated way of adding an event-specific `json_message` field to the json event involved using a marker named `"JSON"`.

For example:

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


The *new* preferred way of doing this is by using the **Event-specific Custom Fields**.

For example:

```java
import static net.logstash.logback.marker.Markers.*

logger.info(appendArray("json_message", 12, map), "Message {}", 12);
```

#### Old *deprecated* way of adding event-specific custom fields

The old deprecated way of embedding custom fields in the json event was to configure `enableContextMap` to true,
and then add a `Map` as the last argument on the log line. 

For example:

Configuration:
```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <enableContextMap>true</enableContextMap>
</encoder>
```

Log line:
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
}
```

The *new* preferred way of doing this is by using the **Custom Dynamic JSON fields**.

For example:

```java
import static net.logstash.logback.marker.Markers.*

logger.info(embed("duration", duration), "Service started in {} seconds", duration/1000);
```

## Logback access logs
For logback access logs, use it in your `logback-access.xml` like this:

```xml
<appender name="stash" class="ch.qos.logback.core.rolling.RollingFileAppender">
  <file>/some/path/to/your/file.log</file>
  <encoder class="net.logstash.logback.encoder.LogstashAccessEncoder" />
</appender>

<appender-ref ref="stash" />
```
