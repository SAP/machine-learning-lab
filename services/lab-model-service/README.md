# Lab Model Service

Load any unified model and serve it via a REST API with Lab Support

## Usage

To start the lab-model-service in a single host deployment, execute (replace `MY_MODEL` with an model key to a unfied model file on the Lab remote storage):

```bash
docker run -d -p 8091:8091 --env MODEL_KEY:MY_MODEL --restart always lab-model-service:latest
```

### Configuration

#### Parameters

The container can be configured with following environment variables (`--env`):

<table>
    <tr>
        <th>Variable</th>
        <th>Description</th>
        <th>Default</th>
    </tr>
    <tr>
        <td>INSTALL_REQUIREMENTS</td>
        <td>Set to true to automatically install model requirements.</td>
        <td>True</td>
    </tr>
    <tr>
        <td>MODEL_KEY</td>
        <td>Key of default model (e.g. remote file key or file path).</td>
        <td>/default_model</td>
    </tr>
    <tr>
        <td colspan="3">Lab Configuration (will be automatically attached when started in Lab):</td>
    </tr>
    <tr>
        <td>LAB_ENDPOINT</td>
        <td>Endpoint URL of an ML Lab instance.</td>
        <td>(required)</td>
    </tr>
    <tr>
        <td>LAB_PROJECT</td>
        <td>Specified project of an ML Lab Instance.</td>
        <td>(required)</td>
    </tr>
    <tr>
        <td>LAB_API_TOKEN</td>
        <td>API Token to access the REST API of an ML Lab instance.</td>
        <td>(optional)</td>
    </tr>
</table>

#### Proxy

If a proxy is required, you can pass it via the `http_proxy`and `no_proxy` environment varibales.

#### Docker Configuration

You can find more ways of configuration about [docker run](https://docs.docker.com/engine/reference/commandline/run) and [docker service create](https://docs.docker.com/engine/reference/commandline/service_create) in the official Docker documentation.

## Develop

### Requirements

- Java 8, Python 3, Maven, Docker

### Build

Execute this command in the project root folder to build the docker container:

```bash
python build.py --make --version {MAJOR.MINOR.PATCH-TAG}
```

The version has to be provided. The version format should follow the [Semantic Versioning](https://semver.org/) standard (MAJOR.MINOR.PATCH). For additional script options:

```bash
python build.py --help
```

### Deploy

Execute this command in the project root folder to push the container to the configured docker registry:

```bash
python build.py --release --version {MAJOR.MINOR.PATCH-TAG} --docker-image-prefix {PREFIX}
```

The version has to be provided. The version format should follow the [Semantic Versioning](https://semver.org/) standard (MAJOR.MINOR.PATCH). For additional script options:

```bash
python build.py --help
```
