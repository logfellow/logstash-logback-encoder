Notes on refactoring required for Jackson 3.0.
This will probably eventually turn into release notes


# Decorators

In Jackson 3.0, `ObjectMapper` and `JsonFactory` are now immutable (previously, they were mutable).

An `ObjectMapper` is now configured and created via a `MapperBuilder`.
In addition, Jackson now has subclasses of `ObjectMapper` and `MapperBuilder`
specific to different data formats (e.g. `JsonMapper` and `JsonMapper.Builder` for json,
and `YAMLMapper` and `YAMLMapper.Builder` for yaml).

A `JsonFactory` is now configured and created via a `JsonFactoryBuilder`.
In addition, Jackson now has a higher level `TokenStreamFactory` and `TokenStreamFactory.TSFBuilder`
as a superclass to support different data formats (e.g. `YAMLFactory` and `YAMLFactoryBuilder`).


Previously, logstash-logback-encoder had the following decorators:
* `JsonFactoryDecorator`
  * decorated a `JsonFactory` (and `ObjectMapper` indirectly via `jsonFactory.getCodec()`)
  * registered via `<jsonFactoryDecorator class="SomeJsonFactoryDecorator"/>`
* `JsonGeneratorDecorator`
  * decorated a `JsonGenerator`
  * registered via `<jsonGeneratorDecorator class="SomeJsonGeneratorDecorator"/>`


To support the changes in Jackson 3.0, `JsonFactoryDecorator` and all its implementations were refactored.

Now, logstash-logback-encoder has the following decorators:
* `MapperBuilderDecorator`, which decorates a `MapperBuilder` (or subclass),
* `TokenStreamFactoryDecorator`, which decorates a `TokenStreamFactory.TSFBuilder` (or subclass), and
* `JsonGeneratorDecorator`, which decorates a `JsonGenerator`.

All decorators are now registered via `<decorator class="SomeDecorator"/>`.

Note that a `JsonGeneratorDecorator` can still be used to decorate a `JsonGenerator`.
However, `JsonGeneratorDecorator` is now a generic type, and some implementations changed.

Replacements for previous decorator implementations provided by logstash-logback-encoder are as follows:

| Old                                    | New                                                                                |
| ----                                   | ----                                                                               |
| `JsonFactoryDecorator`                 | `MapperBuilderDecorator` or `TokenStreamFactoryBuilderDecorator`                   |
| `CompositeJsonFactoryDecorator`        | `CompositeMapperBuilderDecorator` or `CompositeTokenStreamFactoryBuilderDecorator` |
| `CharacterEscapesJsonFactoryDecorator` | `CharacterEscapesJsonFactoryBuilderDecorator`                                      |
| `EscapeNonAsciiJsonFactoryDecorator`   | `EscapeNonAsciiJsonFactoryBuilderDecorator`                                        |
| `CborJsonFactoryDecorator`             | see _Data Format_ section                                                          |
| `YamlJsonFactoryDecorator`             | see _Data Format_ section                                                          |
| `SmileJsonFactoryDecorator`            | see _Data Format_ section                                                          |
| `JsonGeneratorDecorator`               | `JsonGeneratorDecorator` (but now takes a generic type)                            |
| `PrettyPrintingJsonGeneratorDecorator` | `PrettyPrintingMapperBuilderDecorator`                                             |
| `MaskingJsonGeneratorDecorator`        | `MaskingJsonGeneratorDecorator` (no change)                                        |
| `FeatureJsonFactoryDecorator`          | `JsonFactoryFeatureDecorator` (see _Feature Decorators_ section)                   |
| `FeatureJsonGeneratorDecorator`        | See Feature Decorators section                                                     |
| `CborFeatureJsonGeneratorDecorator`    | `CborGeneratorFeatureDecorator` (see _Feature Decorators_ section)                 |
| `SmileFeatureJsonGeneratorDecorator`   | `SmileGeneratorFeatureDecorator` (see _Feature Decorators_ section)                |
| `YamlFeatureJsonGeneratorDecorator`    | `YamlGeneratorFeatureDecorator` (see _Feature Decorators_ section)                 |
| `NullJsonFactoryDecorator`             | removed                                                                            |
| `NullJsonGeneratorDecorator`           | removed                                                                            |


# Feature Decorators

Feature enums were refactored drastically in Jackson 3.0.
As a result, the decorators used to configure features have changed.
The following table shows which feature decorator to use to configure each Jackson feature type. 

| Jackson Feature Enum         | Feature Decorator                                                      |
| --------------------         | -----------------                                                      |
| `TokenStreamFactory.Feature` | `net.logstash.logback.decorate.TokenStreamFactoryFeatureDecorator`     |
| `MapperFeature`              | `net.logstash.logback.decorate.MapperFeatureDecorator`                 |
| `SerializationFeature`       | `net.logstash.logback.decorate.SerializationFeatureDecorator`          |
| `StreamWriteFeature`         | `net.logstash.logback.decorate.StreamWriteFeatureDecorator`            |
| `JsonFactory.Feature`        | `net.logstash.logback.dataformat.json.JsonFactoryFeatureDecorator`     |
| `JsonWriteFeature`           | `net.logstash.logback.dataformat.json.JsonWriteFeatureDecorator`       |
| `SmileGenerator.Feature`     | `net.logstash.logback.dataformat.smile.SmileGeneratorFeatureDecorator` |
| `YAMLGenerator.Feature`      | `net.logstash.logback.dataformat.yaml.YamlGeneratorFeatureDecorator`   |
| `CBORGenerator.Feature`      | `net.logstash.logback.dataformat.cbor.CborGeneratorFeatureDecorator`   |

The following old decorators were replaced by one or more of the above feature decorators
(depending on where the feature moved in Jackson 3.0):

* `FeatureJsonFactoryDecorator`
* `FeatureJsonGeneratorDecorator`
* `CborFeatureJsonGeneratorDecorator`
* `SmileFeatureJsonGeneratorDecorator`
* `YamlFeatureJsonGeneratorDecorator`


# Data Format

Previously, non-JSON data formats were enabled via a `JsonFactoryDecorator`.  For example:

```xml
  <jsonFactoryDecorator class="net.logstash.logback.decorate.smile.SmileJsonFactoryDecorator"/>
```

Now, the data format is controlled by a `DataFormatFactory`, with built-in support provided for json (the default), yaml, cbor, and smile.
For example:

```xml
  <dataFormat>smile</dataFormat>
```

Or: 

```xml
 <dataFormatFactory class="net.logstash.logback.dataformat.smile.SmileDataFormatFactory"/>
```


Also, the feature decorator class names for the configuring data format features has changed
(see also the Feature Decorators section):

| Old                                  | New                              |
| ----                                 | ----                             |
| `SmileFeatureJsonGeneratorDecorator` | `SmileGeneratorFeatureDecorator` |
| `YamlFeatureJsonGeneratorDecorator`  | `YamlGeneratorFeatureDecorator`  |
| `CborFeatureJsonGeneratorDecorator`  | `CborGeneratorFeatureDecorator`  |


# Maskers

In Jackson 3.0, `JsonStreamContext` was replaced with `TokenStreamContext`.

Therefore, references to `JsonStreamContext` in logstash-logback-encoder's `FieldMasker` and `ValueMasker` were changed to `TokenStreamContext`. 


# JsonFactoryAware

Previously, a `JsonProvider` could implement `JsonFactoryAware` to be injected with the `JsonFactory`.

Now, `JsonFactoryAware` has been removed.
Instead, a `JsonProvider` can implement `ObjectMapperAware` to be injected with the `ObjectMapper`.

This change was made because in Jackson 3.0:
* `JsonFactory` is now specific to json (i.e. other data formats use subclasses of `TokenStreamFactory`),
* An `ObjectMapper` can no longer be retrieved from a `JsonFactory`, but a `TokenStreamFactory` can be retrieved from an `ObjectMapper`.