# ML Lab Backend

The lab-service contains the main ML Lab backend REST API.

## Requirements

- Java 8, Python 3, Maven

## Build

Execute this command in the project root folder to build this project:

```bash
python build.py --make
```

This script compiles the project and assembles the various JAR artifacts (library, jar-with-dependencies, sources). For additional script options:

```bash
python build.py --help
```

To only compile the Java artifacts and install the library locally (for development):

```bash
mvn clean install
```

## Tests

To run the ML Lab backend tests, execute the following:

```python
pythonb build.py --test
```

> If the project has never been built before append the `--make` flag as well.
