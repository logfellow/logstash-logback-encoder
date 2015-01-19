# Logback JSON encoder for Logstash

Provides a logback encoder, logback layout, and several logback appenders
for outputting log messages in logstash's JSON format.


## Including it in your project
Maven style:

```xml
<dependency>
  <groupId>net.logstash.logback</groupId>
  <artifactId>logstash-logback-encoder</artifactId>
  <version>3.5</version>
</dependency>
```
## Usage

Two output appenders are provided:
* Syslog UDP Socket ([`LogstashSocketAppender`](/src/main/java/net/logstash/logback/appender/LogstashSocketAppender.java))
* TCP Socket ([`LogstashTcpSocketAppender`](/src/main/java/net/logstash/logback/appender/LogstashTcpSocketAppender.java))

Other logback appenders (such as `RollingFileAppender`) can use the
[`LogstashEncoder`](/src/main/java/net/logstash/logback/encoder/LogstashEncoder.java) or
[`LogstashLayout`](/src/main/java/net/logstash/logback/layout/LogstashLayout.java)
to format log messages.

### Syslog UDP Socket Appender

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

You can also use the `udp` input, which provides threading.
 
### TCP Socket Appender

Use the `LogstashEncoder` along with the `LogstashTcpSocketAppender` to log over TCP.
See the next section for an example of how to use the encoder with a file appender.

Example appender configuration in `logback.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <appender name="stash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
      <!-- remoteHost and port are optional (default values shown) -->
      <remoteHost>127.0.0.1</remoteHost>
      <port>4560</port>
  
      <!-- encoder is required -->
      <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
  </appender>
  
  <root level="DEBUG">
      <appender-ref ref="stash" />
  </root>
</configuration>
```

Example logstash configuration to read `LogstashTcpSocketAppender` messages:

```
input {
    tcp {
        port => 4560
        codec => json
    }
}
```

### Encoder / Layout

You can use the [`LogstashEncoder`](/src/main/java/net/logstash/logback/encoder/LogstashEncoder.java) or
[`LogstashLayout`](/src/main/java/net/logstash/logback/layout/LogstashLayout.java) with other logback appenders.

For example, to output logstash compatible JSON to a file, use the `LogstashEncoder` with the `RollingFileAppender` in your `logback.xml` like this:

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


## Fields

The fields included in the logstash event are described in the sections below.

### Standard Fields

These fields will appear in every log event unless otherwise noted.
The field names listed here are the default field names.
The field names can be customized (see below).

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


Additionally, every field in the Mapped Diagnostic Context (MDC) (`org.slf4j.MDC`)
and properties of Logback's Context (`ch.qos.logback.core.Context`) will appear as a field in the log event.
These can be disabled by specifying `<includeMdc>false</includeMdc>` or `<includeContext>false</includeContext>`,
respectively, in the encoder/layout/appender configuration.


#### Caller Info Fields
The encoder/layout/appender do not contain caller info by default. 
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

### Customizing Standard Field Names

The standard field names above can be customized by using the `fieldNames`
configuration element in the encoder or appender configuration

A shortened version of the field names can be configured like this:
```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <fieldNames class="net.logstash.logback.fieldnames.ShortenedFieldNames"/>
</encoder>
```
See [`ShortenedFieldNames`](/src/main/java/net/logstash/logback/fieldnames/ShortenedFieldNames.java)
for complete list of shortened names. 

If you want to customize individual field names, you can do so like this:
```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <fieldNames>
    <logger>logger</logger>
    <thread>thread</logger>
    ...
  </fieldNames>
</encoder>
```

See [`LogstashFieldNames`](/src/main/java/net/logstash/logback/fieldnames/LogstashFieldNames.java)
for all the field names that can be customized.

Prevent a field from being output by setting the field name to `[ignore]`.

Log the caller info, MDC properties, and context properties
in sub-objects within the JSON event by specifying field
names for `caller`, `mdc`, and `context`, respectively.
 
### Customizing Logger Name Field Length

You can shorten the logger name field length similar to the normal pattern style of "%logger{36}".  Examples of how it is shortened
can be found here: http://logback.qos.ch/manual/layouts.html#conversionWord

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <shortenedLoggerNameLength>36</shortenedLoggerNameLength>
</encoder>
```

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
logger.info(append("name1", "value1").and(append("name2", "value2")), "log message");

/*
 * Add "name1":"value1","name2":"value2" to the json event by using a map.
 *
 * Note the values can be any object that can be serialized by Jackson's ObjectMapper
 * (e.g. other Maps, JsonNodes, numbers, arrays, etc)
 */
Map myMap = new HashMap();
myMap.put("name1", "value1");
myMap.put("name2", "value2");
logger.info(appendEntries(myMap), "log message");

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

/*
 * Add fields of any object that can be unwrapped by Jackson's UnwrappableBeanSerializer.
 * i.e. The fields of an object can be written directly into the json output.
 * This is similar to the @JsonUnwrapped annotation.
 */
logger.info(appendFields(myobject), "log message");

```

See [DEPRECATED.md](DEPRECATED.md) for other deprecated ways of adding json to the output.

## Customizing JSON Factory and Generator

The `JsonFactory` and `JsonGenerator` used to serialize output can be customized by creating
custom instances of [`JsonFactoryDecorator`](/src/main/java/net/logstash/logback/decorate/JsonFactoryDecorator.java)
or [`JsonGeneratorDecorator`](/src/main/java/net/logstash/logback/decorate/JsonGeneratorDecorator.java), respectively.

For example, you could enable pretty printing like this:
```java
public class PrettyPrintingDecorator implements JsonGeneratorDecorator {

    @Override
    public JsonGenerator decorate(JsonGenerator generator) {
        return generator.useDefaultPrettyPrinter();
    }

}
```

and then specify your decorator in the logback.xml file like this:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <jsonGeneratorDecorator class="your.package.PrettyPrintingDecorator"/>
</encoder>
```

## Customizing Stack Traces

By default, stack traces are formatted using logback's default format.

Alternatively, you can use your own stack trace formatter, or the included
[`ShortenedStackTraceFormatter`](/src/main/java/net/logstash/logback/stacktrace/ShortenedStackTraceFormatter.java)
to shorten stacktraces by:

* Limiting the number of stackTraceElements per throwable (applies to each individual throwable.  e.g. caused-bys and suppressed).
* Abbreviating class names (similar to how logger names are shortened)</li>
* Filtering out consecutive unwanted stackTraceElements based on regular expressions

For example:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <stackTraceFormatter class="net.logstash.logback.stacktrace.ShortenedStackTraceFormatter">
    <maxStackTraceElementsPerThrowable>30</maxStackTraceElementsPerThrowable>
    <shortenedClassNameLength>20</shortenedClassNameLength>
    <exclude>sun\.reflect\..*\.invoke.*</exclude>
    <exclude>net\.sf\.cglib\.proxy\.MethodProxy\.invoke</exclude>
  </stackTraceFormatter>
</encoder>
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

or if you want to log directly into logstash,

```xml
<appender name="stash" class="net.logstash.logback.appender.LogstashAccessTcpSocketAppender">
  <remoteHost>........</remoteHost>
  <port>.....</port>
  <encoder class="net.logstash.logback.encoder.LogstashAccessEncoder" />
</appender>

<appender-ref ref="stash" />
```

The default field names used for access logs are different than those documented above.
See [`LogstashAccessFieldNames`](/src/main/java/net/logstash/logback/fieldnames/LogstashAccessFieldNames.java)
for all the field names used for access logs.

### Using PatternLayout--based JSON encoders
In addition to the pair of encoder (LogstashEncoder/LogstashAccessEncoder) shown in the examples above, there is
a pair of alternative encoders (LoggingEventJsonPatternLayoutEncoder/AccessEventJsonPatternLayoutEncoder) that allow you to
customize JSON being that is send to logstash even more.

Effectively you are providing a template of JSON to be send to the logstash server and encoder just populates it with values.
Every value in the template is treated as a pattern for logback's standard PatternLayout so it can be a combination
of literal strings (for some constants) and various conversion specifiers (like %d for date).
Only the fields you explicitly included into pattern will be logged.

Note that "@version" and "@timestamp" fields are added automatically so they do not have to be specified in the format pattern.
If, however, you include them in the format then your definition will be used instead.

This example
```xml
        <encoder class="net.logstash.logback.encoder.LoggingEventJsonPatternLayoutEncoder">
            <pattern>
                { "level": "%p" }
            </pattern>
        </encoder>
```
Will produce something like
```
{"@time":"...", "@version": 1, "level": "DEBUG"}
```

The real power comes from the fact that there are lots of standard conversion specifiers so you
can customise what is logged and how. For example, you could log a single specific value from MDC with "%X{mykey}"
or, for access logs, you could log a single request header with "%i{User-Agent}".

You can use nested objects an arrays in your pattern which provides you the same functionality as custom fields feature
of other logstash appenders.

<b>Important note</b>: if you use a null, number or a boolean constant in a pattern, it will keep its type in the
resulting JSON. However only the text values are searched for conversion patterns.
And, as these patterns are sent through PatternLayout, the result is always a string
even for something which you may feel should be a number - like for %b (bytes sent, in access logs).

You can either deal with the type conversion on the logstash side or you may use special operations provided by this encoder.
The operations are:

* @asLong{...} - evaluates pattern in curly braces and then converts resulting string to a long.
* @asDouble{...} - evaluates pattern in curly braces and then converts resulting string to a double.

So this example
```xml
            <pattern>
                {
                "bytes_sent_str": "%b",
                "bytes_sent_long": "@asLong{%b}"
                }
            </pattern>
```
Will produce something like
```
{"@time":"...", "@version": 1, "bytes_sent_str": "1024", "bytes_sent_long": 1024}
```

So value that is sent for bytes_sent_long is a number even though in your pattern it is a quoted text.

Note that there are two different PatternLayouts just like there are two different encoders for logstash.
And these layouts support different set of conversion patterns

### LoggingEventJsonPatternLayoutEncoder

LogstashLoggingLoggingEncoder uses PatternLayouts from logback-classic - http://logback.qos.ch/xref/ch/qos/logback/classic/PatternLayout.html

For the help with supported conversions - see http://logback.qos.ch/manual/layouts.html#conversionWord

```xml
        <encoder class="net.logstash.logback.encoder.LoggingEventJsonPatternLayoutEncoder">
            <pattern>
                {
                "custom_constant": "123",
                "tags": ["one", "two"],

                "logger": "%logger",
                "level": "%level",
                "thread": "%thread",
                "message": "%m%n%ex",
...
                }
            </pattern>
        </encoder>
```

Note that LogstashLoggingAccessEncoder does not support stack trace formatters supported by LogstashLayout so
you cannot abbreviate class names in the stack trace. You still can limit stack length though with the
standard conversion pattern of %ex{10} for 10 first lines.

There is also a special operation that can be used with this encoder:
* @mdc - if you use it as a value, that value will be replaced with a nested object containing all
the values from MDC

If, instead, you want your MDC values on the top level, you will have to pick them individually with something like

```xml
            <pattern>
                {
                "mdckey1": "%X{key1}",
                "mdckey2": "%X{key2}"
                }
            </pattern>
```

There is no way to include all the values from MDC into the top level of the resulting JSON object.


### AccessEventJsonPatternLayoutEncoder

LogstashLoggingAccessEncoder uses PatternLayouts from logback-access - http://logback.qos.ch/xref/ch/qos/logback/access/PatternLayout.html

```xml
        <encoder class="net.logstash.logback.encoder.AccessEventJsonPatternLayoutEncoder">
            <pattern>
                {
                "custom_constant": "123",

                "remote_ip": "%a",
                "status_code": "%s",
                "elapsed_time": "%D",
                "user_agent": "%i{User-Agent}",
                "accept": "%i{Accept}",
                "referer": "%i{Referer}",
...
                }
            </pattern>
        </encoder>
```

## Build status
[![Build Status](https://travis-ci.org/logstash/logstash-logback-encoder.svg?branch=master)](https://travis-ci.org/logstash/logstash-logback-encoder)
