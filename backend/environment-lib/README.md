# Environment Lib

The environment-lib contains basic utilities and functionality to manage the project environment. It is integrated into all libraries and services.

## Requirements

- Java 8, Python 3, Maven

## Build

Execute this command in the project root folder to build this project:

```bash
python build.py
```

This script compiles the project and assembles the various JAR artifacts (library, jar-with-dependencies, sources). For additional script options:

```bash
python build.py --help
```

To only compile the Java artifacts and install the library locally (for development):

```bash
mvn clean install
```

## Deploy

Please refer to the [README of the core-platform](../README.md) for information on how to deploy all libraries. **Only deploy this library on its own in special occasions**: 

```bash
python build.py --deploy
```