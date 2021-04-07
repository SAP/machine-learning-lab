"""
Build ML Lab React Webapp
"""

from universal_build import build_utils

COMPONENT_NAME = "ml-lab-webapp"

args = build_utils.parse_arguments()

build_utils.log("Install essentials")
build_utils.run("npm install")

if args[build_utils.FLAG_CHECK]:
    build_utils.log("Run linters:")
    build_utils.run("npm run lint:js", exit_on_error=False)
    build_utils.run("npm run lint:css", exit_on_error=False)
    build_utils.log("No linter problems")

if args[build_utils.FLAG_MAKE]:
    completed_process = build_utils.run(
        "npm --allow-same-version --no-git-tag-version version "
        + args[build_utils.FLAG_VERSION],
        exit_on_error=True,
    )
    completed_process = build_utils.run("npm run setup", exit_on_error=True)

if args[build_utils.FLAG_TEST]:
    build_utils.log("Test the webapp:")
    build_utils.run("npm run test", exit_on_error=True)
