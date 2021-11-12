# Python Client Library for ML Lab

This library is a high-level abstraction of Contaxy's client library.

## How to install

```shell
git clone https://github.wdf.sap.corp/ml-foundation/ml-lab-ctxy-py
cd ml-lab-ctxy-py
CTXY_PACKAGE_PATH="/path/to/contaxy/backend" pip install -e .
```

## Usage

After installation, the package can be imported:

```python
import lab_client
```

## Current progress:

- [x] upload_file
- [x] get_file
- [x] print_info
- [ ] cleanup
- [ ] upload_folder

Experiment related functions:
- [ ] create_file_path
- [ ] create_experiment
