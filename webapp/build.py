"""
Build ML Lab React Webapp
"""

from universal_build import build_utils

COMPONENT_NAME = "ml-lab-webapp"

args = build_utils.get_sanitized_arguments()

if args[build_utils.FLAG_MAKE]:
    completed_process = build_utils.run("npm --allow-same-version --no-git-tag-version version " + args[build_utils.FLAG_VERSION], exit_on_error=True)
    completed_process = build_utils.run("npm run setup", exit_on_error=True)
