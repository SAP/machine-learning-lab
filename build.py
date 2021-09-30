"""Root build.py for the ML Lab project. Will also execute the build.py in sub-directories."""

import os

from universal_build import build_utils
from universal_build.helpers import build_docker, build_python, openapi_utils

HERE = os.path.abspath(os.path.dirname(__file__))

LAB_COMPONENTS = "components"
WEBAPP_COMPONENT = "webapp"

PROJECT_NAME = "py-machine-learning-lab"


def main(args: dict) -> None:
    """Execute all component builds."""

    # set script path as working dir
    os.chdir(HERE)

    version = args.get(build_utils.FLAG_VERSION)

    # Build all ML Lab components
    build_utils.build(LAB_COMPONENTS, args)

    # Build the webapp
    build_utils.build(WEBAPP_COMPONENT, args)

    # Build ML Lab docker image
    if args.get(build_utils.FLAG_MAKE):
        build_docker.build_docker_image(PROJECT_NAME, version, exit_on_error=True)

    # TODO: Uncomment when dockerfile is finalized
    # if args.get(build_utils.FLAG_CHECK):
    # build_docker.lint_dockerfile(exit_on_error=True)


if __name__ == "__main__":
    args = build_utils.parse_arguments()

    if args.get(build_utils.FLAG_RELEASE):
        # Run main without release to see whether everything can be built and all tests run through
        args = dict(args)
        args[build_utils.FLAG_RELEASE] = False
        main(args)
        # Run main again without building and testing the components again
        args = {
            **args,
            build_utils.FLAG_MAKE: False,
            build_utils.FLAG_CHECK: False,
            build_utils.FLAG_TEST: False,
            build_utils.FLAG_RELEASE: True,
            build_utils.FLAG_FORCE: True,
        }
    main(args)
