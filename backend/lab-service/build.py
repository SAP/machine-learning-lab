from universal_build import build_utils

COMPONENT_NAME = "lab-service"

args = build_utils.get_sanitized_arguments()

if args[build_utils.FLAG_MAKE]:
    completed_process = build_utils.run("mvn package")
    if completed_process.returncode > 0:
        build_utils.log("Failed to compile project")
        build_utils.exit_process(1)

    version_build_arg = " --build-arg service_version=" + args[build_utils.FLAG_VERSION]
    completed_process = build_utils.build_docker_image(
        COMPONENT_NAME, args[build_utils.FLAG_VERSION], version_build_arg
    )
    if completed_process.returncode > 0:
        build_utils.exit_process(completed_process.returncode)

if args[build_utils.FLAG_TEST]:
    completed_process = build_utils.run("mvn verify")
    if completed_process.returncode > 0:
        build_utils.log(f"Tests failed for component {COMPONENT_NAME}")
        build_utils.exit_process(1)

# Only allow releasing from sub componenents when force flag is set as an extra precaution step
if args[build_utils.FLAG_RELEASE] and args[build_utils.FLAG_FORCE]:
    build_utils.release_docker_image(COMPONENT_NAME, args[build_utils.FLAG_VERSION], args[build_utils.FLAG_DOCKER_IMAGE_PREFIX])
