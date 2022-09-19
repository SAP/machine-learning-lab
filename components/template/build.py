import os
from argparse import ArgumentParser

from universal_build import build_utils
from universal_build.helpers import build_docker, build_python, openapi_utils

HERE = os.path.abspath(os.path.dirname(__file__))

WEBAPP_COMPONENT = "webapp"
PYTHON_LIB_COMPONENT = "backend"
COMPONENT_NAME = "insert-component-name-here"


def main(args: dict) -> None:
    """Execute all component builds."""

    # set script path as working dir
    os.chdir(HERE)

    version = args.get(build_utils.FLAG_VERSION)

    # Build python lib
    build_utils.build(PYTHON_LIB_COMPONENT, args)

    if args.get(build_utils.FLAG_MAKE):
        build_utils.build(WEBAPP_COMPONENT, args)

        build_docker.build_docker_image(COMPONENT_NAME, version, exit_on_error=True)

    # TODO: Uncomment when dockerfile is finalized
    # if args.get(build_utils.FLAG_CHECK):
    # build_docker.lint_dockerfile(exit_on_error=True)

    # Only allow releasing from sub components when force flag is set as an extra precaution step
    if args.get(build_utils.FLAG_RELEASE) and args.get(build_utils.FLAG_FORCE):
        build_docker.release_docker_image(
            COMPONENT_NAME,
            args[build_utils.FLAG_VERSION],
            "ghcr.io/sap/machine-learning-lab",
        )


if __name__ == "__main__":
    parser = ArgumentParser()
    parser.add_argument(
        f"--contaxy-version", help="Version of the contaxy library to use."
    )
    args = build_utils.parse_arguments(argument_parser=parser)
    main(args)
