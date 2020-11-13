# ML Lab Backend

The machine-learning-lab contains the main backend services of the ML Lab and a collection of libraries that provide a generic way to build production-ready machine learning services:

- **environment-lib**: Basic utilities and functionality to manage the project environment.
- **service-lib**: Basic functionality to easily create REST API services.
- **lab-service**: Main lab backend service.

## Develop

### Requirements

- Java 8, Python 3, Maven, Docker

### Build

Execute this command in the project root folder to build the project and all its subprojects for local development:

```bash
python build.py --make
```

This script compiles the project and all sub-projects, assembles all libraries and services, and builds related docker containers. For additional script options:

```bash
python build.py --help
```

To only compile Java artifacts:

```bash
mvn clean package
```

### Deploy

Execute this command in the project root folder to push all docker containers to the configured docker registry:

```bash
python build.py --release --version {MAJOR.MINOR.PATCH-TAG} --docker-image-prefix {PREFIX}
```

For deployment, the version has to be provided. The version format should follow the [Semantic Versioning](https://semver.org/) standard (MAJOR.MINOR.PATCH). For additional script options:

```bash
python build.py --help
```

### Dev Guidelines

#### Code Style

Our coding guideline for source code in the Java is based on the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).  For any code-style related questions, please refer to the [linked guide](https://google.github.io/styleguide/javaguide.html).

#### Git Workflow

Our git branching for all repositories is based on the [Git-Flow standard](https://datasift.github.io/gitflow/IntroducingGitFlow.html). Please go trough the [linked introduction](https://datasift.github.io/gitflow/IntroducingGitFlow.html), and visit [here](http://nvie.com/posts/a-successful-git-branching-model) and [here](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow) for more information.

#### Build Versioning

Our build versioning for all projects is based on the [Semantic Versioning specification](https://semver.org/). For any versioning-related questions, please refer to the [linked guide](https://semver.org/). In additon to the MAJOR.MINOR.PATCH format, a `SNAPSHOT` tag will be attached to the version for all development builds (based on the [Maven versioning standard](https://docs.oracle.com/middleware/1212/core/MAVEN/maven_version.htm)). As define in  Git-Flow, the build version is also used as tag for releases on the master branch.

## Dev Walkthrough

The code for the main service can be found in the `/lab-service` directory. Since this repo is a maven project with a parent pom and multiple children poms, we recommend to open the `/backend` folder (on the same level as the Readme) in your IDE.

We fundamentally believe that the most important thing a development documentation should provide is to give a good introduction into the concepts of the good, e.g. the entrypoint of the code from where you can start to explore it or some overall concepts such as how authorization is implemented. Everything else should be documented by the code itself.

The starting point of the application is `Launcher.java`.
It will configure the web server ([Grizzly](https://javaee.github.io/grizzly/httpserverframework.html)) and makes the documentation and web app accessible. 

Further, it registers the `authorization.AuthorizationManager.java`, the part of the app that takes care of permission handling (basic auth, auth header & cookie handling, ...) etc. Mainly, the library [Pac4J for JAX-RS](https://github.com/pac4j/jax-rs-pac4j) is used. To understand the authorization flow it is crucial to understand that the Pac4J library works a lot with annotations; see for example the `@Pac4JSecurity` annotation in the endpoint classes. The Pac4J library only takes care of the authorization on Java level; the nginx proxy in the container has its own handling of all non-Java related stuff.

Lab can run either on Docker-local or on Kubernetes. For that, different *Service Managers* are implemented (`services.managers.*.java`).
Depending on the environment variable `$SERVICES_RUNTIME`, either the Docker or the Kubernetes service manager are used to execute actions like creating containers, listing them, deleting them etc. (`ComponentManager.java`). The `KubernetesServiceManager` itself has some differentations between being hosted on a non-managed cluster and a managed cluster. Those can differe with regards to how certain resources, e.g. volumes, are provisioned.

All environment variables ML Lab accepts are listed in `LabConfig.java`.
