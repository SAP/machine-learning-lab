from universal_build import build_utils

COMPONENT_NAME = "simple-demo-job"

args = build_utils.get_sanitized_arguments()
if args[build_utils.FLAG_MAKE]:
    completed_process = build_utils.build_docker_image(
        COMPONENT_NAME, args[build_utils.FLAG_VERSION]
    )
    if completed_process.returncode > 0:
        build_utils.exit_process(completed_process.returncode)

if args[build_utils.FLAG_RELEASE]:
    completed_process = build_utils.release_docker_image(
        COMPONENT_NAME, args[build_utils.FLAG_VERSION], args[build_utils.FLAG_DOCKER_IMAGE_PREFIX]
    )
