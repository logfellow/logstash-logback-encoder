> !! This document applies to the next version under development.
>
> &nbsp; &nbsp; See [here for documentation on the latest released version](https://github.com/logfellow/logstash-logback-encoder/tree/logstash-logback-encoder-7.4).

# Logstash Logback Encoder

[![Build](https://github.com/logfellow/logstash-logback-encoder/workflows/build/badge.svg?branch=main)](https://github.com/logfellow/logstash-logback-encoder/actions)
[![Javadocs](http://www.javadoc.io/badge/net.logstash.logback/logstash-logback-encoder.svg)](http://www.javadoc.io/doc/net.logstash.logback/logstash-logback-encoder)
[![Maven Central](https://img.shields.io/maven-central/v/net.logstash.logback/logstash-logback-encoder)](https://search.maven.org/artifact/net.logstash.logback/logstash-logback-encoder)
[![Release Notes](https://img.shields.io/github/v/release/logfellow/logstash-logback-encoder?label=release%20notes)](https://github.com/logfellow/logstash-logback-encoder/releases/latest)

Provides [logback](http://logback.qos.ch/) encoders, layouts, and appenders to log in JSON and [other formats supported by Jackson](#data-format).

Supports both regular _LoggingEvents_ (logged through a `Logger`) and _AccessEvents_ (logged via [logback-access](http://logback.qos.ch/access.html)).

Originally written to support output in [logstash](https://www.elastic.co/guide/en/logstash/current)'s JSON format, but has evolved into a highly-configurable, general-purpose, structured logging mechanism for JSON and other Jackson dataformats.
The structure of the output, and the data it contains, is fully configurable.

#### Contents:

* [Including it in your project](#including-it-in-your-project)
* [Java Version Requirements](#java-version-requirements)
* [Usage](#usage)
	* [UDP Appenders](#udp-appenders)
	* [TCP Appenders](#tcp-appenders)
		* [Keep-alive](#keep-alive)
		* [Multiple Destinations](#multiple-destinations)
		* [Reconnection Delay](#reconnection-delay)
		* [Connection Timeout](#connection-timeout)
		* [Write Buffer Size](#write-buffer-size)
		* [Write Timeout](#write-timeout)
		* [Initial Send Delay](#initial-send-delay)
		* [SSL](#ssl)
	* [Async Appenders](#async-appenders)
		* [RingBuffer Size](#ringbuffer-size)
		* [RingBuffer Full](#ringbuffer-full)
		* [Graceful Shutdown](#graceful-shutdown)
		* [Wait Strategy](#wait-strategy)
	* [Appender Listeners](#appender-listeners)
	* [Encoders / Layouts](#encoders--layouts)
* [LoggingEvent Fields](#loggingevent-fields)
	* [Standard Fields](#standard-fields)
	* [MDC fields](#mdc-fields)
	* [Key Value Pair fields](#key-value-pair-fields)
	* [Context fields](#context-fields)
	* [Caller Info Fields](#caller-info-fields)
	* [Custom Fields](#custom-fields)
		* [Global Custom Fields](#global-custom-fields)
		* [Event-specific Custom Fields](#event-specific-custom-fields)
* [AccessEvent Fields](#accessevent-fields)
	* [Standard Fields](#standard-fields-1)
	* [Header Fields](#header-fields)
* [Customizing Jackson](#customizing-jackson)
	* [Data Format](#data-format)
	* [Customizing JSON Factory and Generator](#customizing-json-factory-and-generator)
	* [Registering Jackson Modules](#registering-jackson-modules)
	* [Customizing Character Escapes](#customizing-character-escapes)
* [Masking](#masking)
* [Customizing Standard Field Names](#customizing-standard-field-names)
* [Customizing Version](#customizing-version)
* [Customizing Timestamp](#customizing-timestamp)
* [Customizing LoggingEvent Message](#customizing-loggingevent-message)
* [Customizing AccessEvent Message](#customizing-accessevent-message)
* [Customizing Logger Name Length](#customizing-logger-name-length)
* [Customizing Stack Traces](#customizing-stack-traces)
    * [Omit Common Frames](#omit-common-frames)
    * [Truncate after Regex](#truncate-after-regex)
    * [Exclude Frames per Regex](#exclude-frames-per-regex)
    * [Maximum Depth per Throwable](#maximum-depth-per-throwable)
    * [Maximum Trace Size (bytes)](#maximum-trace-size)
    * [Classname Shortening](#classname-shortening)
    * [Custom Line Separator](#custom-line-separator)
    * [Root Cause First](#root-cause-first)
    * [Conditional Output](#conditional-output)
    * [Stack Hashes](#stack-hashes)
    * [Using with PatternLayout](#using-with-patternlayout)
* [Registering Additional Providers](#registering-additional-providers)
* [Prefix/Suffix/Separator](#prefixsuffixseparator)
* [Composite Encoder/Layout](#composite-encoderlayout)
	* [Providers common to LoggingEvents and AccessEvents](#providers-common-to-loggingevents-and-accessevents)
	* [Providers for LoggingEvents](#providers-for-loggingevents)
	* [Providers for AccessEvents](#providers-for-accessevents)
	* [Nested JSON Provider](#nested-json-provider)
	* [Pattern JSON Provider](#pattern-json-provider)
		* [LoggingEvent patterns](#loggingevent-patterns)
		* [AccessEvent patterns](#accessevent-patterns)
	* [Custom JSON Provider](#custom-json-provider)
* [Status Listeners](#status-listeners)
* [Joran/XML Configuration](#joran-xml-configuration)
	* [Duration Property](#duration-property)
	* [Comma separated list of values](#comma-separated-list-of-values)


## Including it in your project

Maven style:

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
    <!-- Use runtime scope if the project does not have any compile-time usage of logstash-logback-encoder,
         such as usage of StructuredArguments/Markers or implementations such as
         JsonProvider, AppenderListener, JsonFactoryDecorator, JsonGeneratorDecorator, etc
    <scope>runtime</scope>
    -->
</dependency>
<!-- Your project must also directly depend on either logback-classic or logback-access. For example: -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.3.7</version>
    <!-- Use runtime scope if the project does not have any compile-time usage of logback,
         such as implementations of Appender, Encoder, Layout, TurboFilter, etc
    <scope>runtime</scope>
    -->
</dependency>
```

If you get `ClassNotFoundException`/`NoClassDefFoundError`/`NoSuchMethodError` at runtime,
then ensure the required dependencies (and appropriate versions) as specified in the pom file
from the maven repository exist on the runtime classpath.
Specifically, the following need to be available on the runtime classpath:

* jackson-databind / jackson-core / jackson-annotations >= 2.12.0
* logback-core >= 1.3.0
* logback-classic >= 1.3.0 (required for logging _LoggingEvents_)
* logback-access >= 1.3.0 (required for logging _AccessEvents_)
* slf4j-api (usually comes as a transitive dependency of logback-classic)
* java-uuid-generator (required if the `uuid` provider is used)

Older versions than the ones specified in the pom file _might_ work, but the versions in the pom file are what testing has been performed against.
Support for logback versions prior to 1.3.0 was removed in logstash-logback-encoder 7.4.

If you are using logstash-logback-encoder in a project (such as spring-boot) that also declares dependencies on any of the above libraries, you might need to tell maven explicitly which versions to use to avoid conflicts.
You can do so using maven's [dependencyManagement](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Management) feature.
For example, to ensure that maven doesn't pick different versions of logback-core, logback-classic, and logback-access, add this to your project's pom.xml

```xml
<properties>
    <logback.version>1.3.7</logback.version>
</properties>
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-core</artifactId>
            <version>${logback.version}</version>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>${logback.version}</version>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-access</artifactId>
            <version>${logback.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## Java Version Requirements

| logstash-logback-encoder | Minimum Java Version supported |
|--------------------------|--------------------------------|
| &gt;= 6.0                | 8                              |
| 5.x                      | 7                              |
| &lt;= 4.x                | 6                              |


## Usage

To log using JSON format, you must configure logback to use either:

* an appender provided by the logstash-logback-encoder library, OR
* an appender provided by logback (or another library) with an encoder or layout provided by the logstash-logback-encoder library

The appenders, encoders, and layouts provided by the logstash-logback-encoder library are as follows:

| Format        | Protocol   | Function | LoggingEvent | AccessEvent
|---------------|------------|----------| ------------ | -----------
| Logstash JSON | Syslog/UDP | Appender | [`LogstashUdpSocketAppender`](/src/main/java/net/logstash/logback/appender/LogstashUdpSocketAppender.java) | [`LogstashAccessUdpSocketAppender`](/src/main/java/net/logstash/logback/appender/LogstashAccessUdpSocketAppender.java)
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



### UDP Appenders

To output JSON for LoggingEvents to a syslog/UDP channel,
use the `LogstashUdpSocketAppender` with a `LogstashLayout` or `LoggingEventCompositeJsonLayout`
in your `logback.xml`, like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="stash" class="net.logstash.logback.appender.LogstashUdpSocketAppender">
        <host>MyAwesomeSyslogServer</host>
        <!-- port is optional (default value shown) -->
        <port>514</port>
        <!-- layout is required -->
        <layout class="net.logstash.logback.layout.LogstashLayout"/>
    </appender>
    
    <root level="all">
        <appender-ref ref="stash" />
    </root>
</configuration>
```
You can further customize the JSON output by customizing the layout as described in later sections.

For example, to configure [global custom fields](#global-custom-fields), you can specify
```xml
<appender name="stash" class="net.logstash.logback.appender.LogstashUdpSocketAppender">
    <host>MyAwesomeSyslogServer</host>
    <!-- port is optional (default value shown) -->
    <port>514</port>
    <layout class="net.logstash.logback.layout.LogstashLayout">
        <customFields>{"appname":"myWebservice"}</customFields>
    </layout>
</appender>
```

To output JSON for AccessEvents over UDP, use a `LogstashAccessUdpSocketAppender`
with a `LogstashAccessLayout` or `AccessEventCompositeJsonLayout`
in your `logback-access.xml`, like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="stash" class="net.logstash.logback.appender.LogstashAccessUdpSocketAppender">
        <host>MyAwesomeSyslogServer</host>
        <!-- port is optional (default value shown) -->
        <port>514</port>

        <layout class="net.logstash.logback.layout.LogstashAccessLayout">
            <customFields>{"appname":"myWebservice"}</customFields>
        </layout>
    </appender>

    <appender-ref ref="stash" />
</configuration>
```


To receive syslog/UDP input in logstash, configure a [`syslog`](https://www.elastic.co/guide/en/logstash/current/plugins-inputs-syslog.html) or [`udp`](https://www.elastic.co/guide/en/logstash/current/plugins-inputs-udp.html) input with the [`json`](https://www.elastic.co/guide/en/logstash/current/plugins-codecs-json.html) codec in logstash's configuration like this:
```
input {
    syslog {
        codec => "json"
    }
}
```


### TCP Appenders

To output JSON for LoggingEvents over TCP, use a `LogstashTcpSocketAppender`
with a `LogstashEncoder` or `LoggingEventCompositeJsonEncoder`
in your `logback.xml`, like this:

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
with a `LogstashAccessEncoder` or `AccessEventCompositeJsonEncoder`
in your `logback-access.xml`, like this:

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

The TCP appenders use an encoder, rather than a layout as the [UDP appenders](#udp-appenders) . 
You can use a `Logstash*Encoder`, `*EventCompositeJsonEncoder`, or any other logback encoder.
All of the output formatting options are configured at the encoder level.

Internally, the TCP appenders are asynchronous (using the [LMAX Disruptor RingBuffer](https://lmax-exchange.github.io/disruptor/)).
All the encoding and TCP communication is delegated to a single writer thread.
There is no need to wrap the TCP appenders with another asynchronous appender
(such as `AsyncAppender` or `LoggingEventAsyncDisruptorAppender`).

All the configuration parameters (except for sub-appender) of the [async appenders](#async-appenders) are valid for TCP appenders. For example, `waitStrategyType` and `ringBufferSize`.

By default the TCP appenders will never block the logging thread - if the RingBuffer is full (e.g. due to slow network, etc), then events will be dropped. If desired, the appender can also be configured to block and wait for free space, see [RingBuffer Full](#ringbuffer-full) for more information.

The TCP appenders will automatically reconnect if the connection breaks. Multiple destinations can be configured to increase availability and reduce message lost. See [Multiple Destinations](#multiple-destinations) for more information.

To receive TCP input in logstash, configure a [`tcp`](https://www.elastic.co/guide/en/logstash/current/plugins-inputs-tcp.html) input with the [`json_lines`](https://www.elastic.co/guide/en/logstash/current/plugins-codecs-json_lines.html) codec in logstash's configuration like this:

```
input {
    tcp {
        port => 4560
            codec => json_lines
    }
}
```

In order to guarantee that logged messages have had a chance to be processed by the TCP appender, you'll need to [cleanly shut down logback](http://logback.qos.ch/manual/configuration.html#stopContext) when your application exits.


#### Keep-Alive

If events occur infrequently, and the connection breaks consistently due to a server-side idle timeout,
then you can enable keep alive functionality by configuring a `keepAliveDuration` like this:

```xml
<appender name="stash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    ...
    <keepAliveDuration>5 minutes</keepAliveDuration>
</appender>
```

This setting accepts a Logback Duration value - see the section dedicated to [Duration Property](#duration-property) for more information about the valid values.

When the `keepAliveDuration` is set, then a keep alive message will be sent if an event has not occurred for the length of the duration.
The keep alive message defaults to unix line ending (`\n`), but can be changed by setting the `keepAliveMessage` property to the desired value. The following values have special meaning:

- `<empty string>`: no keep alive
- `SYSTEM`: system's line separator
- `UNIX`: unix line ending (`\n`)
- `WINDOWS`: windows line ending (`\r\n`)

Any other value will be used as-is.

The keep alive message is encoded in `UTF-8` by default. This can be changed by setting the `keepAliveCharset` property to the name of the desired charset.


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

Destinations are expressed using the following format: `host[:port]` where:
- `host` can be a hostname (eg. `localhost`) , an IPv4 address (eg. `192.168.1.1`) or an IPv6 address enclosed between brackets (eg. `[2001:db8::1]`).
- `port` is optional and, if specified, must be prefixed by a colon (`:`). It must be a valid integer value between `0` and `65535`.


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
      <td valign="top"><tt>preferPrimary</tt></td>
      <td>(default)
The first destination is considered the <em>primary</em> destination.
Each additional destination is considered a <em>secondary</em> destination.
This strategy prefers the primary destination, unless it is down.
The appender will attempt to connect to each destination in the order in which they are configured.
If a connection attempt fails, thes the appender will attempt to connect to the next destination.
If a connection succeeds, and then closes <em>before</em> the <tt>minConnectionTimeBeforePrimary</tt>
has elapsed, then the appender will attempt to connect to the next destination.
If a connection succeeds, and then closes <em>after</em> the <tt>minConnectionTimeBeforePrimary</tt>
has elapsed, then the appender will attempt to connect
to the destinations in the order in which they are configured,
starting at the first/primary destination.
<br/><br/>
The <tt>secondaryConnectionTTL</tt> can be set to gracefully close connections to <em>secondary</em>
destinations after a specific duration.  This will force the
the appender to reattempt to connect to the destinations in order again.
The <tt>secondaryConnectionTTL</tt> value does not affect connections to the
<em>primary</em> destination.
<br/><br/>
The <tt>minConnectionTimeBeforePrimary</tt> (10 seconds by default) specifies
the minimum amount of time that a sucessfully established connection
must remain open before the next connection attempt will try the primary.
i.e. If a connection stays open less than this amount of time,
then the next connection attempt will attempt the next destination (instead of the primary).
This is used to prevent a connection storm to the primary in the case the
primary accepts a connection, and then immediately closes it. 
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
      <td valign="top"><tt>roundRobin</tt></td>
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
      <td valign="top"><tt>random</tt></td>
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

By default, the TCP appender will wait 30 seconds between connection attempts to a single destination.
The time between connection attempts to each destination is tracked separately.

This amount of time to delay can be changed by setting the `reconnectionDelay` field.

```xml
<appender name="stash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    ...
    <reconnectionDelay>1 second</reconnectionDelay>
</appender>
```

This setting accepts a Logback Duration value - see the section dedicated to [Duration Property](#duration-property) for more information about the valid values.


#### Connection Timeout

By default, a connection timeout of 5 seconds is used when connecting to a remote destination.
You can adjust this by setting the appender's `connectionTimeout` configuration property to the desired value.

```xml
<appender name="stash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    ...
    <connectionTimeout>5 seconds</connectionTimeout>
</appender>
```

A value of `0` means "don't use a timeout and wait indefinitely" which often really means "use OS defaults".

This setting accepts a Logback Duration value - see the section dedicated to [Duration Property](#duration-property) for more information about the valid values.


#### Write Buffer Size

By default, a buffer size of `8192` bytes is used to buffer socket output stream writes.
You can adjust this by setting the appender's `writeBufferSize`.
 
```xml
<appender name="stash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    ...
    <writeBufferSize>16384</writeBufferSize>
</appender>
```

Buffering can be disabled by setting the `writeBufferSize` to `0`.
Consider disabling the write buffer if you are concerned about losing data from the buffer for flaky connections.
Disabling the buffer can potentially slow down the writer thread due to increased system calls,
but in some environments, this does not seem to affect overall performance.
See [this discussion](https://github.com/logfellow/logstash-logback-encoder/issues/342).


#### Write Timeout

If a destination stops reading from its socket input, but does not close the connection, then writes from the TCP appender will eventually backup, causing the ring buffer to backup, causing events to be dropped.

To detect this situation, you can enable a write timeout, so that "stuck" writes will eventually timeout, at which point the connection will be re-established.
When the [write buffer](#write-buffer-size) is enabled, any buffered data will be lost when the connection is reestablished.

By default there is no write timeout. To enable a write timeout, do the following:

```xml
<appender name="stash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    ...
    <writeTimeout>1 minute</writeTimeout>
</appender>
```

Note that since the blocking java socket output stream used to send events does not have a concept of a write timeout, write timeouts are detected using a task scheduled periodically with the same frequency as the write timeout.
For example, if the write timeout is set to 30 seconds, then a task will execute every 30 seconds to see if 30 seconds has elapsed since the start of the current write operation.
Therefore, it is recommended to use longer write timeouts (e.g. > 30s, or minutes), rather than short write timeouts, so that this task does not execute too frequently.
Also, this approach means that it could take up to two times the write timeout before a write timeout is detected.

The write timeout must be >0. A timeout of zero is interpreted as an infinite timeout which effecively means "no write timeout".

This setting accepts a Logback Duration value - see the section dedicated to [Duration Property](#duration-property) for more information about the valid values.



#### Initial Send Delay

The appender starts writing the events stored in the queue as soon as the connection is established. In some cases you may want to add an extra delay before sending the first events after the connection is established. This may come in handy in situations where the appender connects to an intermediate proxy that needs some time to establish a connection to the final destination. If the appender starts writing immediately, events may be lost in-flight if the proxy ultimately fails to connect to the final destination. 

To enable this feature, set the `initialSendDelay` to the desired delay before the first event is sent after the connection is established. If the connection is lost before the delay expires, no event will be lost. The default value is `0` which means no delay and start flusing pending events immediately.

The following example configures a delay of 5 secondes before writing in the new connection:

```xml
<appender name="stash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    ...
    <initialSendDelay>5 secondes</initialSendDelay>
</appender>
```

This setting accepts a Logback Duration value - see the section dedicated to [Duration Property](#duration-property) for more information about the valid values.


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

See the logstash documentation for the [`tcp`](https://www.elastic.co/guide/en/logstash/current/plugins-inputs-tcp.html) input for how to configure it to use SSL.


### Async Appenders

The `*AsyncDisruptorAppender` appenders are similar to logback's `AsyncAppender`,
except that a [LMAX Disruptor RingBuffer](https://lmax-exchange.github.io/disruptor/)
is used as the queuing mechanism, as opposed to a `BlockingQueue`.
These async appenders can delegate to any other underlying logback appender.

For example:

```xml
<appender name="file" class="ch.qos.logback.core.rolling.RollingFileAppender">
    ...
</appender>
    
<appender name="async" class="net.logstash.logback.appender.LoggingEventAsyncDisruptorAppender">
    <appender-ref ref="file" />
</appender>
```

> **Warning**
> Since Logback 1.3 it is not allowed anymore to declare an `<appender>` inside another `<appender>`. The nested appender should instead be declared outside and `<appender-ref>` must be used to refer to it.
> 
> See [LOGBACK-1674](https://jira.qos.ch/browse/LOGBACK-1674) for more information.


#### RingBuffer Size

Logging events are first enqueued in a ring buffer before they are delivered to their final destination by a separate handler thread.
The buffer size is fixed, it does not grow or shrink at runtime. Its size is determined  by the `ringBufferSize` configuration property set to `8192` by default.

If the handler thread is not as fast as the producing threads, then the ring buffer will eventually fill up, at which point events will be dropped (the default) or the producing threads are blocked depending on configured `appendTimeout` (see [RingBuffer Full](#ringbuffer-full).


#### RingBuffer Full

The async appenders will by default never block the logging thread.
If the RingBuffer is full (e.g. due to slow network, etc), then events will be dropped.

Alternatively, you can configure the appender to wait until space becomes available instead of dropping the events immediately. This may come in handy when you want to rely on the buffering and the async nature of the appender but don't want to loose any event in case of large logging bursts that exceed the size of the RingBuffer.

The behaviour of the appender when the RingBuffer is controlled by the `appendTimeout` configuration property:

| `appendTimeout` | Behaviour when RingBuffer is full                                      |
|-----------------|------------------------------------------------------------------------|
| `< 0`           | disable timeout and wait until space is available                      |
| `0`             | no timeout, give up immediately and drop event (this is the *default*) |
| `> 0`           | retry during the specified amount of time                              |


Logging threads waiting for space in the RingBuffer wake up periodically at a frequency starting at `1ns` and increasing exponentially up to `appendRetryFrequency` (default `5ms`). 
Only one thread is allowed to retry at a time. If a thread is already retrying, additional threads are waiting on a lock until the first is finished. This strategy should help to limit CPU consumption while providing good enough latency and throughput when the ring buffer is at (or close) to its maximal capacity.

When the appender drops an event, it emits a warning status message every `droppedWarnFrequency` consecutive dropped events (`1000` by default, use `0` to turn off warnings). Another status message is emitted when the drop period is over and a first event is succesfully enqueued reporting the total number of events that were dropped.


#### Graceful Shutdown

In order to guarantees that logged messages have had a chance to be processed by asynchronous appenders (including the TCP appender) and ensure background threads have been stopped, you'll need to [cleanly shut down logback](http://logback.qos.ch/manual/configuration.html#stopContext) when your application exits.

When gracefully stopped, async appenders wait until all events in the buffer are processed and the buffer is empty.
The maximum time to wait is configured by the `shutdownGracePeriod` parameter and is set to `1 minute` by default.
Events still in the buffer after this period is elapsed are dropped and the appender is stopped.


#### Wait Strategy

By default, the [`BlockingWaitStrategy`](https://lmax-exchange.github.io/disruptor/docs/com/lmax/disruptor/BlockingWaitStrategy.html) is used by the worker thread spawned by this appender.
The `BlockingWaitStrategy` minimizes CPU utilization, but results in slower latency and throughput.
If you need faster latency and throughput (at the expense of higher CPU utilization), consider
a different [wait strategy](https://lmax-exchange.github.io/disruptor/docs/com/lmax/disruptor/WaitStrategy.html) offered by the disruptor.

> !! Whichever wait strategy you choose, be sure to test and monitor CPU utilization, latency, and throughput to ensure it meets your needs.
> For example, in some configurations, `SleepingWaitStrategy` can consume 90% CPU utilization at rest.

The wait strategy can be configured on the async appender using the `waitStrategyType` parameter, like this:

```xml
<appender name="async" class="net.logstash.logback.appender.LoggingEventAsyncDisruptorAppender">
    <waitStrategyType>sleeping</waitStrategyType>
    ...
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
      <td><tt>yielding</tt></td>
      <td>none</td>
      <td><a href="https://lmax-exchange.github.io/disruptor/docs/com/lmax/disruptor/YieldingWaitStrategy.html"><tt>YieldingWaitStrategy</tt></a></td>
    </tr>
    <tr>
      <td><pre>sleeping{
  <em>retries</em>,
  <em>sleepTimeNs</em>
}
</pre>e.g.<br/><tt>sleeping</tt><br/>or<br/><tt>sleeping{500,1000}</tt></td>
      <td>
        <ol>
          <li><tt>retries</tt> - Number of times (integer) to spin before sleeping. (default = 200)</li>
          <li><tt>sleepTimeNs</tt> - Time in nanoseconds to sleep each iteration after spinning (default = 100)</li>
        </ol>
      </td>
      <td><a href="https://lmax-exchange.github.io/disruptor/docs/com/lmax/disruptor/SleepingWaitStrategy.html"><tt>SleepingWaitStrategy</tt></a></td>
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
    <tr>
      <td><pre>liteTimeoutBlocking{
  <em>timeout</em>,
  <em>timeUnit</em>
}
</pre>e.g.<br/><tt>liteTimeoutBlocking{1,minutes}</tt></td>
      <td>
        <ol>
          <li><tt>timeout</tt> - Time to block before throwing an exception</li>
          <li><tt>timeUnit</tt> - Units of time for timeout. String name of a <a href="http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeUnit.html"><tt>TimeUnit</tt></a> value (e.g. <tt>seconds</tt>)</li>
        </ol>
      </td>
      <td><a href="https://lmax-exchange.github.io/disruptor/docs/com/lmax/disruptor/LiteTimeoutBlockingWaitStrategy.html"><tt>LiteTimeoutBlockingWaitStrategy</tt></a></td>
    </tr>
  </tbody>
</table>

See [AsyncDisruptorAppender](/src/main/java/net/logstash/logback/appender/AsyncDisruptorAppender.java)
for other configuration parameters (such as `ringBufferSize`, `threadNamePrefix`, `daemon`, and `droppedWarnFrequency`)


### Appender Listeners

Listeners can be registered to an appender to receive notifications for the appender lifecycle and event processing.

See the two listener interfaces for the types of notifications that can be received:

* [`AppenderListener`](/src/main/java/net/logstash/logback/appender/listener/AppenderListener.java) - basic notifications for the [async appenders](#async-appenders) and [UDP appenders](#udp-appenders).
* [`TcpAppenderListener`](/src/main/java/net/logstash/logback/appender/listener/TcpAppenderListener.java) - extension of `AppenderListener` with additional TCP-specific notifications.  Only works with the [TCP appenders](#tcp-appenders). 

Some example use cases for a listener are:

* Monitoring metrics for events per second, event processing durations, dropped events, connections successes / failures, etc.
* Logging event processing errors to a different appender (that perhaps appends to a different destination).
  
A [`FailureSummaryLoggingAppenderListener`](src/main/java/net/logstash/logback/appender/listener/FailureSummaryLoggingAppenderListener.java)
is provided that will log a warning on the first success after a series of consecutive append/send/connect failures.
The message includes summary details of the failures that occurred (such as the number of failures, duration of the failures, etc).
To register it:

```xml
<appender name="stash" class="net.logstash.logback.appender.LogstashAccessTcpSocketAppender">
    <listener class="net.logstash.logback.appender.listener.FailureSummaryLoggingAppenderListener">
        <loggerName>net.logstash.logback.appender.listener.FailureSummaryLoggingAppenderListener</loggerName>
    </listener>
</appender>
```

You may also create your own listener by implementing the `*Listener` interface and register it to an appender using the `listener` xml element like this:

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

To receive file input in logstash, configure a [`file`](https://www.elastic.co/guide/en/logstash/current/plugins-inputs-file.html) input in logstash's configuration like this:

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
| `@timestamp`  | Time of the log event (`ISO_OFFSET_DATE_TIME`) - see [Customizing Timestamp](#customizing-timestamp)
| `@version`    | Logstash format version (e.g. `1`) - see [Customizing Version](#customizing-version)
| `message`     | Formatted log message of the event - see [Customizing Message](#customizing-message)
| `logger_name` | Name of the logger that logged the event
| `thread_name` | Name of the thread that logged the event
| `level`       | String name of the level of the event
| `level_value` | Integer value of the level of the event
| `stack_trace` | (Only if a throwable was logged) The stacktrace of the throwable.  Stackframes are separated by line endings.
| `tags`        | (Only if tags are found) The names of any markers not explicitly handled.  (e.g. markers from `MarkerFactory.getMarker` will be included as tags, but the markers from [`Markers`](/src/main/java/net/logstash/logback/marker/Markers.java) will not.) This can be fully disabled by specifying `<includeTags>false</includeTags>`, in the encoder/layout/appender configuration.



### MDC fields

By default, `LogstashEncoder`/`LogstashLayout` will write each
[Mapped Diagnostic Context (MDC) (`org.slf4j.MDC`)](https://www.slf4j.org/api/org/slf4j/MDC.html)
entry to the output.

To disable writing MDC entries, add `<includeMdc>false</includeMdc>`
to the `LogstashEncoder`/`LogstashLayout` configuration.

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

By default, the MDC key is used as the field name in the output.
To use an alternative field name in the output for an MDC entry,
specify `<mdcKeyFieldName>mdcKeyName=fieldName</mdcKeyFieldName>`: 

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <mdcKeyFieldName>key1=alternateFieldNameForKey1</mdcKeyFieldName>
</encoder>
```

You can also manipulate the MDC entry values written to the JSON output.
By default, no manipulations are done and all MDC entry values are written as text.

Currently, MDC entry writers for the following value types are supported:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <!--
        Writes long values (instead of String values) for any MDC values
        that can be parsed as a long (radix 10).
        e.g. Writes 1234 instead of "1234"
    -->
    <mdcEntryWriter class="net.logstash.logback.composite.loggingevent.mdc.LongMdcEntryWriter"/>

    <!--
        Writes double values (instead of String values) for any MDC values
        that can be parsed as a double, except NaN and positive/negative Infinity.
        e.g. 1234.5678 instead of "1234.5678"
    -->
    <mdcEntryWriter class="net.logstash.logback.composite.loggingevent.mdc.DoubleMdcEntryWriter"/>

    <!--
        Writes boolean values (instead of String values) for any MDC values
        that equal "true" or "false", ignoring case.
        e.g. Writes true instead of "true"
    -->
    <mdcEntryWriter class="net.logstash.logback.composite.loggingevent.mdc.BooleanMdcEntryWriter"/>
</encoder>
```

To add your own MDC entry writer for other types or apply the manipulations only for specific fields
you can write your own implementation of [`MdcEntryWriter`](src/main/java/net/logstash/logback/composite/loggingevent/mdc/MdcEntryWriter.java).

You can also replace the default MDC JSON provider with your own class extending from
[`MdcJsonProvider`](src/main/java/net/logstash/logback/composite/loggingevent/MdcJsonProvider.java).
Configuring your class as a [Custom JSON Provider](#custom-json-provider) will then replace
the default `MdcJsonProvider`.

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <provider class="mypackagenames.MyCustomMdcJsonProvider"/>
</encoder>
```


### Key Value Pair Fields

Slf4j 2's [fluent API](https://www.slf4j.org/manual.html#fluent) supports attaching key value pairs to the log event.

`LogstashEncoder`/`LogstashLayout` will write each key value pair as a field in the output by default.

To disable writing key value pairs, add `<includeKeyValuePairs>false</includeKeyValuePairs>`
to the `LogstashEncoder`/`LogstashLayout` configuration.

You can also configure specific key value pairs to be included or excluded as follows:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <includeKeyValueKeyName>key1ToInclude</includeKeyValueKeyName>
    <includeKeyValueKeyName>key2ToInclude</includeKeyValueKeyName>
</encoder>
```

or

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <excludeKeyValueKeyName>key1ToExclude</excludeKeyValueKeyName>
    <excludeKeyValueKeyName>key2ToExclude</excludeKeyValueKeyName>
</encoder>
```

When key names are specified for inclusion, then all other keys will be excluded.
When key names are specified for exclusion, then all other keys will be included.
It is a configuration error to specify both included and excluded key names.

By default, the key is used as the field name in the output.
To use an alternative field name in the output for an key value pair,
specify`<keyValuePairsKeyFieldName>keyName=fieldName</keyValuePairsKeyFieldName>`: 

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <keyValueKeyFieldName>key1=alternateFieldNameForKey1</keyValueKeyFieldName>
</encoder>
```


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
import static net.logstash.logback.argument.StructuredArguments.*;

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
 * For example:
 *     public static StructuredArgument foo(Foo foo) {
 *         return StructuredArguments.value("foo", foo);
 *     }
 */
logger.info("log message {}", foo(foo));

```

Abbreviated convenience methods are available for all the structured argument types.
For example, instead of `keyValue(key, value)`, you can use `kv(key, value)`.



Examples using `Markers`:

```java
import static net.logstash.logback.marker.Markers.*;

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
```

Custom filters implementing [`HeaderFilter`](/src/main/java/net/logstash/logback/composite/accessevent/HeaderFilter.java)
can be used by specifying the filter class like this:

```xml
<requestHeaderFilter class="your.package.YourFilterClass"/>
```

## Customizing Jackson

Logstash-logback-encoder uses [Jackson](https://github.com/FasterXML/jackson) to encode log and access events.

Logstash-logback-encoder provides sensible defaults for Jackson, but gives you full control over the Jackson configuration.

For example, you can:
* specify the [data format](#data-format)
* customize the [`JsonFactory` and `JsonGenerator`](#customizing-json-factory-and-generator)
* register [jackson modules](#registering-jackson-modules)
* configure [character escapes](#customizing-character-escapes) 

### Data Format

JSON is used by default, but other data formats supported by Jackson can be used.
* [text data formats](https://github.com/FasterXML/jackson-dataformats-text)
* [binary data formats](https://github.com/FasterXML/jackson-dataformats-binary)

> :warning: When using non-JSON data formats, you must include the appropriate jackson dataformat library on the runtime classpath,
> typically via a  maven/gradle dependency  (e.g. for Smile, include `jackson-dataformat-smile`).

[Decorators](#customizing-json-factory-and-generator) are provided for the following data formats:
* `cbor` - [`CborJsonFactoryDecorator`](src/main/java/net/logstash/logback/decorate/cbor/CborJsonFactoryDecorator.java)
* `smile` - [`SmileJsonFactoryDecorator`](src/main/java/net/logstash/logback/decorate/smile/SmileJsonFactoryDecorator.java)
* `yaml` - [`YamlJsonFactoryDecorator`](src/main/java/net/logstash/logback/decorate/yaml/YamlJsonFactoryDecorator.java)

To use one these formats, specify the `<jsonFactoryDecorator>` like this:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <jsonFactoryDecorator class="net.logstash.logback.decorate.smile.SmileJsonFactoryDecorator"/>
</encoder>
```
Other data formats can be used by implementing a custom
[`net.logstash.logback.decorate.JsonFactoryDecorator`](src/main/java/net/logstash/logback/decorate/JsonFactoryDecorator.java).


The following [decorators](#customizing-json-factory-and-generator)
can be used to configure data-format-specific generator features:
* [`SmileFeatureJsonGeneratorDecorator`](src/main/java/net/logstash/logback/decorate/smile/SmileFeatureJsonGeneratorDecorator.java)
* [`CborFeatureJsonGeneratorDecorator`](src/main/java/net/logstash/logback/decorate/cbor/CborFeatureJsonGeneratorDecorator.java)
* [`YamlFeatureJsonGeneratorDecorator`](src/main/java/net/logstash/logback/decorate/yaml/YamlFeatureJsonGeneratorDecorator.java)

For example:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <jsonFactoryDecorator class="net.logstash.logback.decorate.smile.SmileJsonFactoryDecorator"/>
    <jsonGeneratorDecorator class="net.logstash.logback.decorate.smile.SmileFeatureJsonGeneratorDecorator">
        <disable>WRITE_HEADER</disable>
    </jsonGeneratorDecorator>
</encoder>
``` 

### Customizing JSON Factory and Generator

The `JsonFactory` and `JsonGenerator` used to write output can be customized by instances of:
* [`JsonFactoryDecorator`](/src/main/java/net/logstash/logback/decorate/JsonFactoryDecorator.java)
* [`JsonGeneratorDecorator`](/src/main/java/net/logstash/logback/decorate/JsonGeneratorDecorator.java)

For example, you could enable pretty printing by using the
[PrettyPrintingJsonGeneratorDecorator](/src/main/java/net/logstash/logback/decorate/PrettyPrintingJsonGeneratorDecorator.java)

Or customize object mapping like this:

```java
public class ISO8601DateDecorator implements JsonFactoryDecorator  {

    @Override
    public JsonFactory decorate(JsonFactory factory) {
        ObjectMapper codec = (ObjectMapper) factory.getCodec();
        codec.setDateFormat(new ISO8601DateFormat());
        return factory;
    }
}
```
and then specify the decorators in the logback.xml file like this:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <jsonGeneratorDecorator class="net.logstash.logback.decorate.PrettyPrintingJsonGeneratorDecorator"/>
    <jsonFactoryDecorator class="your.package.ISO8601DateDecorator"/>
</encoder>
```

`JsonFactory` and `JsonGenerator` features can be enabled/disabled by using the
`FeatureJsonFactoryDecorator` and `FeatureJsonGeneratorDecorator`, respectively.
For example:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <jsonFactoryDecorator class="net.logstash.logback.decorate.FeatureJsonFactoryDecorator">
        <disable>USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING</disable>
    </jsonFactoryDecorator>
    <jsonGeneratorDecorator class="net.logstash.logback.decorate.FeatureJsonGeneratorDecorator">
        <enable>WRITE_NUMBERS_AS_STRINGS</enable>
    </jsonGeneratorDecorator>
</encoder>
``` 

See the [net.logstash.logback.decorate](/src/main/java/net/logstash/logback/decorate) package
and sub-packages for other decorators.

### Registering Jackson Modules

By default, Jackson modules are dynamically registered via
[`ObjectMapper.findAndRegisterModules()`](https://fasterxml.github.io/jackson-databind/javadoc/2.9/com/fasterxml/jackson/databind/ObjectMapper.html#findAndRegisterModules--).

Therefore, you just need to add jackson modules (e.g. jackson-datatype-jdk8) to the classpath,
and they will be dynamically registered.

To disable automatic discovery, set `<findAndRegisterJacksonModules>false</findAndRegisterJacksonModules>` on the encoder/layout.

If you have a module that Jackson is not able to dynamically discover,
you can register it manually via a [`JsonFactoryDecorator`](#customizing-json-factory-and-generator).

### Customizing Character Escapes

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

## Masking

The [`MaskingJsonGeneratorDecorator`](src/main/java/net/logstash/logback/mask/MaskingJsonGeneratorDecorator.java)
can be used to mask sensitive values (e.g. personally identifiable information (PII) or financial data).

Data to be masked can be identified by [path](#identifying-field-values-to-mask-by-path)
and/or by [value](#identifying-field-values-to-mask-by-value).

### Identifying field values to mask by path

Paths of fields to mask can be specified in several ways, as shown in the following example:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <jsonGeneratorDecorator class="net.logstash.logback.mask.MaskingJsonGeneratorDecorator">
    
        <!-- The default mask string can optionally be specified by <defaultMask>.
             When the default mask string is not specified, **** is used.
        -->
        <defaultMask>****</defaultMask>
        
        <!-- Field paths to mask added via <path> will use the default mask string -->
        <path>singleFieldName</path>
        <path>/absolute/path/to/mask</path>
        <path>partial/path/to/mask</path>
        <path>partial/path/with/*/wildcard</path>
        <path>tilde~0slash~1escapedPath</path>
        
        <!-- Multiple field paths can be specified as a comma separated string in the <paths> element. -->
        <paths>path1, path2, path3</paths>
        
        <!-- Field paths to mask added via <pathMask> can use a non-default mask string -->
        <pathMask>
            <path>some/path</path>
            <path>some/other/path</path>
            <mask>[masked]</mask>
        </pathMask>
        <pathMask>
            <paths>anotherFieldName,anotherFieldName2</paths>
            <mask>**anotherCustomMask**</mask>
        </pathMask>
        
        <!-- Field paths to mask can be supplied dynamically with an implementation
             of MaskingJsonGeneratorDecorator.PathMaskSupplier
        -->
        <pathMaskSupplier class="your.custom.PathMaskSupplierA"/>
        
        <!-- Custom implementations of net.logstash.logback.mask.FieldMasker
             can be used for more advanced masking behavior
        -->
        <fieldMasker class="your.custom.FieldMaskerA"/>
        <fieldMasker class="your.custom.FieldMaskerB"/>
    </jsonGeneratorDecorator>
</encoder>
```

See [`PathBasedFieldMasker`](src/main/java/net/logstash/logback/mask/PathBasedFieldMasker.java)
for the path string format and more examples.  But in general:

* Paths follow a format similar to (but not _exactly_ same as) a [JSON Pointer](http://tools.ietf.org/html/draft-ietf-appsawg-json-pointer-03).
* Absolute paths start with `/` and are absolute to the root of the JSON output event (e.g. `/@timestamp` would mask the default timestamp field)
* Partial paths do not start with `/` and match anywhere that path sequence is seen in the output.
* A path with a single token (i.e. no `/` characters) will match all occurrences of a field with the given name
* A wildcard token (`*`) will match anything at that location within the path
* Use `~1` to escape `/` within a token
* Use `~0` to escape `~` within a token

### Identifying field values to mask by value

Specific values to be masked can be specified in several ways, as seen in the following example:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <jsonGeneratorDecorator class="net.logstash.logback.mask.MaskingJsonGeneratorDecorator">
    
        <!-- The default mask string can optionally be specified by <defaultMask>.
             When the default mask string is not specified, **** is used.
        -->
        <defaultMask>****</defaultMask>
        
        <!-- Values to mask added via <value> will use the default mask string -->
        <value>^foo$</value>
        <value>bar</value>
        
        <!-- Multiple values can be specified as a comma separated string in the <values> element. -->
        <values>
            ^baz$,
            ^blah$
        </values>
        
        <!-- Values to mask added via <valueMask> can use a non-default mask string
             The mask string here can reference regex capturing groups if needed 
        -->
        <valueMask>
            <value>^(foo)-.*$</value>
            <value>^(bar)-.*$</value>
            <mask>$1****</mask>
        </valueMask>
        
        <!-- Values to mask can be supplied dynamically with an implementation of
             MaskingJsonGeneratorDecorator.ValueMaskSupplier
        -->
        <valueMaskSupplier class="your.custom.ValueMaskSupplierA"/>
        
        <!-- Custom implementations of net.logstash.logback.mask.ValueMasker
             can be used for more advanced masking behavior
        -->
        <valueMasker class="your.custom.ValueMaskerA"/>
        <valueMasker class="your.custom.ValueMaskerB"/>
    </jsonGeneratorDecorator>
</encoder>
```

Identifying data to mask by value is much more expensive than identifying data to mask by [path](#identifying-field-values-to-mask-by-path).
Therefore, prefer identifying data to mask by path.

The value to mask is passed through every value masker, with the output of one masker passed as input to the next masker. 
This allows each masker to mask specific substrings within the value.
The order in which the maskers are executed is not defined, and should not be relied upon.

When using regexes to identify strings to mask, all matches within each string field value will be replaced.
If you want to match the full string field value, then use the beginning of line (`^`) and end of line (`$`) markers.


## Customizing Standard Field Names

The standard field names above for LoggingEvents and AccessEvents can be customized by using the `fieldNames`configuration element in the encoder or appender configuration.

For example:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <fieldNames>
        <timestamp>time</timestamp>
        <message>msg</message>
        <stackTrace>stacktrace</stackTrace>
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

By default, timestamps are written as string values in the format specified by
[`DateTimeFormatter.ISO_OFFSET_DATE_TIME`](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/format/DateTimeFormatter.html#ISO_OFFSET_DATE_TIME)
(e.g. `2019-11-03T10:15:30.123+01:00`), in the default TimeZone of the host Java platform.

You can change the pattern like this:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <timestampPattern>yyyy-MM-dd'T'HH:mm:ss.SSS</timestampPattern>
</encoder>
```

The value of the `timestampPattern` can be any of the following:

* `[UNIX_TIMESTAMP_AS_NUMBER]` - timestamp written as a JSON number value of the milliseconds since unix epoch
* `[UNIX_TIMESTAMP_AS_STRING]` - timestamp written as a JSON string value of the milliseconds since unix epoch
* `[` _`constant`_ `]` - (e.g. `[ISO_OFFSET_DATE_TIME]`) timestamp written using the given `DateTimeFormatter` constant
* any other value - (e.g. `yyyy-MM-dd'T'HH:mm:ss.SSS`) timestamp written using a `DateTimeFormatter` created from the given pattern

The provider uses a standard Java DateTimeFormatter under the hood. However, special optimisations are applied when using one of the following standard ISO formats that make it nearly 7x faster and more GC friendly:

* `[ISO_OFFSET_DATE_TIME]`
* `[ISO_ZONED_DATE_TIME]`
* `[ISO_LOCAL_DATE_TIME]`
* `[ISO_DATE_TIME]`
* `[ISO_INSTANT]`


With logback 1.3+ the timestamp will have millisecond precision.

The formatter uses the default TimeZone of the host Java platform by default. You can change it like this:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <timeZone>UTC</timeZone>
</encoder>
```

The value of the `timeZone` element can be any string accepted by java's `TimeZone.getTimeZone(String id)` method.
For example `America/Los_Angeles`, `GMT+10` or `UTC`.
Use the special value `[DEFAULT]` to use the default TimeZone of the system.



## Customizing LoggingEvent Message

By default, LoggingEvent messages are written as JSON strings. Any characters not allowed in a JSON string, such as newlines, are escaped.
See the [Customizing Character Escapes](#customizing-character-escapes) section for details.

You can also write messages as JSON arrays instead of strings, by specifying a `messageSplitRegex` to split the message text.
This configuration element can take the following values:

* any valid regex pattern
* `SYSTEM` (uses the system-default line separator)
* `UNIX` (uses `\n`)
* `WINDOWS` (uses `\r\n`)

If you split the log message by the origin system's line separator, the written message does not contain any embedded line separators.
The target system can unambiguously parse the message without any knowledge of the origin system's line separators.

For example:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <messageSplitRegex>SYSTEM</messageSplitRegex>
</encoder>
```
```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <messageSplitRegex>\r?\n</messageSplitRegex>
</encoder>
```
```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <messageSplitRegex>#+</messageSplitRegex>
</encoder>
```

## Customizing AccessEvent Message

By default, AccessEvent messages are written in the following format:

```
%clientHost - %user [%date] "%requestURL" %statusCode %bytesSent
```

To customize the message pattern, specify the `messagePattern` like this:

```xml
<encoder class="net.logstash.logback.encoder.LogstashAccessEncoder">
    <messagePattern>%clientHost [%date] "%requestURL" %statusCode %bytesSent</messagePattern>
</encoder>
```

The pattern can contain any of the [AccessEvent conversion words](http://logback.qos.ch/manual/layouts.html#AccessPatternLayout).


## Customizing Logger Name Length

For LoggingEvents, you can shorten the logger name field length similar to the normal pattern style of `%logger{36}`.
Examples of how it is shortened can be found [here](https://logback.qos.ch/manual/layouts.html#logger).

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <shortenedLoggerNameLength>36</shortenedLoggerNameLength>
</encoder>
```

The algorithm will shorten the logger name and attempt to reduce its size down to a maximum of number of characters.
It does so by reducing each part between dots to their first letter and gradually expand them starting from the right most element until the maximum size is reached.

To enable this feature, set the `shortenedLoggerNameLength` property to the desired value.
Setting the length to zero constitutes an exception and returns only the part of the logger name after last dot.
Use `-1` to disable shortening entirely.

The next table provides examples of the abbreviation algorithm in action.

|LENGTH|LOGGER NAME                 |SHORTENED                  |
|------|----------------------------|---------------------------|
|0     | `org.company.stack.Sample` | `Sample`                  |
|5     | `org.company.stack.Sample` | `o.c.s.Sample`            |
|16    | `org.company.stack.Sample` | `o.c.stack.Sample`        |
|22    | `org.company.stack.Sample` | `o.company.stack.Sample`  |
|25    | `org.company.stack.Sample` | `org.company.stack.Sample`|



## Customizing Stack Traces

When [logging exceptions](https://www.baeldung.com/slf4j-log-exceptions),
stack traces are formatted using logback's `ExtendedThrowableProxyConverter` by default.
However, you can configure the encoder to use any [`ThrowableHandlingConverter`](https://logback.qos.ch/apidocs/ch/qos/logback/classic/pattern/ThrowableHandlingConverter.html) to format stacktraces.

Note that the `ThrowableHandlingConverter` only applies to the
[exception passed as an extra argument](https://www.baeldung.com/slf4j-log-exceptions)
to the log method, the way you normally log an exception in slf4j.
Do **NOT** use [structured arguments or markers](#event-specific-custom-fields) for exceptions.

A powerful [`ShortenedThrowableConverter`](/src/main/java/net/logstash/logback/stacktrace/ShortenedThrowableConverter.java) is included in the logstash-logback-encoder library to format stacktraces with the features listed in the next sections.
This converter can even be used within a `PatternLayout` to format stacktraces in any non-JSON logs you may have.


### Omit Common Frames

Nested stacktraces often contain redudant frames that can safely be omitted without loosing any valuable information.

The following example shows a standard stack trace of an exception with a single root cause:

```
java.lang.RuntimeException: Unable to invoke service
	at org.company.stack.gen.StackGenerator.causedBy(StackGenerator.java:40)
	at org.company.stack.gen.StackGenerator.generateCausedBy(StackGenerator.java:34)
	at org.company.stack.framework.Dispatcher.invoke(Dispatcher.java:11)
	at org.company.stack.framework.Dispatcher.dispatch(Dispatcher.java:8)
	at org.company.stack.Sample.execute(Sample.java:36)
	at org.company.stack.Sample.omitCommonFrames(Sample.java:22)
	at org.company.stack.Sample.main(Sample.java:18)
Caused by: java.lang.RuntimeException: Destination unreachable
	at org.company.stack.gen.StackGenerator.two(StackGenerator.java:58)
	at org.company.stack.gen.StackGenerator.one(StackGenerator.java:55)
	at org.company.stack.gen.StackGenerator.causedBy(StackGenerator.java:38)
	at org.company.stack.gen.StackGenerator.generateCausedBy(StackGenerator.java:34)
	at org.company.stack.framework.Dispatcher.invoke(Dispatcher.java:11)
	at org.company.stack.framework.Dispatcher.dispatch(Dispatcher.java:8)
	at org.company.stack.Sample.execute(Sample.java:36)
	at org.company.stack.Sample.omitCommonFrames(Sample.java:22)
	at org.company.stack.Sample.main(Sample.java:18)
```

As we can see, the exception and the root cause have the first 7 frames in common. The overall stack trace length can be reduced by omitting these redundant frames from the root cause, as shown below:

```
java.lang.RuntimeException: Unable to invoke service
	at org.company.stack.gen.StackGenerator.causedBy(StackGenerator.java:40)
	at org.company.stack.gen.StackGenerator.generateCausedBy(StackGenerator.java:34)
	at org.company.stack.framework.Dispatcher.invoke(Dispatcher.java:11)
	at org.company.stack.framework.Dispatcher.dispatch(Dispatcher.java:8)
	at org.company.stack.Sample.execute(Sample.java:36)
	at org.company.stack.Sample.omitCommonFrames(Sample.java:22)
	at org.company.stack.Sample.main(Sample.java:18)
Caused by: java.lang.RuntimeException: Destination unreachable
	at org.company.stack.gen.StackGenerator.two(StackGenerator.java:58)
	at org.company.stack.gen.StackGenerator.one(StackGenerator.java:55)
	at org.company.stack.gen.StackGenerator.causedBy(StackGenerator.java:38)
	... 6 common frames omitted
```

Common frames are omitted and replaced with a message indicating the number of frames dropped. Note that the last common frame remains as a visual cue to the reader.

This feature is enabled by default and can be disabled if desired by setting the `omitCommonFrames` property to `false`.



### Truncate after Regex

It is possible to use regular expressions to truncate the stacktrace after the first matching stacktrace element. The strings being matched against are in the form "fullyQualifiedClassName.methodName".

For example, to suppress everything below `org.company.stack.framework` package or after a call to the `StackGenerator.one()` method, configure the following:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
        <truncateAfter>^org\.company\.stack\.framework\..*</truncateAfter>
        <truncateAfter>^\.StackGenerator\.one</truncateAfter>
    </throwableConverter>
</encoder>
```

This will produce something similar to the following:

```
java.lang.RuntimeException: Unable to invoke service
	at org.company.stack.gen.StackGenerator.causedBy(StackGenerator.java:40)
	at org.company.stack.gen.StackGenerator.generateCausedBy(StackGenerator.java:34)
	at org.company.stack.framework.Dispatcher.invoke(Dispatcher.java:11)
	... 4 frames truncated
Caused by: java.lang.RuntimeException: Destination unreachable
	at org.company.stack.gen.StackGenerator.two(StackGenerator.java:58)
	at org.company.stack.gen.StackGenerator.one(StackGenerator.java:55)
	... 7 frames truncated (including 6 common frames)
```

Note how the stacktrace is truncated _after_ the matching frames.

Alternatively, multiple regex patterns can be specified at once using the `<truncateAfters>` configuration keyword. This property accepts an optional comma separated list of patterns. The previous example configuration can also be written as follows:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
        <truncateAfters>
            ^org\.company\.stack\.framework\..*,
            ^\.StackGenerator\.one
        </truncateAfters>
    </throwableConverter>
</encoder>
```

Using the `<truncateAfters>` configuration option can be useful when using an environment variable to specify the actual patterns at deployment time.



### Exclude Frames per Regex

Sometimes portions of the stacktrace are not worthy of interest and you want to exclude them to make the overall stacktrace shorter. The encoder allows to filter out consecutive unwanted stacktrace elements based on regular expressions and replace them with a single message indicating "something" has been removed.

To enable this feature, simply define the regex patterns matching the frames you want to exclude using one or more `<exclude>` configuration keywords.

Take the following stacktrace as an example:

```
java.lang.RuntimeException: Destination unreachable
	at org.company.stack.gen.StackGenerator.two(StackGenerator.java:58)
	at org.company.stack.gen.StackGenerator.one(StackGenerator.java:55)
	at org.company.stack.gen.StackGenerator.threeSingle$SpringCGLIB(StackGenerator.java:14)
	at org.company.stack.gen.StackGenerator.twoSingle$SpringCGLIB(StackGenerator.java:11)
	at org.company.stack.gen.StackGenerator.oneSingle$SpringCGLIB(StackGenerator.java:8)
	at org.company.stack.gen.StackGenerator.generateSingle(StackGenerator.java:5)
	at org.company.stack.framework.Dispatcher.invoke(Dispatcher.java:11)
	at org.company.stack.framework.Dispatcher.dispatch(Dispatcher.java:8)
	at org.company.stack.Sample.execute(Sample.java:107)
	at org.company.stack.Sample.exclude(Sample.java:94)
	at org.company.stack.Sample.main(Sample.java:19)
```

Suppose the last three frames are common to all your exceptions (they come from the application bootstrap) and you want to reduce them to a single line for brevetiy. Also, you are not interested in frames with `package.classname` ending with `$SpringCGLIB` because they are generated by the Spring runtime... To do this, you will use the following configuration:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
        <exclude>\$SpringCGLIB$</exclude>
        <exclude>^org\.company\.stack\.Sample\..*</exclude>
    </throwableConverter>
</encoder>
```

And your stacktrace would be rendered as follows:

```
java.lang.RuntimeException: Destination unreachable
	at org.company.stack.gen.StackGenerator.two(StackGenerator.java:58)
	at org.company.stack.gen.StackGenerator.one(StackGenerator.java:55)
	... 3 frames excluded
	at org.company.stack.gen.StackGenerator.generateSingle(StackGenerator.java:5)
	at org.company.stack.framework.Dispatcher.invoke(Dispatcher.java:11)
	at org.company.stack.framework.Dispatcher.dispatch(Dispatcher.java:8)
	... 3 frames excluded
```

Note that the converter effectively removes stack trace elements only if at least **TWO** consecutive frames match the configured regex patterns. This is to avoid replacing a single frame with "... 1 frames excluded" that doesn't shorten the stacktrace at all...

In addition, the first frame of the stacktrace is always output and cannot be excluded.


Alternatively, multiple exclusion patterns can be specified at once using the `<exclusions>` configuration keyword. This property accepts an optional comma separated list of patterns. The previous example configuration can also be written as follows:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
        <exclusions>
            \$SpringCGLIB$,
            ^org\.company\.stack\.Sample\..*
        </exclusions>
    </throwableConverter>
</encoder>
```

Using the `<exclusions>` configuration option can be useful when using an environment variable to specify the actual patterns at deployment time.



### Maximum Depth per Throwable

The `maxDepthPerThrowable` property is used to limit the depth of each individual throwable nested inside the original exception, caused-bys and suppressed exceptions included. Beyond this limit, additional elements are omitted and a message indicating the number elements removed is added instead.

For example, the following configuration limits the stacktrace to 2 elements:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
        <maxDepthPerThrowable>2</maxDepthPerThrowable>
    </throwableConverter>
</encoder>
```

This would produce something similar to the following:

```
java.lang.RuntimeException: Unable to invoke service
	at org.company.stack.gen.StackGenerator.causedBy(StackGenerator.java:40)
	at org.company.stack.gen.StackGenerator.generateCausedBy(StackGenerator.java:34)
	... 5 frames truncated
Caused by: java.lang.RuntimeException: Destination unreachable
	at org.company.stack.gen.StackGenerator.two(StackGenerator.java:58)
	at org.company.stack.gen.StackGenerator.one(StackGenerator.java:55)
	... 7 frames truncated (including 6 common frames)
```

Note how the maximum depth applies to each individual throwables. The last message indicates that 7 frames were truncated of which 6 are common to both the exception and the cause.

The special value `-1` can be used to disable the feature and allow for an unlimited depth (no limit), which is the default.



### Maximum Trace Size (bytes)

The `maxLength` property is used to set a limit on the size of the overall trace, all throwables combined.

For example, use the following configuration to limit the size to `256` characters:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
        <maxLength>256</maxLength>
    </throwableConverter>
</encoder>
```

The overall trace will be limited to 256 characters like this:

```
java.lang.RuntimeException: Unable to invoke service
	at org.company.stack.gen.StackGenerator.causedBy(StackGenerator.java:40)
	at org.company.stack.gen.StackGenerator.generateCausedBy(StackGenerator.java:34)
	at org.company.stack.framework.Dispatcher....
```

The special value `-1` can be used to disable the feature and allow for an unlimited length (no limit), which is the default.



### Classname Shortening

Class names can be abbreviated in a way similar to the Logback layout [feature](https://logback.qos.ch/manual/layouts.html#logger).
The algorithm will shorten the full class name (package + class) and attempt to reduce its size down to a maximum of number of characters.
It does so by reducing the package elements to their first letter and gradually expand them starting from the right most element until the maximum size is reached.

To enable this feature, set the `shortenedClassNameLength` property to the desired value.
Setting the length to zero constitutes an exception and returns the "simple" class name without package name.
Set length to `-1` to disable shortening entirely.

The next table provides examples of the abbreviation algorithm in action.

|LENGTH|CLASSNAME                   |SHORTENED                  |
|------|----------------------------|---------------------------|
|0     | `org.company.stack.Sample` | `Sample`                  |
|5     | `org.company.stack.Sample` | `o.c.s.Sample`            |
|16    | `org.company.stack.Sample` | `o.c.stack.Sample`        |
|22    | `org.company.stack.Sample` | `o.company.stack.Sample`  |
|25    | `org.company.stack.Sample` | `org.company.stack.Sample`|


For example, use the following configuration to try to shorten the class names down to 25 characters:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
        <shortenedClassNameLength>25</shortenedClassNameLength>
    </throwableConverter>
</encoder>
```

This will produce an output similar to this:

```
j.lang.RuntimeException: Unable to invoke service
	at o.c.s.gen.StackGenerator.causedBy(StackGenerator.java:40)
	at o.c.s.gen.StackGenerator.generateCausedBy(StackGenerator.java:34)
	at o.c.s.f.Dispatcher.invoke(Dispatcher.java:11)
	at o.c.s.f.Dispatcher.dispatch(Dispatcher.java:8)
	at org.company.stack.Sample.execute(Sample.java:97)
	at org.company.stack.Sample.classNameShortening(Sample.java:77)
	at org.company.stack.Sample.main(Sample.java:19)
Caused by: j.lang.RuntimeException: Destination unreachable
	at o.c.s.gen.StackGenerator.two(StackGenerator.java:58)
	at o.c.s.gen.StackGenerator.one(StackGenerator.java:55)
	at o.c.s.gen.StackGenerator.causedBy(StackGenerator.java:38)
	... 6 common frames omitted
```

Note that the exception name is also shortened, as are the individual frames.

Alternatively you can specify your own custom abbreviation strategy with the `<classNameAbbreviator>` configuration property as shown below:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <throwableConverter>
        <classNameAbbreviator class="your.own.CustomAbbreviator">
            <param1>aValue</param1>
        </classNameAbbreviator>
    </throwableConverter>
</encoder>
```

> **Note**
> The value of `<shortenedClassNameLength>` property is ignored when a custom abbreviator is explicitly specified.



### Custom Line Separator

Stacktrace elements are sperated by the `SYSTEM` line separator by default. 
The `linesSeparator` property can be used to specify a different value. The line separator can be specified as:

* `SYSTEM` (uses the system default)
* `UNIX` (uses `\n`)
* `WINDOWS` (uses `\r\n`), or
* any other string.

For example, to use a pipe (`|`) as separator between stacktrace elements you would use the following configuration:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
        <lineSeparator>|</lineSeparator>
    </throwableConverter>
</encoder>
```

The stacktrace will be rendered on a single line with `|` between frames as follows (the line is truncated for readability):

```
java.lang.RuntimeException: Unable to invoke service|	at org.company.stack.gen.StackGenerator.causedBy(StackGenerator.java:40)|	at org.c
```


### Root Cause First

Stacktraces are usually rendered with the root cause appearing last.
You can invert the order and have the root cause output first by setting the `rootCauseFirst` property to `true` (`false` by default).

Sample output:

```
java.lang.RuntimeException: Destination unreachable
	at org.company.stack.gen.StackGenerator.two(StackGenerator.java:58)
	at org.company.stack.gen.StackGenerator.one(StackGenerator.java:55)
	at org.company.stack.gen.StackGenerator.causedBy(StackGenerator.java:38)
	... 6 common frames omitted
Wrapped by: java.lang.RuntimeException: Unable to invoke service
	at org.company.stack.gen.StackGenerator.causedBy(StackGenerator.java:40)
	at org.company.stack.gen.StackGenerator.generateCausedBy(StackGenerator.java:34)
	at org.company.stack.framework.Dispatcher.invoke(Dispatcher.java:11)
	at org.company.stack.framework.Dispatcher.dispatch(Dispatcher.java:8)
	at org.company.stack.Sample.execute(Sample.java:79)
	at org.company.stack.Sample.rootCauseFirst(Sample.java:66)
	at org.company.stack.Sample.main(Sample.java:18)
```


### Conditional Output

Standard Logback [EventEvaluators](https://logback.qos.ch/manual/filters.html#evalutatorFilter) can be used to determine if the stacktrace should be rendered.

EventEvaluators are used to _skip_ generation of the stack trace for matching ILoggingEvents. In other words, an evaluator must evaluate to `false` (do not skip) to include the stacktrace...

The following sample configuration leverage the Logback [JaninoEventEvaluator](https://logback.qos.ch/manual/filters.html#JaninoEventEvaluator) event evaluator to output the stacktrace only if the log message contains the word `billing`:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
        <evaluator class="ch.qos.logback.classic.boolex.JaninoEventEvaluator">
            <expression>return !message.contains("billing");</expression>
        </evaluator>
    </throwableConverter>
</encoder>
```

Multiple evaluators can be registered and are evaluated in the order in which they are registered. The stacktrace is only generated if all evaluators returned `false`.



### Stack Hashes

**To Be Documented**

Computing and inlining hexadecimal hashes for each exception stack using the `inlineHash` or `stackHash` provider ([more info](stack-hash.md)).



### Using with PatternLayout

To use this with a PatternLayout, you must configure a new "conversionRule" as described [here](http://logback.qos.ch/manual/layouts.html#customConversionSpecifier). 

For example:

```xml
<!-- Define a new conversion rule named "stack" -->
<conversionRule conversionWord="stack"
                converterClass="net.logstash.logback.stacktrace.ShortenedThrowableConverter" />
```

This configuration registers the `ShortenedThrowableConverter` under the name `stack`. From there the converter can be used in a PatternLayout using the syntax `%stack{options}` with optional configuration options between `{}`, each separated by a comma.

The first three options must appear in the following order:

1. maxDepthPerThrowable - `full` or `short` or an integer value
2. shortenedClassNameLength - `full` or `short` or an integer value
3. maxLength - `full` or `short` or an integer value

The remaining options can appear in any order and are interpreted as follows:

- keyword `rootFirst` - indicating that stacks should be printed root-cause first
- keyword `inlineHash` - indicating that hexadecimal error hashes should be computed and inlined
- keyword `inline` - indicating that the whole stack trace should be inlined, using `\\n` as separator
- keyword `omitCommonFrames` - indicating that common frames should be omitted
- keyword `keepCommonFrames` - indicating that common frames should be preserved
- any other string:
	- first evaluated as the name of a registered Evaluator that will determine if the stacktrace is ignored,
	- if no evaluator is found with that name, the string is interpreted as a regex pattern for stack trace elements to exclude

For example,

```xml
<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <pattern>[%thread] - %msg%n%stack{5,1024,10,rootFirst,omitCommonFrames,regex1,regex2,evaluatorName}</pattern>
    </encoder>
</appender>
```

Note that it is not possible to configure the `truncateAfter` feature when the converter is used within a pattern layout.



## Registering Additional Providers

`LogstashEncoder`, `LogstashAccessEncoder` and their "layout" counterparts all come with a predefined set of encoders. You can register additional JsonProviders using the `<provider>` configuration property as shown in the following example:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <!-- Add a new provider after those than come with the LogstashEncoder -->
    <provider class="net.logstash.logback.composite.loggingevent.LoggingEventPatternJsonProvider">
        <pattern>
          {
             "message": "%mdc{custom_value} %message"
          }
        </pattern>
    </provider>

    <!-- Disable the default message provider -->
    <fieldNames>
        <message>[ignore]</message>
    </fieldNames>
</encoder>
```

You can add several additional JsonProviders using multiple `<provider>` entries. They will appear just after the default providers registered by the LogstashEncoder.

In this example, the pattern provider produces a "message" JSON field that will conflict with the message field produced by the MessageJsonProvider already registered by the LogstashEncoder itself. Different options to avoid the conflict:

- you instruct LogstashEncoder to use a different field name using the [fieldNames](#customizing-standard-field-names) configuration property;
- you disable the message provider that comes with the encoder (that's the option illustrated in the example above);
- you use a different field name in your pattern.


## Prefix/Suffix/Separator

You can specify a prefix (written before the JSON object),
suffix (written after the JSON object),
and/or line separator (written after suffix),
which may be required for the log pipeline you are using, such as:

* If you are using the Common Event Expression (CEE) format for syslog, you need to add the `@cee:` prefix.
* If you are using other syslog destinations, you might need to add the standard syslog headers.
* If you are using Loggly, you might need to add your customer token.

For example, to add standard syslog headers for syslog over UDP, configure the following:

```xml
<configuration>
    <conversionRule conversionWord="syslogStart" converterClass="ch.qos.logback.classic.pattern.SyslogStartConverter"/>

    <appender name="stash" class="net.logstash.logback.appender.LogstashUdpSocketAppender">
        <host>MyAwesomeSyslogServer</host>
        <!-- port is optional (default value shown) -->
        <port>514</port>
        <layout>
            <prefix class="ch.qos.logback.classic.PatternLayout">
                <pattern>%syslogStart{USER}</pattern>
            </prefix>
        </layout>
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

The line separator, which is written after the suffix, can be specified as:
* `SYSTEM` (uses the system default)
* `UNIX` (uses `\n`)
* `WINDOWS` (uses `\r\n`), or
* any other string.

For example:

```xml
<configuration>
    ...
    <appender ...>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            ...
            <lineSeparator>UNIX</lineSeparator>
        </encoder>
    </appender>
</configuration>
```

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

These encoders/layouts make use of an internal buffer to hold the JSON output during the rendering process. 
The size of this buffer is set to `1024` bytes by default. A different size can be configured by setting the `minBufferSize` property to the desired value.
The buffer automatically grows above the `minBufferSize` when needed to accommodate with larger events. However, only the first `minBufferSize` bytes will be reused by subsequent invocations. It is therefore strongly advised to set the minimum size at least equal to the average size of the encoded events to reduce unnecessary memory allocations and reduce pressure on the garbage collector.

### Providers common to LoggingEvents and AccessEvents

The table below lists the providers available to both _LoggingEvents_ and _AccessEvents_.
The provider name is the xml element name to use when configuring.

<table>
  <tbody>
    <tr>
      <th>Provider</th>
      <th>Description/Properties</th>
    </tr>
    <tr>
      <td valign="top"><tt>context</tt></td>
      <td><p>Outputs entries from logback's context.</p>
        <ul>
          <li><tt>fieldName</tt> - Sub-object field name (no sub-object)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>nestedField</tt></td>
      <td>
        <p>Nests a JSON object under the configured fieldName.</p>
        <p>The nested object is populated by other providers added to this provider.</p>
        <p>See <a href="#nested-json-provider">Nested JSON provider</a>.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name</li>
          <li><tt>providers</tt> - The providers that should populate the nested object.</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>pattern</tt></td>
      <td>
        <p>Outputs fields from a configured JSON Object string,
           while substituting patterns supported by logback's <tt>PatternLayout</tt>.
        </p>
        <p>
           See <a href="#pattern-json-provider">Pattern JSON Provider</a>
        </p>
        <ul>
          <li><tt>pattern</tt> - JSON object string (no default)</li>          
          <li><tt>omitEmptyFields</tt> - whether to omit fields with empty values (<tt>false</tt>)</li>          
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>sequence</tt></td>
      <td>
        <p>Event sequence number.
        </p>
        <p>With Logback 1.3+ the sequence number is obtained from the event itself as long as the LoggerContext is configured with a `SequenceNumberGenerator` (which is not by default).
If no SequenceNumberGenerator is configured, the provider emits a warning and reverts to a locally generated incrementing number starting at 1.
        </p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>sequence</tt>)</li>
          <li><tt>sequenceProvider</tt> - Alternate strategy to obtain the sequence number associated with the supplied event. Must implement `Function<ILoggingEvent, Long>` or `Function<IAccessEvent, Long>` depending on the type of event to process.
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>threadName</tt></td>
      <td><p>Name of the thread from which the event was logged.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>thread_name</tt>)</li>          
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>timestamp</tt></td>
      <td><p>Event timestamp.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>@timestamp</tt>)</li>
          <li><tt>pattern</tt> - Output format (<tt>[ISO_OFFSET_DATE_TIME]</tt>)  See <a href="#customizing-timestamp">Customizing Timestamp</a> for possible values.</li>
          <li><tt>timeZone</tt> - Timezone (system timezone)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>uuid</tt></td>
      <td>
        <p>
          Outputs random UUID as field value. Handy when you want to provide unique identifier
          for log lines.
        </p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>uuid</tt>)</li>
          <li><tt>strategy</tt> - UUID generation strategy (<tt>random</tt>). Supported options: <ul><li><tt>random</tt> - for Type 4 UUID</li>
          <li><tt>time</tt> - for Type 1 time based UUID</li>
          </ul></li>
          <li><tt>ethernet</tt> - Only for 'time' strategy. When defined - MAC address to use for location part of UUID. Set it to <tt>interface</tt> value to use real underlying network interface or to specific values like <tt>00:C0:F0:3D:5B:7C</tt></li>          
        </ul>
          <p>Note: The <a href="https://mvnrepository.com/artifact/com.fasterxml.uuid/java-uuid-generator/"><tt>com.fasterxml.uuid:java-uuid-generator</tt></a> optional dependency must be added to applications that use the `uuid` provider.</p>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>version</tt></td>
      <td><p>Logstash JSON format version.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>@version</tt>)</li>
          <li><tt>version</tt> - Output value (<tt>1</tt>)</li>
          <li><tt>writeAsInteger</tt> - Write the version as a integer value (<tt>false</tt> = write as a string value)</li>
        </ul>
      </td>
    </tr>
  </tbody>
</table>


### Providers for LoggingEvents

The [common providers mentioned above](#providers-common-to-loggingevents-and-accessevents), and the providers listed in the table below, are available for _LoggingEvents_.
The provider name is the xml element name to use when configuring. Each provider's configuration properties are shown, with default configuration values in parenthesis.

<table>
  <tbody>
    <tr>
      <th>Provider</th>
      <th>Description/Properties</th>
    </tr>
    <tr>
      <td valign="top"><tt>arguments</tt></td>
      <td>
        <p>Outputs fields from the event arguments array.</p>
        <p>See <a href="#event-specific-custom-fields">Event-specific Custom Fields</a>.</p>
        <ul>
          <li><tt>fieldName</tt> - Sub-object field name (no sub-object)</li>
          <li><tt>includeNonStructuredArguments</tt> - Include arguments that are not an instance
          of <a href="/src/main/java/net/logstash/logback/argument/StructuredArgument.java"><tt>StructuredArgument</tt></a>. 
          Object field name will be <tt>nonStructuredArgumentsFieldPrefix</tt> prepend to the argument index.
          (default=false)
          </li>
          <li><tt>nonStructuredArgumentsFieldPrefix</tt> - Object field name prefix (default=arg)</li>
        </ul>
      </td>
    </tr> 
    <tr>
      <td valign="top"><tt>callerData</tt></td>
      <td><p>Outputs data about from where the logger was called (class/method/file/line).</p>
        <ul>
          <li><tt>fieldName</tt> - Sub-object field name (no sub-object)</li>
          <li><tt>classFieldName</tt> - Field name for class name (<tt>caller_class_name</tt>)</li>
          <li><tt>methodFieldName</tt> - Field name for method name (<tt>caller_method_name</tt>)</li>
          <li><tt>fileFieldName</tt> - Field name for file name (<tt>caller_file_name</tt>)</li>
          <li><tt>lineFieldName</tt> - Field name for line number (<tt>caller_line_number</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>contextName</tt></td>
      <td><p>Outputs the name of logback's context.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>context</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>loggerName</tt></td>
      <td><p>Name of the logger that logged the message.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>logger_name</tt>)</li>          
          <li><tt>shortenedLoggerNameLength</tt> - Length to which the name will be attempted to be abbreviated (no abbreviation)</li>          
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>logLevel</tt></td>
      <td><p>Logger level text (INFO, WARN, etc).</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>level</tt>)</li>          
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>logLevelValue</tt></td>
      <td><p>Logger level numerical value.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>level_value</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>logstashMarkers</tt></td>
      <td><p>Used to output Logstash Markers as specified in <em>Event-specific Custom Fields</em>.</p>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>mdc</tt></td>
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
          <li><tt>excludeMdcKeyName</tt> - Name of keys to exclude (none)</li>
          <li><tt>mdcKeyFieldName</tt> - Strings in the form <tt>mdcKeyName=fieldName</tt>
              that specify an alternate field name to output for specific MDC key (none)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>keyValuePairs</tt></td>
      <td>
        <p>Outputs key value pairs added via slf4j's fluent api.
           Will include all key value pairs by default.
           When key names are specified for inclusion, then all other keys will be excluded.
           When key names are specified for exclusion, then all other keys will be included.
           It is a configuration error to specify both included and excluded key names.
        </p>
        <ul>
          <li><tt>fieldName</tt> - Sub-object field name (no sub-object)</li>
          <li><tt>includeKeyName</tt> - Name of keys to include (all)</li>
          <li><tt>excludeKeyName</tt> - Name of keys to exclude (none)</li>
          <li><tt>keyFieldName</tt> - Strings in the form <tt>keyName=fieldName</tt>
              that specify an alternate field name to output for specific key (none)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>message</tt></td>
      <td><p>Formatted log event message.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>message</tt>)</li>
          <li><tt>messageSplitRegex</tt> - If null or empty, write the message text as is (the default behavior).
              Otherwise, split the message text using the specified regex and write it as an array.
              See the <a href="#customizing-message">Customizing Message</a> section for details.</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>rawMessage</tt></td>
      <td><p>Raw log event message, as opposed to formatted log where parameters are resolved.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>raw_message</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>rootStackTraceElement</tt></td>
      <td><p>(Only if a throwable was logged) Outputs a JSON Object containing the class and method name from which the outer-most exception was thrown.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>root_stack_trace_element</tt>)</li>
          <li><tt>classFieldName</tt> - Field name containing the class name from which the outermost exception was thrown (<tt>class_name</tt>)</li>
          <li><tt>methodFieldName</tt> - Field name containing the method name from which the outermost exception was thrown (<tt>method_name</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>stackHash</tt></td>
      <td><p>(Only if a throwable was logged) Computes and outputs a hexadecimal hash of the throwable stack.</p>
        <p>This helps identifying several occurrences of the same error (<a href="stack-hash.md">more info</a>).</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>stack_hash</tt>)</li>
          <li><tt>exclude</tt> - Regular expression pattern matching <i>stack trace elements</i> to exclude when computing the error hash</li>
          <li><tt>exclusions</tt> - Comma separated list of regular expression patterns matching <i>stack trace elements</i> to exclude when computing the error hash</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>stackTrace</tt></td>
      <td><p>Stacktrace of any throwable logged with the event. Stackframes are separated by newline chars.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>stack_trace</tt>)</li>
          <li><tt>throwableConverter</tt> - The <tt>ThrowableHandlingConverter</tt> to use to format the stacktrace (<tt>stack_trace</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>tags</tt></td>
      <td><p>Outputs logback markers as a comma separated list.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>tags</tt>)</li>          
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>throwableClassName</tt></td>
      <td><p>(Only if a throwable was logged) Outputs a field that contains the class name of the thrown Throwable.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>throwable_class</tt>)</li>
          <li><tt>useSimpleClassName</tt> - When true, the throwable's simple class name will be used. When false, the fully qualified class name will be used. (<tt>true</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>throwableMessage</tt></td>
      <td><p>(Only if a throwable was logged) Outputs a field that contains the message of the thrown Throwable.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>throwable_message</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>throwableRootCauseClassName</tt></td>
      <td><p>(Only if a throwable was logged and a root cause could be determined) Outputs a field that contains the class name of the root cause of the thrown Throwable.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>throwable_root_cause_class</tt>)</li>
          <li><tt>useSimpleClassName</tt> - When true, the throwable's simple class name will be used. When false, the fully qualified class name will be used. (<tt>true</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>throwableRootCauseMessage</tt></td>
      <td><p>(Only if a throwable was logged and a root cause could be determined) Outputs a field that contains the message of the root cause of the thrown Throwable.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>throwable_root_cause_message</tt>)</li>
        </ul>
      </td>
    </tr>
  </tbody>
</table>



### Providers for AccessEvents  

The [common providers mentioned above](#providers-common-to-loggingevents-and-accessevents), and the providers listed in the table below, are available for _AccessEvents_.
The provider name is the xml element name to use when configuring. Each provider's configuration properties are shown, with default configuration values in parenthesis.

<table>
  <tbody>
    <tr>
      <th>Provider</th>
      <th>Description/Properties</th>
    </tr>
    <tr>
      <td valign="top"><tt>contentLength</tt></td>
      <td><p>Content length.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>content_length</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>elapsedTime</tt></td>
      <td><p>Elapsed time in milliseconds.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>elapsed_time</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>message</tt></td>
      <td><p>Message in the form `${remoteHost} - ${remoteUser} [${timestamp}] "${requestUrl}" ${statusCode} ${contentLength}`.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>message</tt>)</li>
          <li><tt>pattern</tt> - Output format of the timestamp (<tt>[ISO_OFFSET_DATE_TIME]</tt>). See <a href="#customizing-timestamp">above</a> for possible values.</li>
          <li><tt>timeZone</tt> - Timezone (system timezone)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>method</tt></td>
      <td><p>HTTP method.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>method</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>protocol</tt></td>
      <td><p>HTTP protocol.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>protocol</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>remoteHost</tt></td>
      <td><p>Remote Host.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>remote_host</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>remoteUser</tt></td>
      <td><p>Remote User.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>remote_user</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>requestedUri</tt></td>
      <td><p>Requested URI.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>requested_uri</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>requestedUrl</tt></td>
      <td><p>Requested URL.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>requested_url</tt>)</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td valign="top"><tt>requestHeaders</tt></td>
      <td><p>Include the request headers.</p>
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
      <td valign="top"><tt>responseHeaders</tt></td>
      <td><p>Include the response headers.</p>
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
      <td valign="top"><tt>statusCode</tt></td>
      <td><p>HTTP status code.</p>
        <ul>
          <li><tt>fieldName</tt> - Output field name (<tt>status_code</tt>)</li>
        </ul>
      </td>
    </tr>
  </tbody>
</table>


### Nested JSON Provider

Use the `nestedField` provider to create a sub-object in the JSON event output.

For example...

```xml
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

```json
{
    "@timestamp": "...",
    "fields": {
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
    "@timestamp": "...",
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

* `#asLong{...}` - evaluates the pattern in curly braces and then converts resulting string to a Long (or a `null` if conversion fails).
* `#asDouble{...}` - evaluates the pattern in curly braces and then converts resulting string to a Double (or a `null` if conversion fails).
* `#asBoolean{...}`- evaluates the pattern in curly braces and then converts resulting string to a Boolean. Conversion is case insensitive. `true`, `yes`, `y` and `1` (case insensitive) are converted to a boolean `true`, a `null` or empty string is converted to `null`, anything else returns `false`.
* `asNullIfEmpty{...}` - evaluates the pattern in curly braces and the converts resulting string into `null` if it is empty.
* `#asJson{...}` - evaluates the pattern in curly braces and then converts resulting string to json (or a `null` if conversion fails).
* `#tryJson{...}` - evaluates the pattern in curly braces and then converts resulting string to json (or just the string if conversion fails).

So this example...

```xml
<pattern>
    {
        "line_str": "%line",
        "line_long": "#asLong{%line}",
        "has_message": "#asBoolean{%mdc{hasMessage}}",
        "json_message": "#asJson{%message}"
    }
</pattern>
```

... and this logging code...

```java
MDC.put("hasMessage", "true");
LOGGER.info("{\"type\":\"example\",\"msg\":\"example of json message with type\"}");
```

...will produce something like...

```json
{
    "line_str": "97",
    "line_long": 97,
    "has_message": true,
    "json_message": {"type":"example","msg":"example of json message with type"}
}
```

Note that the value that is sent for `line_long` is a number even though in your pattern it is a quoted text.
And the `json_message` field value is a json object, not a string.

You can escape an operation by prefixing it with `\` if you don't want it to be interpreted.


#### Omitting fields with empty values
 
The pattern provider can be configured to omit fields with the following _empty_ values:
* `null`
* empty string (`""`)
* empty array (`[]`)
* empty object (`{}`)
* objects containing only fields with empty values
* arrays containing only empty values

To omit fields with empty values, configure `omitEmptyFields` to `true` (default is `false`), like this:

```xml
<encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
    <providers>
        <pattern>
            <omitEmptyFields>true</omitEmptyFields>
            <pattern>
                {
                    "logger": "%logger",
                    "level": "%level",
                    "thread": "%thread",
                    "message": "%message",
                    "traceId": "%mdc{traceId}"
                }
            </pattern>
        </pattern>
    </providers>
</encoder>
```

If the MDC did not contain a `traceId` entry, then a JSON log event from the above pattern would not contain the `traceId` field...

```json
{
    "logger": "com.example...",
    "level": "DEBUG",
    "thread": "exec-1",
    "message": "Hello World!"
}
```

#### LoggingEvent patterns

For LoggingEvents, conversion specifiers from logback-classic's
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

Note that the [`%property{key}`](https://logback.qos.ch/manual/layouts.html#property) conversion specifier behaves slightly differently when used in the context of the Pattern Json provider. If the property cannot be found in the logger context or the System properties, it returns **an empty string** instead of `null` as it would normally do. For example, assuming the "foo" property is not defined, `%property{foo}` would return `""` (an empty string) instead of `"null"` (a string whose content is made of 4 letters).

The _property_ conversion specifier also allows you to specify a default value to use when the property is not defined. The default value is optional and can be specified using the `:-` operator as in Bash shell. For example, assuming the "foo" property is not defined, `%property{foo:-bar}` will return `bar`.


#### AccessEvent patterns

For AccessEvents, conversion specifiers from logback-access's
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

* `#nullNA{...}` - if the pattern in curly braces evaluates to a dash (`-`), it will be replaced with a `null` value.

You may want to use it because many of the `PatternLayout` conversion words from logback-access will evaluate to `-`
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

```json
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

You can do something similar for `AccessEventCompositeJsonEncoder` and `LogstashAccessEncoder` as well, if your `JsonProvider` handles `IAccessEvent`s.


## Status Listeners

During execution, the encoders/appenders/layouts provided in logstash-logback-encoder
will add logback status messages to the logback [`StatusManager`](https://logback.qos.ch/apidocs/ch/qos/logback/core/status/StatusManager.html).
These status messages are typically reported via a logback [`StatusListener`](https://logback.qos.ch/apidocs/ch/qos/logback/core/status/StatusListener.html).

Since the [async appenders](#async-appenders) (especially the [tcp appenders](#tcp-appenders))
report warnings and errors via the status manager, a default status listener that
outputs WARN and ERROR level status messages to standard out
will be registered on startup if a status listener has not already been registered.
To disable the automatic registering of the default status listener by an appender, do one of the following:
* register a different logback [status listener](https://logback.qos.ch/manual/configuration.html#dumpingStatusData), or
* set `<addDefaultStatusListener>false</addDefaultStatusListener` in each async appender.


## Joran/XML Configuration

Configuring Logback using XML is handled by Logback's Joran configuration system. This section is a short description of the high level data types supported by Joran. For more information, please refer to the [official documentation](http://logback.qos.ch/manual).

### Duration property

Duration represents a laps of time.
It can be specified as an integer value representing a number of milliseconds, or a string such as "20 seconds", "3.5 minutes" or "5 hours" that will be automatically  converted by logback's configuration system into Duration instances.
The recognized units of time are the `millisecond`, `second`, `minute`, `hour` and `day`. The unit name may be followed by an "s". Thus, "2000 millisecond" and "2000 milliseconds" are equivalent. In the absence of a time unit specification, milliseconds are assumed.

The following examples are therefore equivalent:

```xml
<duration>2000</duration>
<duration>2000 millisecond</duration>
<duration>2000 milliseconds</duration>
```

### Comma separated list of values

When specified, some properties accept a comma-separated list of values. 

Leading and trailing whistespace characters are removed from each value during the decoding process, including tabs (`\t`), carriage return (`\r`) and line feeds (`\n`) - see the `String.trim()` function for more information. It is therefore safe to add an extra blank after the comma or write the values on multiple lines for better readability.

The examples below are equivalent and produce a list containing the values `valueA`, `valueB`, and `valueC`:

```xml
<property>valueA, valueB, valueC</property>

<property>
    valueA,
    valueB,
    valueC
</property>
```

Also, multiple consecutive comma are treated as a single delimiter. This allows you to write a generic configuration as follows where the actual value comes from an external environment variable whose value may be empty.

```xml
<property>valueA, ${ENV_VAR}, valueC</property>
```

If needed, the comma delimiter may be escaped by prefixing it with a backslash (`\`) to treat it as being part of the value instead of considered as an actual delimiter. The example below defines a list containing one single element whose value is the string `foo,bar`:

```xml
<property>foo\,bar</property>
```


## Profiling

<a href="https://www.yourkit.com/java/profiler/"><img src="https://www.yourkit.com/images/yk_logo.svg" alt="YourKit Logo" height="22"/></a>

Memory usage and performance of logstash-logback-encoder have been improved
by addressing issues discovered with the help of the
[YourKit Java Profiler](https://www.yourkit.com/java/profiler/).

YourKit, LLC has graciously donated a free license of the
[YourKit Java Profiler](https://www.yourkit.com/java/profiler/)
to this open source project.
