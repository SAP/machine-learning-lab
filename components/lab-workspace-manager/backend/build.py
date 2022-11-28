import os
import re
from argparse import ArgumentParser

from universal_build import build_utils
from universal_build.helpers import build_python

# Project specific configuration
MAIN_PACKAGE = "lab_workspace_manager"
GITHUB_URL = "https://github.com/ml-tooling/contaxy"

HERE = os.path.abspath(os.path.dirname(__file__))

INTEGRATION_TEST_MARKER = "integration"


def update_contaxy_version(file_path, contaxy_version):
    with open(file_path, "r+") as f:
        data = f.read()
        f.seek(0)
        f.write(re.sub(r'\"contaxy==[^\"]*\"', f'"contaxy=={contaxy_version}"', data))
        f.truncate()


def main(args: dict) -> None:
    # set current path as working dir
    os.chdir(HERE)

    version = args.get(build_utils.FLAG_VERSION)
    if version:
        # Update version in _about.py
        build_python.update_version(
            os.path.join(HERE, f"src/{MAIN_PACKAGE}/_about.py"),
            build_utils._Version.get_pip_compatible_string(str(version)),
            exit_on_error=True,
        )

    contaxy_version = args.get("contaxy-version")
    if contaxy_version:
        update_contaxy_version("./setup.py", contaxy_version)

    if args.get(build_utils.FLAG_MAKE):
        # Install pipenv dev requirements
        build_python.install_build_env(exit_on_error=True)

        # Generate the OpenAPI spec so that clients can be generated
        # build_utils.run("pipenv run ctxy-workspace export-openapi-specs ./openapi-spec.json")

        # Build distribution via setuptools
        build_python.build_distribution(exit_on_error=True)

    if args.get(build_utils.FLAG_CHECK):
        build_python.code_checks(exit_on_error=True, safety=False)

    if args.get(build_utils.FLAG_TEST):
        build_utils.run("pipenv run coverage erase", exit_on_error=False)

        test_markers = args.get(build_utils.FLAG_TEST_MARKER)

        # if the test_markers list exists, join those markers via "or". pytest will ignore markers it does not know
        pytest_marker = (
            "unit"
            if (not isinstance(test_markers, list) or test_markers == [])
            else " or ".join(test_markers)
        )
        if isinstance(test_markers, list) and INTEGRATION_TEST_MARKER in test_markers:
            pytest_marker = "integration"
        # Activated Python Environment (3.8)
        # TODO: this is not needed since it is the env installed via make: build_python.install_build_env()
        # Run pytest in pipenv environment
        build_utils.run(
            f"pipenv run pytest tests -m {pytest_marker} --cov=src --cov-append --cov-config=setup.cfg --cov-report=xml --cov-report term --cov-report=html",
            exit_on_error=True,
        )

        # Update pipfile.lock when all tests are successfull (lock environment)
        build_utils.run("pipenv lock", exit_on_error=True)

    if args.get(build_utils.FLAG_RELEASE):
        # Create API documentation via lazydocs
        build_python.generate_api_docs(
            github_url=GITHUB_URL, main_package=MAIN_PACKAGE, exit_on_error=True
        )


if __name__ == "__main__":
    parser = ArgumentParser()
    parser.add_argument(
        f"--contaxy-version", help="Version of the contaxy library to use."
    )
    args = build_python.parse_arguments(argument_parser=parser)
    main(args)
