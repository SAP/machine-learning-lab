from universal_build import build_utils
from universal_build.helpers import build_docker

COMPONENT_NAME = "simple-demo-job"

args = build_utils.parse_arguments()
if args[build_utils.FLAG_MAKE]:
    completed_process = build_docker.build_docker_image(
        COMPONENT_NAME, args[build_utils.FLAG_VERSION]
    )
    if completed_process.returncode > 0:
        build_utils.exit_process(completed_process.returncode)

if args[build_utils.FLAG_RELEASE]:
    completed_process = build_docker.release_docker_image(
        COMPONENT_NAME, args[build_utils.FLAG_VERSION], args[build_docker.FLAG_DOCKER_IMAGE_PREFIX]
    )
