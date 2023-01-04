# Kafka Processor Decode Cosmos TX

[![version](https://img.shields.io/github/v/release/okp4/kafka-processor-cosmos-tx-proto2json?style=for-the-badge&logo=github)](https://github.com/okp4/kafka-processor-cosmos-tx-proto2json/releases)
[![lint](https://img.shields.io/github/actions/workflow/status/okp4/kafka-processor-cosmos-tx-proto2json/lint.yml?branch=main&label=lint&style=for-the-badge&logo=github)](https://github.com/okp4/kafka-processor-cosmos-tx-proto2json/actions/workflows/lint.yml)
[![build](https://img.shields.io/github/actions/workflow/status/okp4/kafka-processor-cosmos-tx-proto2json/build.yml?branch=main&label=build&style=for-the-badge&logo=github)](https://github.com/okp4/kafka-processor-cosmos-tx-proto2json/actions/workflows/build.yml)
[![test](https://img.shields.io/github/actions/workflow/status/okp4/kafka-processor-cosmos-tx-proto2json/test.yml?branch=main&label=test&style=for-the-badge&logo=github)](https://github.com/okp4/kafka-processor-cosmos-tx-proto2json/actions/workflows/test.yml)
[![conventional commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-yellow.svg?style=for-the-badge&logo=conventionalcommits)](https://conventionalcommits.org)
[![contributor covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg?style=for-the-badge)](https://github.com/okp4/.github/blob/main/CODE_OF_CONDUCT.md)
[![License](https://img.shields.io/badge/License-BSD_3--Clause-blue.svg?style=for-the-badge)](https://opensource.org/licenses/BSD-3-Clause)
[![Quarkus](https://img.shields.io/badge/Quarkus-1A2C34?logo=quarkus&logoColor=4695EB&style=for-the-badge)](https://quarkus.io)

## Purpose

A Kafka Streams Processor that consumes [CØSMOS](https://github.com/cosmos/cosmos-sdk) and [ØKP4](https://github.com/okp4/okp4d)
[Protobuf](https://developers.google.com/protocol-buffers) messages from an `input` Kafka topic and sends a [JSON](https://www.json.org/json-en.html)
decoded message in the `output` topic.

<p align="center">
  <img src="./docs/overview.png">
</p>

## Implementation

Implementation mainly relies on [Kafka Streams API](https://kafka.apache.org/documentation/streams), library to create
event-stream applications with the following features:

- no external dependency other than Kafka itself,
- simple and light library,
- fault-tolerant and scalable.

Moreover, this implementation:

- uses [Kotkin](https://kotlinlang.org/) as primary coding language,
- uses [Quarkus](https://quarkus.io/) to minimize resources consumption,
- is as much as possible, lean, i.e. tries to minimize the dependencies to 3rd party libraries and the resulting package
  footprint.

## Build

This project targets the [JVM 11+](https://openjdk.java.net/), so be sure to have it available in your environment.

This project relies on the [Gradle](https://gradle.org/) build system.

If you are on Windows then open a command line, go into the root directory and run:

```sh
.\gradlew build
```

If you are on linux/mac then open a terminal, go into the root directory and run:

```sh
./gradlew build
```

This command line produces a _native_ executable: `kafka-processor-cosmos-tx-proto2json-X.Y-runner`

## You want to get involved? 😍

Please check out OKP4 health files :

- [Contributing](https://github.com/okp4/.github/blob/main/CONTRIBUTING.md)
- [Code of conduct](https://github.com/okp4/.github/blob/main/CODE_OF_CONDUCT.md)
