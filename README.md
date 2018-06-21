> !! This document applies to the next version under development.
>
> &nbsp; &nbsp; See [here for documentation on the latest released version](https://github.com/logstash/logstash-logback-encoder/tree/logstash-logback-encoder-5.1).

# Logback JSON encoder

Provides [logback](http://logback.qos.ch/) encoders, layouts, and appenders to log in JSON format.

Supports both regular _LoggingEvents_ (logged through a `Logger`) and _AccessEvents_ (logged via [logback-access](http://logback.qos.ch/access.html)).

Originally written to support output in [logstash](http://logstash.net/)'s JSON format, but has evolved into a highly-configurable, general-purpose, JSON logging mechanism.  The structure of the JSON output, and the data it contains, is fully configurable.

#### Contents:

* [Including it in your project](#including-it-in-your-project)
* [Usage](#usage)
  * [UDP Appender](#udp-appender)
  * [TCP Appenders](#tcp-appenders)
    * [Keep-alive](#keep-alive)
    * [Multiple Destinations](#multiple-destinations)
    * [Reconnection Delay](#reconnection-delay)
    * [Write buffer size](#write-buffer-size)
    * [SSL](#ssl)
  * [Async Appenders](#async-appenders)
  * [Appender Listeners](#appender-listeners)
  * [Encoders / Layouts](#encoders--layouts)
* [LoggingEvent Fields](#loggingevent-fields)
  * [Standard Fields](#standard-fields)
  * [MDC fields](#mdc-fields)
  * [Context fields](#context-fields)
  * [Caller Info Fields](#caller-info-fields)
  * [Custom Fields](#custom-fields)
    * [Global Custom Fields](#global-custom-fields)
    * [Event-specific Custom Fields](#event-specific-custom-fields)
* [AccessEvent Fields](#accessevent-fields)
  * [Standard Fields](#standard-fields)
  * [Header Fields](#header-fields)
* [Customizing Standard Field Names](#customizing-standard-field-names)
* [Customizing Version](#customizing-version)
* [Customizing Timestamp](#customizing-timestamp)
* [Customizing JSON Factory and Generator](#customizing-json-factory-and-generator)
* [Customizing Character Escapes](#customizing-character-escapes)
* [Customizing Logger Name Length](#customizing-logger-name-length)
* [Customizing Stack Traces](#customizing-stack-traces)
* [Prefix/Suffix](#prefixsuffix)
* [Composite Encoder/Layout](#composite-encoderlayout)
  * [Providers for LoggingEvents](#providers-for-loggingevents)
  * [Providers for AccessEvents](#providers-for-accessevents)
  * [Nested JSON Provider](#nested-json-provider)
  * [Pattern JSON Provider](#pattern-json-provider)
    * [LoggingEvent patterns](#loggingevent-patterns)
    * [AccessEvent patterns](#accessevent-patterns)
  * [Custom JSON Provider](#custom-json-provider)
* [Debugging](#debugging)



## Including it in your project

Maven style:

```xml
<dependency>
  <groupId>net.logstash.logback</groupId>
  <artifactId>logstash-logback-encoder</artifactId>
  <version>5.1</version>
</dependency>
```

If you get `ClassNotFoundException`/`NoClassDefFoundError`/`NoSuchMethodError` at runtime, then ensure the required dependencies (and appropriate versions) as specified in the pom file from the maven repository exist on the runtime classpath.  Specifically, the following need to be available on the runtime classpath:

* jackson-databind / jackson-core / jackson-annotations
* logback-core
* logback-classic (required for logging _LoggingEvents_)
* logback-access (required for logging _AccessEvents_)
* slf4j-api

Older versions than the ones specified in the pom file _might_ work, but the versions in the pom file are what testing has been performed against.

If you are using logstash-logback-encoder in a project (such as spring-boot) that also declares dependencies on any of the above libraries, you might need to tell maven explicitly which versions to use to avoid conflicts.
You can do so using maven's [dependencyManagement](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Management) feature.
For example, to ensure that maven doesn't pick different versions of logback-core, logback-classic, and logback-access, add this to your project's pom.xml

```xml
    <properties>
        <ch.qos.logback.version>1.2.3</ch.qos.logback.version>
    </properties>
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>ch.qos.logback</groupId>
                <artifactId>logback-core</artifactId>
                <version>${ch.qos.logback.version}</version>
            </dependency>
            <dependency>
                <groupId>ch.qos.logback</groupId>
                <artifactId>logback-classic</artifactId>
                <version>${ch.qos.logback.version}</version>
            </dependency>
            <dependency>
                <groupId>ch.qos.logback</groupId>
                <artifactId>logback-access</artifactId>
                <version>${ch.qos.logback.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
```

## Usage

To log using JSON format, you must configure logback to use either:

* an appender provided by the logstash-logback-encoder library, OR
* an appender provided by logback (or another library) with an encoder or layout provided by the logstash-logback-encoder library

The appenders, encoders, and layouts provided by the logstash-logback-encoder library are as follows:

| Format        | Protocol   | Function | LoggingEvent | AccessEvent
|---------------|------------|----------| ------------ | -----------
| Logstash JSON | Syslog/UDP | Appender | [`LogstashSocketAppender`](/src/main/java/net/logstash/logback/appender/LogstashSocketAppender.java) | n/a
| Logstash JSON | TCP        | Appender | [`LogstashTcpSocketAppender`](/src/main/java/net/logstash/logback/appender/LogstashTcpSocketAppender.java) | [`LogstashAccessTcpSocketAppender`](/src/main/java/net/logstash/logback/appender/LogstashAccessTcpSocketAppender.java)
| any           | any        | Appender | [`LoggingEventAsyncDisruptorAppender`](/src/main/java/net/logstash/logback/appender/LoggingEventAsyncDisruptorAppender.java) | [`AccessEventAsyncDisruptorAppender`](/src/main/java/net/logstash/logback/appender/AccessEventAsyncDisruptorAppender.java)
| Logstash JSON | any        | Encoder  | [`LogstashEncoder`](/src/main/java/net/logstash/logback/encoder/LogstashEncoder.java) | [`LogstashAccessEncoder`](/src/main/java/net/logstash/logback/encoder/LogstashAccessEncoder.java)
| Logstash JSON | any        | Layout   | [`LogstashLayout`](/src/main/java/net/logstash/logback/layout/LogstashLayout.java) | [`LogstashAccessLayout`](/src/main/java/net/logstash/logback/layout/LogstashAccessLayout.java)
| General JSON  | any        | Encoder  | [`LoggingEventCompositeJsonEncoder`](/src/main/java/net/logstash/logback/encoder/LoggingEventCompositeJsonEncoder.java) | [`AccessEventCompositeJsonEncoder`](/src/main/java/net/logstash/logback/encoder/AccessEventCompositeJsonEncoder.java)
| General JSON  | any        | Layout   | [`LoggingEventCompositeJsonLayout`](/src/main/java/net/logstash/logback/layout/LoggingEventCompositeJsonLayout.java) | [`AccessEventCompositeJsonLayout`](/src/main/java/net/logstash/logback/encoder/AccessEventCompositeJsonLayout.java)

These encoders/layouts can generally be used by any logback appender (such as `RollingFileAppender`).

The general _composite_ JSON encoders/layouts can be used to
output any JSON format/data by configuring them with various JSON _providers_.
The Logstash encoders/layouts are really just extensions of the general
composite JSON encoders/layouts with a pre-defined set of providers.

The logstash encoders/layouts are easier to configure if you want to use the standard logstash version 1 output format.
Use the [composite encoders/layouts](#composite-encoderlayout) if you want to heavily customize the output,
or if you need to use logstash version 0 output.

The `*AsyncDisruptorAppender` appenders are similar to logback's `AsyncAppender`,
except that a [LMAX Disruptor RingBuffer](https://lmax-exchange.github.io/disruptor/)
is used as the queuing mechanism, as opposed to a `BlockingQueue`.
These async appenders can delegate to any other underlying logback appender.



### UDP Appender

To output JSON for LoggingEvents to a syslog/UDP channel,
use the `LogstashSocketAppender` in your `logback.xml` like this:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <appender name="stash" class="net.logstash.logback.appender.LogstashSocketAppender">
    <host>MyAwesomeSyslogServer</host>
    <!-- port is optional (default value shown) -->
    <port>514</port>
  </appender>
  <root level="all">
    <appender-ref ref="stash" />
  </root>
</configuration>
```
Internally, the `LogstashSocketAppender` uses a `LogstashLayout` to perform the JSON formatting.
Therefore, by default, the output will be logstash-compatible.

You can further customize the JSON output of `LogstashSocketAppender`
just like you can with a `LogstashLayout` or `LogstashEncoder` as described in later sections.
It is not necessary to configure a `<layout>` or `<encoder>` sub-element
within the `<appender>` element in the logback configuration.
All the properties of `LogstashLayout` or `LogstashEncoder` can be set at the `<appender>` level.
For example, to configure [global custom fields](#global-custom-fields), you can specify
```xml
  <appender name="stash" class="net.logstash.logback.appender.LogstashSocketAppender">
    <host>MyAwesomeSyslogServer</host>
    <!-- port is optional (default value shown) -->
    <port>514</port>
    <customFields>{"appname":"myWebservice"}</customFields>
  </appender>
```

There currently is no way to log AccessEvents over syslog/UDP.

To receive syslog/UDP input in logstash, configure a [`syslog`](http://www.logstash.net/docs/latest/inputs/syslog) or [`udp`](http://www.logstash.net/docs/latest/inputs/udp) input with the [`json`](http://www.logstash.net/docs/latest/codecs/json) codec in logstash's configuration like this:
```
input {
  syslog {
    codec => "json"
  }
}
```


### TCP Appenders

To output JSON for LoggingEvents over TCP, use a `LogstashTcpSocketAppender`
with a `LogstashEncoder` or `LoggingEventCompositeJsonEncoder`.

Example logging appender configuration in `logback.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <appender name="stash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
      <destination>127.0.0.1:4560</destination>

      <!-- encoder is required -->
      <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
  </appender>

  <root level="DEBUG">
      <appender-ref ref="stash" />
  </root>
</configuration>
```


To output JSON for AccessEvents over TCP, use a `LogstashAccessTcpSocketAppender`
with a `LogstashAccessEncoder` or `AccessEventCompositeJsonEncoder`.

Example access appender in `logback-access.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <appender name="stash" class="net.logstash.logback.appender.LogstashAccessTcpSocketAppender">
      <destination>127.0.0.1:4560</destination>

      <!-- encoder is required -->
      <encoder class="net.logstash.logback.encoder.LogstashAccessEncoder" />
  </appender>

  <appender-ref ref="stash" />
</configuration>
```

Unlike the [UDP appender](#udp), an encoder must be configured for the TCP appenders.
You can use a `Logstash*Encoder`, `*EventCompositeJsonEncoder`, or any other logback encoder.
All of the output formatting options are configured at the encoder level.

Internally, the TCP appenders are asynchronous (using the [LMAX Disruptor RingBuffer](https://lmax-exchange.github.io/disruptor/)).
All the encoding and TCP communication is delegated to a single writer thread.
There is no need to wrap the TCP appenders with another asynchronous appender
(such as `AsyncAppender` or `LoggingEventAsyncDisruptorAppender`).

All the configuration parameters (except for sub-appender) of the [async appenders](#async)
are valid for TCP appenders.  For example, `waitStrategyType` and `ringBufferSize`.

The TCP appenders will never block the logging thread.
If the RingBuffer is full (e.g. due to slow network, etc), then events will be dropped.

The TCP appenders will automatically reconnect if the connection breaks.
However, events may be lost before Java's socket realizes the connection has broken.

To receive TCP input in logstash, configure a [`tcp`](http://www.logstash.net/docs/latest/inputs/tcp)
input with the [`json_lines`](http://www.logstash.net/docs/latest/codecs/json_lines) codec in logstash's configuration like this:

```
input {
    tcp {
        port => 4560
        codec => json_lines
    }
}
```

In order to guarantee that logged messages have had a chance to be processed by the TCP appender, you'll need to [cleanly shut down logback](http://logback.qos.ch/manual/configuration.html#stopContext) when your application exits.


#### Keep-alive

If events occur infrequently, and the connection breaks consistently due to a server-side idle timeout,
then you can enable keep alive functionality by configuring a `keepAliveDuration` like this:

```xml
  <appender name="stash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
      ...
      <keepAliveDuration>5 minutes</keepAliveDuration>
  </appender>
```

When the `keepAliveDuration` is set, then a keep alive message will be sent
if an event has not occurred for the length of the duration.
The keep alive message defaults to the system's line separator,
but can be changed by setting the `keepAliveMessage` property.


#### Multiple Destinations

The TCP appenders can be configured to try to connect to one of several destinations like this:
```xml
  <appender name="stash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
      <destination>destination1.domain.com:4560</destination>
      <destination>destination2.domain.com:4560</destination>
      <destination>destination3.domain.com:4560</destination>

      ...
  </appender>
```

or this:

```xml
  <appender name="stash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
      <destination>
          destination1.domain.com:4560,
          destination2.domain.com:4560,
          destination3.domain.com:4560
      </destination>

      ...
  </appender>
```

The appender uses a `connectionStrategy` to determine:

* the order in which destination connections are attempted, and 
* when an established connection should be reestablished (to the next destination selected by the connection strategy).

Logs are only sent to one destination at a time (i.e. not all destinations).
By default, the appender will stay connected to the connected destination
until it breaks, or until the application is shut down.
Some connection strategies can force a reconnect (see below).
If a connection breaks, then the appender will attempt to connect
to the next destination selected by the connection strategy. 


The available connection strategies are as follows:

<table>
  <tbody>
    <tr>
      <th>Strategy</th>
      <th>Description</th>
    </tr>
    <tr>
      <td><tt>preferPrimary</tt></td>
      <td>(default)
The first destination is considered the <em>primary</em> destination.
Each additional destination is considered a <em>secondary</em> destination.
This strategy prefers the primary destination, unless it is down.
The appender will attempt to connect to each destination in the order in which they are configured.
If a connection breaks, then the appender will again attempt to connect
to the destinations in the order in which they are configured,
starting at the first/primary destination.
<br/><br/>
The <tt>secondaryConnectionTTL</tt> can be set to gracefully close connections to <em>secondary</em>
destinations after a specific duration.  This will force the
the appender to reattempt to connect to the destinations in order again.
The <tt>secondaryConnectionTTL</tt> value does not affect connections to the
<em>primary</em> destination.
<br/><br/>
Example:
<pre>
  &lt;appender name="stash" class="net.logstash.logback.appender.LogstashTcpSocketAppender"&gt;
      &lt;destination&gt;destination1.domain.com:4560&lt;/destination&gt;
      &lt;destination&gt;destination2.domain.com:4560&lt;/destination&gt;
      &lt;destination&gt;destination3.domain.com:4560&lt;/destination&gt;
      &lt;connectionStrategy&gt;
          &lt;preferPrimary&gt;
              &lt;secondaryConnectionTTL&gt;5 minutes&lt;/secondaryConnectionTTL&gt;
          &lt;/preferPrimary&gt;
      &lt;/connectionStrategy&gt;
  &lt;/appender&gt;
</pre>
      </td>
    </tr>
    <tr>
      <td><tt>roundRobin</tt></td>
      <td>
This strategy attempts connections to the destination in round robin order.
If a connection fails, the next destination is attempted.
<br/><br/>
The <tt>connectionTTL</tt> can be set to gracefully close connections after a specific duration.
This will force the the appender to reattempt to connect to the next destination.
<br/><br/>
Example:
<pre>
  &lt;appender name="stash" class="net.logstash.logback.appender.LogstashTcpSocketAppender"&gt;
      &lt;destination&gt;destination1.domain.com:4560&lt;/destination&gt;
      &lt;destination&gt;destination2.domain.com:4560&lt;/destination&gt;
      &lt;destination&gt;destination3.domain.com:4560&lt;/destination&gt;
      &lt;connectionStrategy&gt;
          &lt;roundRobin&gt;
              &lt;connectionTTL&gt;5 minutes&lt;/connectionTTL&gt;
          &lt;/roundRobin&gt;
      &lt;/connectionStrategy&gt;
  &lt;/appender&gt;
</pre>
      </td>
    </tr>
    <tr>
      <td><tt>random</tt></td>
      <td>
This strategy attempts connections to the destination in a random order.
If a connection fails, the next random destination is attempted.
<br/><br/>
The <tt>connectionTTL</tt> can be set to gracefully close connections after a specific duration.
This will force the the appender to reattempt to connect to the next random destination.
<br/><br/>
Example:
<pre>
  &lt;appender name="stash" class="net.logstash.logback.appender.LogstashTcpSocketAppender"&gt;
      &lt;destination&gt;destination1.domain.com:4560&lt;/destination&gt;
      &lt;destination&gt;destination2.domain.com:4560&lt;/destination&gt;
      &lt;destination&gt;destination3.domain.com:4560&lt;/destination&gt;
      &lt;connectionStrategy&gt;
          &lt;random&gt;
              &lt;connectionTTL&gt;5 minutes&lt;/connectionTTL&gt;
          &lt;/random&gt;
      &lt;/connectionStrategy&gt;
  &lt;/appender&gt;
</pre>
      </td>
    </tr>
  </tbody>
</table>

You can also use your own custom connection strategy by implementing the `DestinationConnectionStrategy` interface,
and configuring the appender to use it like this:

```xml
  <appender name="stash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
      <destination>destination1.domain.com:4560</destination>
      <destination>destination2.domain.com:4560</destination>
      <destination>destination3.domain.com:4560</destination>
      <connectionStrategy class="your.package.YourDestinationConnectionStrategy">
      </connectionStrategy>
  </appender>
```


#### Reconnection Delay

If connecting fails to all configured destinations, the TCP appender by default will wait
30 seconds before reattempting to connect.

This amount of time to delay can be changed by setting the `reconnectionDelay` field.

```xml
  <appender name="stash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
      ...
      <reconnectionDelay>1 second</reconnectionDelay>
  </appender>
```


#### Write buffer size

By default, a buffer size of 8192 is used to buffer socket output stream writes.
You can adjust this by setting the appender's `writeBufferSize`.
 
```xml
  <appender name="stash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
      ...
      <writeBufferSize>16384</writeBufferSize>
  </appender>
```

Buffering can be disabled by setting the `writeBufferSize` to `0`.
If buffering is disabled, the writer thread can slow down, but it will also can prevent dropping events in the buffer on flaky connections.

#### SSL

To use SSL, add an `<ssl>` sub-element within the `<appender>` element for the `LogstashTcpSocketAppender`
or `LogstashAccessTcpSocketAppender`.

See the [logback manual](http://logback.qos.ch/manual/usingSSL.html) for how to configure SSL.
SSL for the `Logstash*TcpSocketAppender`s are configured the same way as logback's `SSLSocketAppender`.

For example, to enable SSL using the JVM's default keystore/truststore, do the following:

```xml
  <appender name="stash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
      ...

      <!-- Enable SSL using the JVM's default keystore/truststore -->
      <ssl/>
  </appender>
```

To use a different truststore, do the following:

```xml
  <appender name="stash" class="net.logstash.logback.appender.LogstashAccessTcpSocketAppender">
      ...

      <!-- Enable SSL and use a different truststore -->
      <ssl>
          <trustStore>
              <location>classpath:server.truststore</location>
              <password>${server.truststore.password}</password>
          </trustStore>
      </ssl>
  </appender>
```

All the customizations that [logback](http://logback.qos.ch/manual/usingSSL.html) offers
(such as configuring cipher specs, protocols, algorithms, providers, etc.)
are supported by the `Logback*TcpSocketAppender`s.

See the logstash documentation for the [`tcp`](http://www.logstash.net/docs/latest/inputs/tcp) input
for how to configure it to use SSL.


### Async Appenders

The `*AsyncDisruptorAppender` appenders are similar to logback's `AsyncAppender`,
except that a [LMAX Disruptor RingBuffer](https://lmax-exchange.github.io/disruptor/)
is used as the queuing mechanism, as opposed to a `BlockingQueue`.
These async appenders can delegate to any other underlying logback appender.

For example:

```xml
  <appender name="async" class="net.logstash.logback.appender.LoggingEventAsyncDisruptorAppender">
    <appender class="ch.qos.logback.core.rolling.RollingFileAppender">
       ...
    </appender>
  </appender>
```

The async appenders will never block the logging thread.
If the RingBuffer is full (e.g. due to slow network, etc), then events will be dropped.

By default, the [`BlockingWaitStrategy`](https://lmax-exchange.github.io/disruptor/docs/com/lmax/disruptor/BlockingWaitStrategy.html)
is used by the worker thread spawned by this appender.
The `BlockingWaitStrategy` minimizes CPU utilization, but results in slower latency and throughput.
If you need faster latency and throughput (at the expense of higher CPU utilization), consider
a different wait strategy offered by the disruptor, such as `SleepingWaitStrategy`.

The wait strategy can be configured on the async appender using the `waitStrategyType` parameter, like this:
```xml
  <appender name="async" class="net.logstash.logback.appender.LoggingEventAsyncDisruptorAppender">
    <waitStrategyType>sleeping</waitStrategyType>
    <appender class="ch.qos.logback.core.rolling.RollingFileAppender">
       ...
    </appender>
  </appender>
```

The supported wait strategies are as follows:

<table>
  <tbody>
    <tr>
      <th>Wait Strategy</th>
      <th>Parameters</th>
      <th>Implementation</th>
    </tr>
    <tr>
      <td><tt>blocking</tt></td>
      <td>none</td>
      <td><a href="https://lmax-exchange.github.io/disruptor/docs/com/lmax/disruptor/BlockingWaitStrategy.html"><tt>BlockingWaitStrategy</tt></a></td>
    </tr>
    <tr>
      <td><tt>busySpin</tt></td>
      <td>none</td>
      <td><a href="https://lmax-exchange.github.io/disruptor/docs/com/lmax/disruptor/BusySpinWaitStrategy.html"><tt>BusySpinWaitStrategy</tt></a></td>
    </tr>
    <tr>
      <td><tt>liteBlocking</tt></td>
      <td>none</td>
      <td><a href="https://lmax-exchange.github.io/disruptor/docs/com/lmax/disruptor/LiteBlockingWaitStrategy.html"><tt>LiteBlockingWaitStrategy</tt></a></td>
    </tr>
    <tr>
      <td><tt>sleeping</tt></td>
      <td>none</td>
      <td><a href="https://lmax-exchange.github.io/disruptor/docs/com/lmax/disruptor/SleepingWaitStrategy.html"><tt>SleepingWaitStrategy</tt></a></td>
    </tr>
    <tr>
      <td><tt>yielding</tt></td>
      <td>none</td>
      <td><a href="https://lmax-exchange.github.io/disruptor/docs/com/lmax/disruptor/YieldingWaitStrategy.html"><tt>YieldingWaitStrategy</tt></a></td>
    </tr>
    <tr>
      <td><pre>phasedBackoff{
  <em>spinTime</em>,
  <em>yieldTime</em>,
  <em>timeUnit</em>,
  <em>fallbackStrategy</em>
}
</pre>
e.g.<br/><tt>phasedBackoff{10,60,seconds,blocking}</tt></td>
      <td>
        <ol>
          <li><tt>spinTime</tt> - Time to spin before yielding</li>
          <li><tt>yieldTime</tt> - Time to yield before falling back to the <tt>fallbackStrategy</tt></li>
          <li><tt>timeUnit</tt> - Units of time for spin and yield timeouts. String name of a <a href="http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeUnit.html"><tt>TimeUnit</tt></a> value (e.g. <tt>seconds</tt>)</li>
          <li><tt>fallbackStrategy</tt> - String name of the wait strategy to fallback to after the timeouts have elapsed</li>
        </ol>
      </td>
      <td><a href="https://lmax-exchange.github.io/disruptor/docs/com/lmax/disruptor/PhasedBackoffWaitStrategy.html"><tt>PhasedBackoffWaitStrategy</tt></a></td>
    </tr>
    <tr>
      <td><pre>timeoutBlocking{
  <em>timeout</em>,
  <em>timeUnit</em>
}
</pre>e.g.<br/><tt>timeoutBlocking{1,minutes}</tt></td>
      <td>
        <ol>
          <li><tt>timeout</tt> - Time to block before throwing an exception</li>
          <li><tt>timeUnit</tt> - Units of time for timeout. String name of a <a href="http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeUnit.html"><tt>TimeUnit</tt></a> value (e.g. <tt>seconds</tt>)</li>
        </ol>
      </td>
      <td><a href="https://lmax-exchange.github.io/disruptor/docs/com/lmax/disruptor/TimeoutBlockingWaitStrategy.html"><tt>TimeoutBlockingWaitStrategy</tt></a></td>
    </tr>
  </tbody>
</table>

See [AsyncDisruptorAppender](/src/main/java/net/logstash/logback/appender/AsyncDisruptorAppender.java)
for other configuration parameters (such as `ringBufferSize`, `producerType`, `threadNamePrefix`, `daemon`, and `droppedWarnFrequency`)

In order to guarantees that logged messages have had a chance to be processed by asynchronous appenders (including the TCP appender) and ensure background threads have been stopped, you'll need to [cleanly shut down logback](http://logback.qos.ch/manual/configuration.html#stopContext) when your application exits.

### Appender Listeners

Listeners can be registered to an appender to receive notifications for the appender lifecycle and event processing.

See the two listener interfaces for the types of notifications that can be received:

* [`AppenderListener`](/src/main/java/net/logstash/logback/appender/listener/AppenderListener.java) - basic notifications for the [async appenders](#async-appenders) and [udp appender](#udp-appender).
* [`TcpAppenderListener`](/src/main/java/net/logstash/logback/appender/listener/TcpAppenderListener.java) - extension of `AppenderListener` with additional TCP-specific notifications.  Only works with the [TCP appenders](#tcp-appenders). 

Some example use cases for a listener are:

* Monitoring metrics for events per second, event processing durations, dropped events, connections successes / failures, etc.
* Reporting event processing errors to a different appender (that perhaps appends to a different destination).

To create a listener, create a new class that extends one of the `*ListenerImpl` classes or directly implements the `*Listener` interface.
Extending the `*ListenerImpl` class will have better backwards compatibilty in the future in case new methods are added to the interfaces.
(Logstash-logback-encoder still supports Java 7, so default interface methods cannot be used yet.)

Then register your listener class to an appender using the `listener` xml element like this:

```xml
  <appender name="stash" class="net.logstash.logback.appender.LogstashAccessTcpSocketAppender">
      ...

      <listener class="your.package.YourListenerClass">
          <yourListenerProperty>propertyValue</yourListenerProperty>
      </listener>
  </appender>
```

Multiple listeners can be registered by supplying multiple `listener` xml elements.


### Encoders / Layouts

You can use any of the encoders/layouts provided by the logstash-logback-encoder library with other logback appenders.

For example, to output LoggingEvents to a file, use the `LogstashEncoder`
with the `RollingFileAppender` in your `logback.xml` like this:

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

To log AccessEvents to a file, configure your `logback-access.xml` like this:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <appender name="stash" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>/some/path/to/your/file.log</file>
    <encoder class="net.logstash.logback.encoder.LogstashAccessEncoder" />
  </appender>

  <appender-ref ref="stash" />
</configuration>
```

The `LogstashLayout` and `LogstashAccessLayout` can be configured the same way as
the `LogstashEncoder` and `LogstashAccessEncoder`.  All the other examples
in this document use encoders, but the same options apply to the layouts as well.

To receive file input in logstash, configure a [`file`](http://www.logstash.net/docs/latest/inputs/file) input in logstash's configuration like this:

```
input {
  file {
    path => "/some/path/to/your/file.log"
    codec => "json"
  }
}
```



## LoggingEvent Fields

The following sections describe the fields included in the JSON output by default for LoggingEvents written by the

* `LogstashEncoder`
* `LogstashLayout`, and
* the logstash appenders

If you are using the [composite encoders/layouts](#composite-encoderlayout), then the fields written will
vary by the providers you configure.


### Standard Fields

These fields will appear in every LoggingEvent unless otherwise noted.
The field names listed here are the default field names.
The field names can be customized (see [Customizing Standard Field Names](#customizing-standard-field-names)).

| Field         | Description
|---------------|------------
| `@timestamp`  | Time of the log event. (`yyyy-MM-dd'T'HH:mm:ss.SSSZZ`)  See [customizing timestamp](#customizing-timestamp).
| `@version`    | Logstash format version (e.g. `1`)   See [customizing version](#customizing-version).
| `message`     | Formatted log message of the event
| `logger_name` | Name of the logger that logged the event
| `thread_name` | Name of the thread that logged the event
| `level`       | String name of the level of the event
| `level_value` | Integer value of the level of the event
| `stack_trace` | (Only if a throwable was logged) The stacktrace of the throwable.  Stackframes are separated by line endings.
| `tags`        | (Only if tags are found) The names of any markers not explicitly handled.  (e.g. markers from `MarkerFactory.getMarker` will be included as tags, but the markers from [`Markers`](/src/main/java/net/logstash/logback/marker/Markers.java) will not.)


### MDC fields

By default, each entry in the Mapped Diagnostic Context (MDC) (`org.slf4j.MDC`)
will appear as a field in the LoggingEvent.

This can be fully disabled by specifying `<includeMdc>false</includeMdc>`,
in the encoder/layout/appender configuration.

You can also configure specific entries in the MDC to be included or excluded as follows:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <includeMdcKeyName>key1ToInclude</includeMdcKeyName>
  <includeMdcKeyName>key2ToInclude</includeMdcKeyName>
</encoder>
```
or

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <excludeMdcKeyName>key1ToExclude</excludeMdcKeyName>
  <excludeMdcKeyName>key2ToExclude</excludeMdcKeyName>
</encoder>
```

When key names are specified for inclusion, then all other fields will be excluded.
When key names are specified for exclusion, then all other fields will be included.
It is a configuration error to specify both included and excluded key names.


### Context fields

By default, each property of Logback's Context (`ch.qos.logback.core.Context`)
will appear as a field in the LoggingEvent.
This can be disabled by specifying `<includeContext>false</includeContext>`
in the encoder/layout/appender configuration.

Note that logback versions prior to 1.1.10 included a `HOSTNAME` property by default in the context.
As of logback 1.1.10, the `HOSTNAME` property is lazily calculated (see [LOGBACK-1221](https://jira.qos.ch/browse/LOGBACK-1221)), and will no longer be included by default.


### Caller Info Fields
The encoder/layout/appender do not contain caller info by default.
This can be costly to calculate and should be switched off for busy production environments.

To switch it on, add the `includeCallerData` property to the configuration.
```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <includeCallerData>true</includeCallerData>
</encoder>
```
If the encoder is included inside an asynchronous appender, such as
`AsyncAppender`, `LoggingEventAsyncDisruptorAppender`, or `LogstashTcpSocketAppender`, then
`includeCallerData` must be set to true on the appender as well.

When switched on, the following fields will be included in the log event:

| Field                | Description
|----------------------|------------
| `caller_class_name`  | Fully qualified class name of the class that logged the event
| `caller_method_name` | Name of the method that logged the event
| `caller_file_name`   | Name of the file that logged the event
| `caller_line_number` | Line number of the file where the event was logged



### Custom Fields

In addition to the fields above, you can add other fields to the LoggingEvent either globally, or on an event-by-event basis.


#### Global Custom Fields

Add custom fields that will appear in every LoggingEvent like this :
```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <customFields>{"appname":"myWebservice","roles":["customerorder","auth"],"buildinfo":{"version":"Version 0.1.0-SNAPSHOT","lastcommit":"75473700d5befa953c45f630c6d9105413c16fe1"}}</customFields>
</encoder>
```

or in an AccessEvent like this :

```xml
<encoder class="net.logstash.logback.encoder.LogstashAccessEncoder">
  <customFields>{"appname":"myWebservice","roles":["customerorder","auth"],"buildinfo":{"version":"Version 0.1.0-SNAPSHOT","lastcommit":"75473700d5befa953c45f630c6d9105413c16fe1"}}</customFields>
</encoder>
```



#### Event-specific Custom Fields

When logging a message, you can add additional fields to the JSON output by using

* _structured arguments_ provided by
  [`StructuredArguments`](/src/main/java/net/logstash/logback/argument/StructuredArguments.java), OR
* _markers_ provided by
  [`Markers`](/src/main/java/net/logstash/logback/marker/Markers.java)

The difference between the two is that
* `StructuredArguments` are included in a the log event's formatted message
(when the message has a parameter for the argument) _AND_ in the JSON output.
  * `StructuredArguments` will be included in the JSON output if using `LogstashEncoder/Layout`
    or if using [composite encoders/layouts](#composite-encoderlayout) with the `arguments` provider.
* `Markers` are only written to the JSON output, and _NEVER_ to the log event's formatted message.
  * `Markers` will be included in the JSON output if using `LogstashEncoder/Layout`
    or if using [composite encoders/layouts](#composite-encoderlayout) with the `logstashMarkers` provider.


You can use `StructuredArguments` even if the message does not contain a parameter
for the argument.  However, in this case, the argument will only be written to the JSON output
and not the formatted message (which is effectively the same behavior that the Markers provide).
In general, you should use `StructuredArguments`, unless you have a static analyzer
that flags parameter count / argument count mismatches.

Both `StructuredArguments` and `Markers` require constructing additional objects.
Therefore, it is best practice to surround the log lines with `logger.isXXXEnabled()`,
to avoid the object construction if the log level is disabled.

Examples using `StructuredArguments`:

```java
import static net.logstash.logback.argument.StructuredArguments.*

/*
 * Add "name":"value" to the JSON output,
 * but only add the value to the formatted message.
 *
 * The formatted message will be `log message value`
 */
logger.info("log message {}", value("name", "value"));

/*
 * Add "name":"value" to the JSON output,
 * and add name=value to the formatted message.
 *
 * The formatted message will be `log message name=value`
 */
logger.info("log message {}", keyValue("name", "value"));

/*
 * Add "name":"value" ONLY to the JSON output.
 *
 * Since there is no parameter for the argument,
 * the formatted message will NOT contain the key/value.
 *
 * If this looks funny to you or to static analyzers,
 * consider using Markers instead.
 */
logger.info("log message", keyValue("name", "value"));

/*
 * Add multiple key value pairs to both JSON and formatted message
 */
logger.info("log message {} {}", keyValue("name1", "value1"), keyValue("name2", "value2")));

/*
 * Add "name":"value" to the JSON output and
 * add name=[value] to the formatted message using a custom format.
 */
logger.info("log message {}", keyValue("name", "value", "{0}=[{1}]"));

/*
 * In the JSON output, values will be serialized by Jackson's ObjectMapper.
 * In the formatted message, values will follow the same behavior as logback
 * (formatting of an array or if not an array `toString()` is called).
 *
 * Add "foo":{...} to the JSON output and add `foo.toString()` to the formatted message:
 *
 * The formatted message will be `log message <result of foo.toString()>`
 */
Foo foo  = new Foo();
logger.info("log message {}", value("foo", foo));

/*
 * Add "name1":"value1","name2":"value2" to the JSON output by using a Map,
 * and add `myMap.toString()` to the formatted message.
 *
 * Note the values can be any object that can be serialized by Jackson's ObjectMapper
 * (e.g. other Maps, JsonNodes, numbers, arrays, etc)
 */
Map myMap = new HashMap();
myMap.put("name1", "value1");
myMap.put("name2", "value2");
logger.info("log message {}", entries(myMap));

/*
 * Add "array":[1,2,3] to the JSON output,
 * and array=[1,2,3] to the formatted message.
 */
logger.info("log message {}", array("array", 1, 2, 3));

/*
 * Add fields of any object that can be unwrapped by Jackson's UnwrappableBeanSerializer to the JSON output.
 * i.e. The fields of an object can be written directly into the JSON output.
 * This is similar to the @JsonUnwrapped annotation.
 *
 * The formatted message will contain `myobject.toString()`
 */
logger.info("log message {}", fields(myobject));

/*
 * In order to normalize a field object name, static helper methods can be created.
 * For example, `foo(Foo)` calls `value("foo" , foo)`
 */
logger.info("log message {}", foo(foo));

```

Abbreviated convenience methods are available for all the structured argument types.
For example, instead of `keyValue(key, value)`, you can use `kv(key, value)`.



Examples using `Markers`:

```java
import static net.logstash.logback.marker.Markers.*

/*
 * Add "name":"value" to the JSON output.
 */
logger.info(append("name", "value"), "log message");

/*
 * Add "name1":"value1","name2":"value2" to the JSON output by using multiple markers.
 */
logger.info(append("name1", "value1").and(append("name2", "value2")), "log message");

/*
 * Add "name1":"value1","name2":"value2" to the JSON output by using a map.
 *
 * Note the values can be any object that can be serialized by Jackson's ObjectMapper
 * (e.g. other Maps, JsonNodes, numbers, arrays, etc)
 */
Map myMap = new HashMap();
myMap.put("name1", "value1");
myMap.put("name2", "value2");
logger.info(appendEntries(myMap), "log message");

/*
 * Add "array":[1,2,3] to the JSON output
 */
logger.info(appendArray("array", 1, 2, 3), "log message");

/*
 * Add "array":[1,2,3] to the JSON output by using raw json.
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




## AccessEvent Fields

The following sections describe the fields included in the JSON output by default for AccessEvents written by the

* `LogstashAccessEncoder`,
* `LogstashAccessLayout`, and
* the logstash access appenders.

If you are using the [composite encoders/layouts](#composite-encoderlayout), then the fields written will
vary by the providers you configure.



### Standard Fields

These fields will appear in every AccessEvent unless otherwise noted.
The field names listed here are the default field names.
The field names can be customized (see [Customizing Standard Field Names](#customizing-standard-field-names)).

| Field         | Description
|---------------|------------
| `@timestamp`  | Time of the log event. (`yyyy-MM-dd'T'HH:mm:ss.SSSZZ`)  See [customizing timestamp](#customizing-timestamp).
| `@version`    | Logstash format version (e.g. `1`)   See [customizing version](#customizing-version).
| `message`     | Message in the form `${remoteHost} - ${remoteUser} [${timestamp}] "${requestUrl}" ${statusCode} ${contentLength}`
| `method` | HTTP method
| `protocol` | HTTP protocol
| `status_code` | HTTP status code
| `requested_url` | Request URL
| `requested_uri` | Request URI
| `remote_host` | Remote host
| `remote_user` | Remote user
| `content_length` | Content length
| `elapsed_time` | Elapsed time in millis


### Header Fields

Request and response headers are not logged by default, but can be enabled by specifying a field name for them, like this:

```xml
<encoder class="net.logstash.logback.encoder.LogstashAccessEncoder">
  <fieldNames>
    <requestHeaders>request_headers</requestHeaders>
    <responseHeaders>response_headers</responseHeaders>
  </fieldNames>
</encoder>
```

See [Customizing Standard Field Names](#customizing-standard-field-names)) for more details.

To write the header names in lowercase (so that header names that only differ by case are treated the same),
set `lowerCaseFieldNames` to true, like this:

```xml
<encoder class="net.logstash.logback.encoder.LogstashAccessEncoder">
  <fieldNames>
    <requestHeaders>request_headers</requestHeaders>
    <responseHeaders>response_headers</responseHeaders>
  </fieldNames>
  <lowerCaseHeaderNames>true</lowerCaseHeaderNames>
</encoder>
```

Headers can be filtered via configuring the `requestHeaderFilter` and/or the `responseHeaderFilter`
with a [`HeaderFilter`](/src/main/java/net/logstash/logback/composite/accessevent/HeaderFilter.java), such as the
[`IncludeExcludeHeaderFilter`](/src/main/java/net/logstash/logback/composite/accessevent/IncludeExcludeHeaderFilter.java).

The `IncludeExcludeHeaderFilter` can be configured like this:
 
```xml
<encoder class="net.logstash.logback.encoder.LogstashAccessEncoder">
  <fieldNames>
    <requestHeaders>request_headers</requestHeaders>
  </fieldNames>
  <requestHeaderFilter>
    <include>Content-Type</include>
  </requestHeaderFilter>
</encoder>

Custom filters implementing [`HeaderFilter`](/src/main/java/net/logstash/logback/composite/accessevent/HeaderFilter.java)
can be used by specifying the filter class like this:

```xml
  <requestHeaderFilter class="your.package.YourFilterClass"/>
```

## Customizing Standard Field Names

The standard field names above for LoggingEvents and AccessEvents can be customized by using the `fieldNames`configuration element in the encoder or appender configuration.

For example:
```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <fieldNames>
    <timestamp>time</timestamp>
    <message>msg</message>
    ...
  </fieldNames>
</encoder>
```
Prevent a field from being output by setting the field name to `[ignore]`.

For LoggingEvents, see [`LogstashFieldNames`](/src/main/java/net/logstash/logback/fieldnames/LogstashFieldNames.java)
for all the field names that can be customized.  Each java field name in that class is the name of the xml element that you would use to specify the field name (e.g. `logger`, `levelValue`).  Additionally, a separate set of [shortened field names](/src/main/java/net/logstash/logback/fieldnames/ShortenedFieldNames.java) can be configured like this:
```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <fieldNames class="net.logstash.logback.fieldnames.ShortenedFieldNames"/>
</encoder>
```

For LoggingEvents, log the caller info, MDC properties, and context properties
in sub-objects within the JSON event by specifying field
names for `caller`, `mdc`, and `context`, respectively.

For AccessEvents, see [`LogstashAccessFieldNames`](/src/main/java/net/logstash/logback/fieldnames/LogstashAccessFieldNames.java)
for all the field names that can be customized. Each java field name in that class is the name of the xml element that you would use to specify the field name (e.g. `fieldsMethod`, `fieldsProtocol`).



## Customizing Version

The version field value by default is the string value `1`.

The value can be changed like this:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <version>2</version>
</encoder>
```

The value can be written as a number (instead of a string) like this:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <writeVersionAsInteger>true</writeVersionAsInteger>
</encoder>
```


## Customizing Timestamp

By default, timestamps are written as string values in the format `yyyy-MM-dd'T'HH:mm:ss.SSSZZ` (e.g. `2018-04-28T22:23:59.164-07:00`), in the default TimeZone of the host Java platform.

You can change the timezone like this:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <timeZone>UTC</timeZone>
</encoder>
```

The value of the `timeZone` element can be any string accepted by java's  `TimeZone.getTimeZone(String id)` method.

You can change the pattern used like this:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <timestampPattern>yyyy-MM-dd'T'HH:mm:ss.SSS</timestampPattern>
</encoder>
```

Use these timestamp pattern values to output the timestamp as a unix timestamp (number of milliseconds since unix epoch).

* `[UNIX_TIMESTAMP_AS_NUMBER]` - write the timestamp value as a numeric unix timestamp
* `[UNIX_TIMESTAMP_AS_STRING]` - write the timestamp value as a string verion of the numeric unix timestamp

For example:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <timestampPattern>[UNIX_TIMESTAMP_AS_NUMBER]</timestampPattern>
</encoder>
```


## Customizing JSON Factory and Generator

The `JsonFactory` and `JsonGenerator` used to serialize output can be customized by
instances of [`JsonFactoryDecorator`](/src/main/java/net/logstash/logback/decorate/JsonFactoryDecorator.java)
or [`JsonGeneratorDecorator`](/src/main/java/net/logstash/logback/decorate/JsonGeneratorDecorator.java), respectively.

For example, you could enable pretty printing by using the
[PrettyPrintingJsonGeneratorDecorator](/src/main/java/net/logstash/logback/decorate/PrettyPrintingJsonGeneratorDecorator.java)

Or customize object mapping like this:

```java
public class ISO8601DateDecorator implements JsonFactoryDecorator  {

	@Override
	public MappingJsonFactory decorate(MappingJsonFactory factory) {
		ObjectMapper codec = factory.getCodec();
		codec.setDateFormat(new ISO8601DateFormat());
		return factory;
	}
}
```
and then specify the decorator in the logback.xml file like this:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <jsonGeneratorDecorator class="net.logstash.logback.decorate.PrettyPrintingJsonGeneratorDecorator"/>
  <jsonFactoryDecorator class="your.package.ISO8601DateDecorator"/>
</encoder>
```

See the [net.logstash.logback.decorate](/src/main/java/net/logstash/logback/decorate) package for other decorators.

## Customizing Character Escapes

By default, when a string is written as a JSON string value, any character not allowed in a JSON string will be escaped.
For example, the newline character (ASCII 10) will be escaped as `\n`.

To customize these escape sequences, use the `net.logstash.logback.decorate.CharacterEscapesJsonFactoryDecorator`.

For example, if you want to use something other than `\n` as the escape sequence for the newline character, you can do the following:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <jsonFactoryDecorator class="net.logstash.logback.decorate.CharacterEscapesJsonFactoryDecorator">
    <escape>
      <targetCharacterCode>10</targetCharacterCode>
      <escapeSequence>\u2028</escapeSequence>
    </escape>
  </jsonFactoryDecorator>
</encoder>
```

You can also disable all the default escape sequences by specifying `<includeStandardAsciiEscapesForJSON>false</includeStandardAsciiEscapesForJSON>` on the `CharacterEscapesJsonFactoryDecorator`.
If you do this, then you will need to register custom escapes for each character that is illegal in JSON string values.  Otherwise, invalid JSON could be written.


## Customizing Logger Name Length

For LoggingEvents, you can shorten the logger name field length similar to the normal pattern style of `%logger{36}`.
Examples of how it is shortened can be found [here](http://logback.qos.ch/manual/layouts.html#conversionWord)

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <shortenedLoggerNameLength>36</shortenedLoggerNameLength>
</encoder>
```


## Customizing Stack Traces

For LoggingEvents, stack traces are formatted using logback's `ExtendedThrowableProxyConverter` by default.
However, you can configure the encoder to use any `ThrowableHandlingConverter`
to format stacktraces.

A powerful [`ShortenedThrowableConverter`](/src/main/java/net/logstash/logback/stacktrace/ShortenedThrowableConverter.java)
is included in the logstash-logback-encoder library to format stacktraces by:

* Limiting the number of stackTraceElements per throwable (applies to each individual throwable.  e.g. caused-bys and suppressed)
* Limiting the total length in characters of the trace
* Abbreviating class names
* Filtering out consecutive unwanted stackTraceElements based on regular expressions.
* Using evaluators to determine if the stacktrace should be logged.
* Outputing in either 'normal' order (root-cause-last), or root-cause-first.
* Computing and inlining hexadecimal hashes for each exception stack ([more info](stack-hash.md)).

For example:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
    <maxDepthPerThrowable>30</maxDepthPerThrowable>
    <maxLength>2048</maxLength>
    <shortenedClassNameLength>20</shortenedClassNameLength>
    <exclude>sun\.reflect\..*\.invoke.*</exclude>
    <exclude>net\.sf\.cglib\.proxy\.MethodProxy\.invoke</exclude>
    <evaluator class="myorg.MyCustomEvaluator"/>
    <rootCauseFirst>true</rootCauseFirst>
    <inlineHash>true</inlineHash>
  </throwableConverter>
</encoder>
```

[`ShortenedThrowableConverter`](/src/main/java/net/logstash/logback/stacktrace/ShortenedThrowableConverter.java)
can even be used within a `PatternLayout` to format stacktraces in any non-JSON logs you may have.


## Prefix/Suffix

You can specify a prefix (written before the JSON object) and/or suffix (written after the JSON object),
which may be required for the log pipeline you are using, such as:

* If you are using the Common Event Expression (CEE) format for syslog, you need to add the `@cee:` prefix.
* If you are using other syslog destinations, you might need to add the standard syslog headers.
* If you are using Loggly, you might need to add your customer token.

For example, to add standard syslog headers for syslog over UDP, configure the following:

```xml
<configuration>
  <conversionRule conversionWord="syslogStart" converterClass="ch.qos.logback.classic.pattern.SyslogStartConverter"/>

  <appender name="stash" class="net.logstash.logback.appender.LogstashSocketAppender">
    <host>MyAwesomeSyslogServer</host>
    <!-- port is optional (default value shown) -->
    <port>514</port>
    <prefix class="ch.qos.logback.classic.PatternLayout">
      <pattern>%syslogStart{USER}</pattern>
    </prefix>
  </appender>

  ...
</configuration>
```

When using the `LogstashEncoder`, `LogstashAccessEncoder` or a composite encoder, the prefix is an `Encoder`, not a `Layout`, so you will need to wrap the prefix `PatternLayout` in a `LayoutWrappingEncoder` like this:

```xml
<configuration>
  ...
  <appender ...>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      ...
      <prefix class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
        <layout class="ch.qos.logback.classic.PatternLayout">
          <pattern>@cee:</pattern>
        </layout>
      </prefix>    
    </encoder>
  </appender>
</configuration>
```

Note that logback's xml configuration reader will [trim whitespace from xml element values](https://github.com/qos-ch/logback/blob/c2dcbfcfb4048d11d7e81cd9220efbaaccf931fa/logback-core/src/main/java/ch/qos/logback/core/joran/event/BodyEvent.java#L27-L37).  Therefore, if you want to end the prefix or suffix pattern with whitespace, first add the whitespace, and then add something like `%mdc{keyThatDoesNotExist}` after it.  For example `<pattern>your pattern %mdc{keyThatDoesNotExist}</pattern>`.  This will cause logback to output the whitespace as desired, and then a blank string for the MDC key that does not exist.


## Composite Encoder/Layout

If you want greater flexibility in the JSON format and data included in LoggingEvents and AccessEvents, use the [`LoggingEventCompositeJsonEncoder`](/src/main/java/net/logstash/logback/encoder/LoggingEventCompositeJsonEncoder.java)  and  [`AccessEventCompositeJsonEncoder`](/src/main/java/net/logstash/logback/encoder/AccessEventCompositeJsonEncoder.java)  (or the corresponding layouts).

These encoders/layouts are composed of one or more JSON _providers_ that contribute to the JSON output.  No providers are configured by default in the composite encoders/layouts.  You must add the ones you want.

For example:

```xml
<encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
  <providers>
    <mdc/>
    <pattern>
      <pattern>
        {
          "timestamp": "%date{ISO8601}",
          "myCustomField": "fieldValue",
          "relative": "#asLong{%relative}"
        }
      </pattern>
    </pattern>
    <stackTrace>
      <throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
        <maxDepthPerThrowable>30</maxDepthPerThrowable>
        <maxLength>2048</maxLength>
        <shortenedClassNameLength>20</shortenedClassNameLength>
        <exclude>^sun\.reflect\..*\.invoke</exclude>
        <exclude>^net\.sf\.cglib\.proxy\.MethodProxy\.invoke</exclude>
        <evaluator class="myorg.MyCustomEvaluator"/>
        <rootCauseFirst>true</rootCauseFirst>
      </throwableConverter>
    </stackTrace>
  </providers>
</encoder>
```


The logstash-logback-encoder library contains many providers out-of-the-box,
and you can even plug-in your own by extending `JsonProvider`.
Each provider has its own configuration options to further customize it.


#### Providers for LoggingEvents

For LoggingEvents, the available providers and their configuration properties (defaults in parenthesis) are as follows:

<table>
  <tbody>
    <tr>
      <th>Provider</th>
      <th>Description/Properties</th>
    </tr>
    <tr>
      <td><tt>timestamp</tt></td>
      <td><p>Event timestamp</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>@timestamp</tt>)</li>
          <li><tt>pattern</tt> - Output format (<tt>yyyy-MM-dd'T'HH:mm:ss.SSSZZ</tt>)
          <ul>
            <li>If set to <tt>[UNIX_TIMESTAMP_AS_NUMBER]</tt>, then the timestamp will be written as a numeric unix timestamp value</li>
            <li>If set to <tt>[UNIX_TIMESTAMP_AS_STRING]</tt>, then the timestamp will be written as a string unix timestamp value</li>
          </ul>
          </li>
          <li><tt>timeZone</tt> - Timezone (local timezone)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>version</tt></td>
      <td><p>Logstash JSON format version</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>@version</tt>)</li>
          <li><tt>version</tt> - Output value (<tt>1</tt>)</li>
          <li><tt>writeAsInteger</tt> - Write the version as a integer value (<tt>false</tt> = write as a string value)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>message</tt></td>
      <td><p>Formatted log event message</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>message</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>rawMessage</tt></td>
      <td><p>Raw log event message, as opposed to formatted log where parameters are resolved</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>raw_message</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>loggerName</tt></td>
      <td><p>Name of the logger that logged the message</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>logger_name</tt>)</li>          
          <li><tt>shortenedLoggerNameLength</tt> - Length to which the name will be attempted to be abbreviated (no abbreviation)</li>          
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>threadName</tt></td>
      <td><p>Name of the thread from which the event was logged</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>thread_name</tt>)</li>          
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>logLevel</tt></td>
      <td><p>Logger level text (INFO, WARN, etc)</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>level</tt>)</li>          
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>logLevelValue</tt></td>
      <td><p>Logger level numerical value </p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>level_value</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>callerData</tt></td>
      <td><p>Outputs data about from where the logger was called (class/method/file/line)
        <ul>
          <li><tt>fieldName</tt> - Sub-object field name (no sub-object)</li>
          <li><tt>classFieldName</tt> - Field name for class name (<tt>caller_class_name</tt>)</li>
          <li><tt>methodFieldName</tt> - Field name for method name (<tt>caller_method_name</tt>)</li>
          <li><tt>fileFieldName</tt> - Field name for file name (<tt>caller_file_name</tt>)</li>
          <li><tt>lineFieldName</tt> - Field name for lin number (<tt>caller_line_number</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>stackTrace</tt></td>
      <td><p>Stacktrace of any throwable logged with the event.  Stackframes are separated by newline chars.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>stack_trace</tt>)</li>
          <li><tt>throwableConverter</tt> - The <tt>ThrowableHandlingConverter</tt> to use to format the stacktrace (<tt>stack_trace</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>stackHash</tt></td>
      <td><p>(Only if a throwable was logged) Computes and outputs a hexadecimal hash of the throwable stack.</p>
        <p>This helps identifying several occurrences of the same error (<a href="stack-hash.md">more info</a>).</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>stack_hash</tt>)</li>
          <li><tt>exclude</tt> - Regular expression pattern matching <i>stack trace elements</i> to exclude when computing the error hash</li>
          <li><tt>exclusions</tt> - Coma separated list of regular expression patterns matching <i>stack trace elements</i> to exclude when computing the error hash</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>throwableClassName</tt></td>
      <td><p>(Only if a throwable was logged) Outputs a field that contains the class name of the thrown Throwable.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>throwable_class</tt>)</li>
          <li><tt>useSimpleClassName</tt> - When true, the throwable's simple class name will be used.  When false, the fully qualified class name will be used. (<tt>true</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>throwableRootCauseClassName</tt></td>
      <td><p>(Only if a throwable was logged) Outputs a field that contains the class name of the root cause of the thrown Throwable.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>throwable_root_cause_class</tt>)</li>
          <li><tt>useSimpleClassName</tt> - When true, the throwable's simple class name will be used.  When false, the fully qualified class name will be used. (<tt>true</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>context</tt></td>
      <td><p>Outputs entries from logback's context</p>
        <ul>
          <li><tt>fieldName</tt> - Sub-object field name (no sub-object)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>contextName</tt></td>
      <td><p>Outputs the name of logback's context</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>context</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>mdc</tt></td>
      <td>
        <p>Outputs entries from the Mapped Diagnostic Context (MDC).
           Will include all entries by default.
           When key names are specified for inclusion, then all other fields will be excluded.
           When key names are specified for exclusion, then all other fields will be included.
           It is a configuration error to specify both included and excluded key names.
        </p>
        <ul>
          <li><tt>fieldName</tt> - Sub-object field name (no sub-object)</li>
          <li><tt>includeMdcKeyName</tt> - Name of keys to include (all)</li>
          <li><tt>excludeMdcKeyName</tt> - Name of keys to include (none)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>tags</tt></td>
      <td><p>Outputs logback markers as a comma separated list</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>tags</tt>)</li>          
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>logstashMarkers</tt></td>
      <td><p>Used to output Logstash Markers as specified in <em>Event-specific Custom Fields</em></p>
      </td>
    </tr>
    <tr>
      <td><tt>nestedField</tt></td>
      <td>
        <p>Nests a JSON object under the configured fieldName.</p>
        <p>The nested object is populated by other providers added to this provider.</p>
        <p>See <a href="#provider_nested">Nested JSON provider</a></p>
        <ul>
          <li><tt>fieldName</tt> - Output field name</li>
          <li><tt>providers</tt> - The providers that should populate the nested object.</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>pattern</tt></td>
      <td>
        <p>Outputs fields from a configured JSON Object string,
           while substituting patterns supported by logback's <tt>PatternLayout</tt>.
        </p>
        <p>
           See <a href="#provider_pattern">Pattern JSON Provider</a>
        </p>
        <ul>
          <li><tt>pattern</tt> - JSON object string (no default)</li>          
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>arguments</tt></td>
      <td>
        <p>Outputs fields from the event arguments array.
        </p>
        <p>
            See <a href="#loggingevent_custom_event">Event-specific Custom Fields</a>
        </p>
        <ul>
          <li><tt>fieldName</tt> - Sub-object field name (no sub-object)</li>
          <li><tt>includeNonStructuredArguments</tt> - Include arguments that are not an instance
          of <a href="/src/main/java/net/logstash/logback/argument/StructuredArgument.java"><tt>StructuredArgument</tt></a>.
          (default=false)
          Object field name will be <tt>nonStructuredArgumentsFieldPrefix</tt> prepend to the argument index</li>
          <li><tt>nonStructuredArgumentsFieldPrefix</tt> - Object field name prefix (default=arg)</li>
        </ul>
      </td>
    </tr>   
    <tr>
      <td><tt>uuid</tt></td>
      <td><p>Outputs random UUID as field value. Handy when you want to provide unique identifier
      for log lines
      </p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>uuid</tt>)</li>
          <li><tt>strategy</tt> - UUID generation strategy (<tt>random</tt>). Supported options: <ul><li><tt>random</tt> - for Type 4 UUID</li>
          <li><tt>time</tt> - for Type 1 time based UUID</li>
          </ul></li>
          <li><tt>ethernet</tt> - Only for 'time' strategy. When defined - MAC address to use for location part of UUID. Set it to <tt>interface</tt> value to use real underlying network interface or to specific values like <tt>00:C0:F0:3D:5B:7C</tt></li>          
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>sequence</tt></td>
      <td>
        <p>
          Outputs an incrementing sequence number for every log event.
          Useful for tracking pottential message loss during transport (eg. UDP)
        </p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>sequence</tt>)</li></ul>
      </td>
    </tr>
  </tbody>
</table>



#### Providers for AccessEvents  

For AccessEvents, the available providers and their configuration properties (defaults in parenthesis) are as follows:



<table>
  <tbody>
    <tr>
      <th>Provider</th>
      <th>Description/Properties</th>
    </tr>
    <tr>
      <td><tt>timestamp</tt></td>
      <td><p>Event timestamp</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>@timestamp</tt>)</li>
          <li><tt>pattern</tt> - Output format (<tt>yyyy-MM-dd'T'HH:mm:ss.SSSZZ</tt>)
          <ul>
            <li>If set to <tt>[UNIX_TIMESTAMP_AS_NUMBER]</tt>, then the timestamp will be written as a numeric unix timestamp value</li>
            <li>If set to <tt>[UNIX_TIMESTAMP_AS_STRING]</tt>, then the timestamp will be written as a string unix timestamp value</li>
          </ul>
          </li>
          <li><tt>timeZone</tt> - Timezone (local timezone)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>version</tt></td>
      <td><p>Logstash JSON format version</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>@version</tt>)</li>
          <li><tt>version</tt> - Output value (<tt>1</tt>)</li>
          <li><tt>writeAsInteger</tt> - Write the version as a integer value (<tt>false</tt> = write as a string value)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>message</tt></td>
      <td><p>Message in the form `${remoteHost} - ${remoteUser} [${timestamp}] "${requestUrl}" ${statusCode} ${contentLength}`</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>message</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>method</tt></td>
      <td><p>HTTP method</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>method</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>protocol</tt></td>
      <td><p>HTTP protocol</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>protocol</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>statusCode</tt></td>
      <td><p>HTTP status code</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>status_code</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>requestedUrl</tt></td>
      <td><p>Requested URL</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>requested_url</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>requestedUri</tt></td>
      <td><p>Requested URI</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>requested_uri</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>remoteHost</tt></td>
      <td><p>Remote Host</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>remote_host</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>remoteUser</tt></td>
      <td><p>Remote User</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>remote_user</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>contentLength</tt></td>
      <td><p>Content length</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>content_length</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>elapsedTime</tt></td>
      <td><p>Elapsed time in milliseconds</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>elapsed_time</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>requestHeaders</tt></td>
      <td><p>Include the request headers</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (no default, must be provided)</li>
          <li><tt>lowerCaseHeaderNames</tt> - Write header names in lower case (<tt>false</tt>)</li>
          <li><tt>filter</tt> - A filter to determine which headers to include/exclude.
          See <a href="/src/main/java/net/logstash/logback/composite/accessevent/HeaderFilter.java"><tt>HeaderFilter</tt></a>
          and <a href="/src/main/java/net/logstash/logback/composite/accessevent/IncludeExcludeHeaderFilter.java"><tt>IncludeExcludeHeaderFilter</tt></a></li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>responseHeaders</tt></td>
      <td><p>Include the response headers</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (no default, must be provided)</li>
          <li><tt>lowerCaseHeaderNames</tt> - Write header names in lower case (<tt>false</tt>)</li>
          <li><tt>filter</tt> - A filter to determine which headers to include/exclude.
          See <a href="/src/main/java/net/logstash/logback/composite/accessevent/HeaderFilter.java"><tt>HeaderFilter</tt></a>
          and <a href="/src/main/java/net/logstash/logback/composite/accessevent/IncludeExcludeHeaderFilter.java"><tt>IncludeExcludeHeaderFilter</tt></a></li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>nestedField</tt></td>
      <td>
        <p>Nests a JSON object under the configured fieldName.</p>
        <p>The nested object is populated by other providers added to this provider.</p>
        <p>See <a href="#provider_nested">Nested JSON provider</a></p>
        <ul>
          <li><tt>fieldName</tt> - Output field name</li>
          <li><tt>providers</tt> - The providers that should populate the nested object.</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><tt>pattern</tt></td>
      <td>
        <p>Outputs fields from a configured JSON Object string,
           while substituting patterns supported by logback access's <tt>PatternLayout</tt>.
        </p>
        <p>
           See <a href="#provider_pattern">Pattern JSON Provider</a>
        </p>
        <ul>
          <li><tt>pattern</tt> - JSON object string (no default)</li>          
        </ul>
      </td>
    </tr>
  </tbody>
</table>


### Nested JSON Provider

Use the `nestedField` provider to create a sub-object in the JSON event output.

For example...

```
<encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
  <providers>
    <timestamp/>
    <nestedField>
      <fieldName>fields</fieldName>
      <providers>
        <logLevel/>
      </providers>
    </nestedField>
  </providers>
</encoder>
```

...will produce something like...

```
{
  "@timestamp":"...",
  "fields":{
    "level": "DEBUG"
  }
}
```


### Pattern JSON Provider

When used with a composite JSON encoder/layout, the `pattern` JSON provider can be used to
define a template for a portion of the logged JSON output.
The encoder/layout will populate values within the template.
Every value in the template is treated as a pattern for logback's standard `PatternLayout` so it can be a combination
of literal strings (for some constants) and various conversion specifiers (like `%d` for date).

The pattern string (configured within the pattern provider) must be a JSON Object.
The contents of the JSON object are included within the logged JSON output.

This example...

```xml
<encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
  <providers>
    <!-- provides the timestamp -->
    <timestamp/>

    <!-- provides the version -->
    <version/>

    <!-- provides the fields in the configured pattern -->
    <pattern>
      <!-- the pattern that defines what to include -->
      <pattern>
        { "level": "%level" }
      </pattern>
    </pattern>
  </providers>
</encoder>
```
... will produce something like...

```
{
  "@timestamp":"...",
  "@version": "1",
  "level": "DEBUG"
}
```

The real power comes from the fact that there are lots of standard conversion specifiers so you
can customise what is logged and how. For example, you could log a single specific value from MDC with `%mdc{mykey}`.
Or, for access logs, you could log a single request header with `%i{User-Agent}`.

You can use nested objects and arrays in your pattern.

If you use a null, number, or a boolean constant in a pattern, it will keep its type in the
resulting JSON. However, only the text values are searched for conversion patterns.
And, as these patterns are sent through `PatternLayout`, the result is always a string
even for something which you may feel should be a number - like for `%b` (bytes sent, in access logs).

You can either deal with the type conversion on the logstash side or you may use special operations provided by this encoder.
The operations are:

* `#asLong{...}` - evaluates the pattern in curly braces and then converts resulting string to a long (or a null if conversion fails).
* `#asDouble{...}` - evaluates the pattern in curly braces and then converts resulting string to a double (or a null if conversion fails).
* `#asJson{...}` - evaluates the pattern in curly braces and then converts resulting string to json (or a null if conversion fails).
* `#tryJson{...}` - evaluates the pattern in curly braces and then converts resulting string to json (or just the string if conversion fails).

So this example...

```xml
<pattern>
  {
    "line_str": "%line",
    "line_long": "#asLong{%line}",
    "has_message": "#asJson{%mdc{hasMessage}}",
    "json_message": "#asJson{%message}"
  }
</pattern>
```

... And this logging code...

```java
MDC.put("hasMessage", "true");
LOGGER.info("{\"type\":\"example\",\"msg\":\"example of json message with type\"}");
```

...will produce something like...

```
{
  "line_str":"97",
  "line_long":97,
  "has_message":true,
  "json_message":{"type":"example","msg":"example of json message with type"}
}
```

Note that the value that is sent for `line_long` is a number even though in your pattern it is a quoted text.
And the json_message field value is a json object, not a string.


#### LoggingEvent patterns

For LoggingEvents, patterns from logback-classic's
[`PatternLayout`](http://logback.qos.ch/manual/layouts.html#conversionWord) are supported.

For example:

```xml
<encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
  <providers>
    <timestamp/>
    <pattern>
      <pattern>
        {
        "custom_constant": "123",
        "tags": ["one", "two"],
        "logger": "%logger",
        "level": "%level",
        "thread": "%thread",
        "message": "%message",
...
        }
      </pattern>
    </pattern>
  </providers>
</encoder>
```


#### AccessEvent patterns

For AccessEvents, patterns from logback-access's
[`PatternLayout`](http://logback.qos.ch/xref/ch/qos/logback/access/PatternLayout.html) are supported.

For example:  

```xml
<encoder class="net.logstash.logback.encoder.AccessEventCompositeJsonEncoder">
  <providers>
    <pattern>
      <pattern>
        {
        "custom_constant": "123",
        "tags": ["one", "two"],
        "remote_ip": "%a",
        "status_code": "%s",
        "elapsed_time": "%D",
        "user_agent": "%i{User-Agent}",
        "accept": "%i{Accept}",
        "referer": "%i{Referer}",
        "session": "%requestCookie{JSESSIONID}",
...
        }
      </pattern>
    </pattern>
  </providers>
</encoder>
```

There is also a special operation that can be used with this AccessEvents:

* `#nullNA{...}` - if the pattern in curly braces evaluates to a dash ("-"), it will be replaced with a null value.

You may want to use it because many of the `PatternLayout` conversion specifiers from logback-access will evaluate to "-"
for non-existent value (for example for a cookie, header or a request attribute).

So the following pattern...

```xml
<pattern>
  {
    "default_cookie": "%requestCookie{MISSING}",
    "filtered_cookie": "#nullNA{%requestCookie{MISSING}}"
  }
</pattern>
```

...will produce...

```
{
  "default_cookie": "-",
  "filtered_cookie": null
}
```

### Custom JSON Provider

You can create your own JSON provider by implementing the [`JsonProvider`](src/main/java/net/logstash/logback/composite/JsonProvider.java) interface (or extending one of the existing classes that implements the `JsonProvider` interface).

Then, add the provider to a `LoggingEventCompositeJsonEncoder` like this:

```xml
<encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
  <providers>
    ...
    <provider class="your.provider.YourJsonProvider">
        <!-- Any properties exposed by your provider can be set here -->
    </provider>
    ...
  </providers>
</encoder>
```

or a `LogstashEncoder` like this:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    ...
    <provider class="your.provider.YourJsonProvider">
        <!-- Any properties exposed by your provider can be set here -->
    </provider>
    ...
</encoder>
```

You can do something similar for `AccessEventCompositeJsonEncoder` and `LogstashAccessEnceder` as well.

## Debugging

During execution, the encoders/appenders/layouts provided in logstash-logback-encoder
will add logback status messages to the logback `StatusManager`.

By default, logback only shows WARN/ERROR status messages on the console during configuration.
No messages are output during actual operation (even if they are WARN/ERROR).

If you are having trouble identifying causes of problems (e.g. events are not getting delivered),
then you can enable logback debugging or add a status listener as specified in
the [logback manual](http://logback.qos.ch/manual/configuration.html#automaticStatusPrinting).

### Profiling

<a href="http://www.yourkit.com/java/profiler/index.jsp"><img src="http://www.yourkit.com/images/yklogo.png" alt="YourKit Logo" height="22"/></a>

Memory usage and performance of logstash-logback-encoder have been improved
by addressing issues discovered with the help of the
[YourKit Java Profiler](http://www.yourkit.com/java/profiler/index.jsp).

YourKit, LLC has graciously donated a free license of the
[YourKit Java Profiler](http://www.yourkit.com/java/profiler/index.jsp)
to this open source project.


## Build status
[![Build Status](https://travis-ci.org/logstash/logstash-logback-encoder.svg?branch=master)](https://travis-ci.org/logstash/logstash-logback-encoder)
