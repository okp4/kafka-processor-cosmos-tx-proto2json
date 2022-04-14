# Kafka Processor Template

> Template for Kafka Streams Processor project @okp4.

[![version](https://img.shields.io/github/v/release/okp4/template-kafka-processor)](https://github.com/okp4/template-kafka-processor/releases)
[![build](https://github.com/okp4/template-kafka-processor/actions/workflows/build.yml/badge.svg)](https://github.com/okp4/template-kafka-processor/actions/workflows/build.yml)
[![lint](https://github.com/okp4/template-kafka-processor/actions/workflows/lint.yml/badge.svg)](https://github.com/okp4/template-kafka-processor/actions/workflows/lint.yml)
[![test](https://github.com/okp4/template-kafka-processor/actions/workflows/test.yml/badge.svg)](https://github.com/okp4/template-kafka-processor/actions/workflows/test.yml)
[![conventional commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-yellow.svg)](https://conventionalcommits.org)
[![License](https://img.shields.io/badge/License-BSD_3--Clause-blue.svg)](https://opensource.org/licenses/BSD-3-Clause)

## Purpose

TBD

## Implementation

Implementation mainly relies on [Kafka Streams API](https://kafka.apache.org/documentation/streams), library to create
event-stream applications with the following features:

- no external dependency other than Kafka itself,
- simple and light library,
- fault-tolerant and scalable.

Moreover, this implementation:

- uses [Kotkin](https://kotlinlang.org/) as primary coding language,
- is as much as possible, lean, i.e. tries to minimize the dependencies to 3rd party libraries and the resulting package
  footprint.

## Build

This project targets the [JVM 11+](https://openjdk.java.net/), so be sure to have it available in your environment.

This project relies on the [Gradle](https://gradle.org/) build system.

If you are on windows then open a command line, go into the root directory and run:

```sh
.\gradlew build
```

If you are on linux/mac then open a terminal, go into the root directory and run:

```sh
./gradlew build
```

This command line produces 2 JAR files:

- a _regular_ JAR: `kafka-processor-X.Y.jar`
- a _fat_ JAR: `kafka-processor-X.Y-standalone.jar`

This last one is the one to use as it contains all the dependencies in it.
