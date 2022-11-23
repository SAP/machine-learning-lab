#!/usr/bin/env python

import os
import re
from glob import glob
from os.path import basename, splitext

from setuptools import find_packages, setup  # type: ignore

NAME = "insert-component-name-here"
MAIN_PACKAGE = NAME.replace("-", "_")  # Change if main package != NAME
DESCRIPTION = "Python package template."
URL = "https://github.com/SAP/machine-learning-lab"
EMAIL = ""
AUTHOR = "SAP ML Lab Team"
LICENSE = "MIT"
REQUIRES_PYTHON = ">=3.8"
VERSION = None  # Only set version if you like to overwrite the version in _about.py

PWD = os.path.abspath(os.path.dirname(__file__))

# Import the README and use it as the long-description.
try:
    with open(os.path.join(PWD, "README.md"), encoding="utf-8") as f:
        long_description = f.read()
except FileNotFoundError:
    long_description = ""

# Extract the version from the _about.py module.
if not VERSION:
    try:
        with open(os.path.join(PWD, "src", MAIN_PACKAGE, "_about.py")) as f:  # type: ignore
            VERSION = re.findall(r"__version__\s*=\s*\"(.+)\"", f.read())[0]
    except FileNotFoundError:
        VERSION = "0.0.0"

# Where the magic happens:
setup(
    name=NAME,
    version=VERSION,
    description=DESCRIPTION,
    long_description=long_description,
    long_description_content_type="text/markdown",
    author=AUTHOR,
    author_email=EMAIL,
    python_requires=REQUIRES_PYTHON,
    url=URL,
    license=LICENSE,
    packages=find_packages(where="src", exclude=("tests", "test", "examples", "docs")),
    package_dir={"": "src"} if os.path.exists("src") else {},
    py_modules=[splitext(basename(path))[0] for path in glob("src/*.py")],
    zip_safe=False,
    install_requires=[
        "fastapi==0.75.2",
        "loguru",
        "contaxy==0.0.20",
    ],
    # deprecated: dependency_links=dependency_links,
    extras_require={
        # Add all extras (e.g. for build and test) here:
        # extras can be installed via: pip install package[dev]
        "dev": [
            "setuptools",
            "wheel",
            "twine",
            "flake8",
            "pytest",
            "pytest-mock",
            "pytest-cov",
            "mypy",
            "black",
            "pydocstyle",
            "isort",
            "lazydocs",
            "locust",
            # Test profiling
            "pyinstrument",
            # Export profiling information about the tests
            "pytest-profiling",
            # Create fake data for testing
            "faker",
            # For better print debugging via debug
            "devtools[pygments]",
            # For Jupyter Kernel support
            "ipykernel",
            # TODO: Move to required when necessary
            "universal-build",
            "requests",
            "uvicorn",
        ],
    },
    setup_requires=['wheel'],
    include_package_data=True,
    classifiers=[
        # TODO: Update based on https://pypi.org/classifiers/
        "Development Status :: 4 - Beta",
        "Intended Audience :: Developers",
        "Intended Audience :: Science/Research",
        "Intended Audience :: Information Technology",
        "License :: OSI Approved :: MIT License",
        "Natural Language :: English",
        "Operating System :: OS Independent",
        "Programming Language :: Python :: 3 :: Only",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.6",
        "Programming Language :: Python :: 3.7",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: Implementation :: PyPy",
        "Programming Language :: Python :: Implementation :: CPython",
        "Topic :: Software Development",
        "Topic :: Software Development :: Libraries",
        "Topic :: Software Development :: Libraries :: Python Modules",
        "Topic :: Scientific/Engineering :: Artificial Intelligence",
        "Topic :: Scientific/Engineering",
        "Topic :: Utilities",
    ],
    project_urls={
        "Changelog": URL + "/releases",
        "Issue Tracker": URL + "/issues",
        "Documentation": URL + "#documentation",
        "Source": URL,
    },
    # entry_points={"console_scripts": [f"{NAME}={MAIN_PACKAGE}._cli:cli"]},
    keywords=[
        # eg: 'keyword1', 'keyword2', 'keyword3',
    ],
)
