from universal_build import build_utils

COMPONENT_NAME = "unified-model-lib"

args = build_utils.parse_arguments()

if args[build_utils.FLAG_MAKE]:
    completed_process = build_utils.run("python setup.py develop")
    if completed_process.returncode > 0:
        build_utils.log(f"Error in building component {COMPONENT_NAME}")

    completed_process = build_utils.run("python generate_docs.py")
    if completed_process.returncode > 0:
        build_utils.log(f"Error in generating docs for component {COMPONENT_NAME}")

    build_utils.build("docker", args)
