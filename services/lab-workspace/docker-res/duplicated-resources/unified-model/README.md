# Unified Model
A lightweight library to create self-contained executable models with focus on compatibility, simplicity, and fast experimentation.

**Key Aspects:**
* Build once, run anywhere.
* Works with any machine learning library that comes with a python interface.
* Packages all your model logic and artifacts into a single self-container file.

At its most basic, a trained model is just a function that takes some input and produces some output. This library builds on this assumption and enables you to package up the preprocessing & prediction logic, and all of your model artifacts into a single model archive file that you can then easily share, distribute, and deploy. There is no need for configuration files, copying files on the file system, or other manual tasks. You can define and save your model without leaving your python notebook or script. With the unified model format, you can build your model once and run it anywhere. It provides a huge flexibility to deploy and serve your models in any environment. Furthermore, this model format makes it possible to easily combine (e.g. via voting ensembles) and evaluate models without having to know the underlying machine learning library.

## Get Started
- [API Documentation](docs/docs.md)
- [Tutorial Notebook](docs/unified-model-tutorial.ipynb)

## Requirements

* Python 3.5+

## Installation

Install via pip:

```bash
pip install --upgrade TODO
```

or directly from the source code:

```bash
git clone TODO
cd unified-model
python setup.py install
```

## Usage

After installation, the package can be imported:

```python
from unified_model import UnifiedModel

# This basic model just returns the data that it gets in.
class MyEchoModel(UnifiedModel):
    def _predict(self, data, **kwargs):
        # Implement this function with the logic to make a prediction on the given data item
        # In this example we just return the text itself.
        return data

# Initialize model
echo_model = MyEchoModel()

# Predict with model
print(echo_model.predict("This is an data example"))

# Save model to file
model_path = echo_model.save('my_first_model.pyz')

# Load model from file
loaded_model = UnifiedModel.load(model_path)
```

## Develop

### Requirements

- Python 3, Maven, Docker

### Build

Execute this command in the project root folder to build this project and the respective docker container:

```bash
python build.py
```

### Deploy

Execute this command in the project root folder to deploy all assembled artifacts to the configured maven repository and push all docker containers to the configured docker registry:

```bash
python build.py --deploy --version={MAJOR.MINOR.PATCH-TAG}
```

For deployment, the version has to be provided. The version format should follow the [Semantic Versioning](https://semver.org/) standard (MAJOR.MINOR.PATCH). For additional script options:

```bash
python build.py --help
```

### Dev Guidelines

#### Git Workflow

Our git branching for all repositories is based on the [Git-Flow standard](https://datasift.github.io/gitflow/IntroducingGitFlow.html). Please go trough the [linked introduction](https://datasift.github.io/gitflow/IntroducingGitFlow.html), and visit [here](http://nvie.com/posts/a-successful-git-branching-model) and [here](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow) for more information.

#### Build Versioning

Our build versioning for all projects is based on the [Semantic Versioning specification](https://semver.org/). For any versioning-related questions, please refer to the [linked guide](https://semver.org/). In additon to the MAJOR.MINOR.PATCH format, a `SNAPSHOT` tag will be attached to the version for all development builds (based on the [Maven versioning standard](https://docs.oracle.com/middleware/1212/core/MAVEN/maven_version.htm)). As define in  Git-Flow, the build version is also used as tag for releases on the master branch.
