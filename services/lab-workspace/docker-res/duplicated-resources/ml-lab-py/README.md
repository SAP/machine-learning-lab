# Python Client Library for ML Lab

- [Lab Client Documentation](docs/docs.md)
- [Lab API Documentation](lab_api)
- [Tutorial Notebook](#TODO)

## Requirements

* Python 2.7+ or 3.5+

## Installation

Install via pip:

```bash
pip install --upgrade git+TODO
```

or directly from the source code:

```bash
git clone TODO
cd ml-lab-py
python setup.py install
```

## Usage

After installation, the package can be imported:

```python
import lab_client
```

## Swagger Client

Lab API client is generated via [Swagger Editor](https://editor.swagger.io). Following changes have been made:
- Changed all path from `swagger_client` to `lab_api.swagger_client`
- In api_client: `swagger_client` and `import and getattr(swagger_client.models, klass)`