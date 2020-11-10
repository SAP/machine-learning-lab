from universal_build import build_utils

COMPONENT_NAME = "ml-lab-py"
args = build_utils.get_sanitized_arguments()

if args[build_utils.FLAG_MAKE]:
    completed_process = build_utils.run("python setup.py develop")
    if completed_process.returncode > 0:
        build_utils.log(f"Error in building component {COMPONENT_NAME}")
