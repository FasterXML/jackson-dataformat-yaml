# Overview

This project contains [Jackson](http://http://wiki.fasterxml.com/JacksonHome) extension component for reading and writing [YAML](http://en.wikipedia.org/wiki/YAML) encoded data.
[SnakeYAML](http://code.google.com/p/snakeyaml/) library is used for low-level YAML parsing.
This project adds necessary abstractions on top to make things work with other Jackson functionality.

Project is licensed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt).

# Status

Project is in its prototype phase, so:

* Basic parsing seems to work, as per simplest of unit tests; including type auto-detection (i.e. underlying `JsonParser` tries to detect implied types)
* Basic generation: not configurable, produces visually ok block format
* Even format auto-detection works

Missing are:

* Configurability: might make sense to esp. allow configuration of generation details
* Support for YAML tags (which theoretically could help with typing), aliases and anchors (which would be good for Object Id, refs): ideally these would be supported, but it will be challenging due to conceptual diffs

No Maven artifacts have been pushed; will do that if and once project gets bit more solid, independently tested.

## Maven dependency

To use this extension on Maven-based projects, use following dependency:

    <dependency>
      <groupId>com.fasterxml.jackson.dataformat</groupId>
      <artifactId>jackson-dataformat-yaml</artifactId>
      <version>2.0.0</version>
    </dependency>

# Usage

## Simple usage

Usage is as with basic `JsonFactory`; most commonly you will just construct a standard `ObjectMapper` with `com.fasterxml.jackson.dataformat.yaml.YAMLFactory`, like so:

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    User user = mapper.readValue(yamlSource, User.class);

but you can also just use underlying `YAMLFactory` and parser it produces, for event-based processing:

    YAMLFactory factory = new YAMLFactory();
    JsonParser parser = factory.createJsonParser(yamlString); // don't be fooled by method name...
    while (parser.nextToken() != null) {
      // do something!
    }

# Documentation

* [Documentation](jackson-dataformat-yaml/wiki/Documentation) IS TO BE WRITTEN
