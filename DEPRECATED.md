# Deprecated Functionality #

This documents deprecated functionality that might be removed in future releases. 


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

The old deprecated way of adding custom fields in the json event was to configure `enableContextMap` to true,
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

The *new* preferred way of doing this is by using the **Event-specific Custom Fields**.

For example:

```java
import static net.logstash.logback.argument.StructuredArguments.*

logger.info("Service started in {} ms", value("duration", duration));
```

