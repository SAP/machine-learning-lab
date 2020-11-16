import subprocess
import argparse
import datetime

from universal_build import build_utils

parser = argparse.ArgumentParser(add_help=False)
parser.add_argument(
    "--flavor", help="flavor (lab, lab-gpu) used for docker container", default="lab"
)


COMPONENT_NAME = "ml-workspace"
FLAG_FLAVOR = "flavor"

args = build_utils.get_sanitized_arguments(argument_parser=parser)

if not args[FLAG_FLAVOR]:
    args[FLAG_FLAVOR] = "lab"

args[FLAG_FLAVOR] = str(args[FLAG_FLAVOR]).lower()

if args[FLAG_FLAVOR] == "all":
    args[FLAG_FLAVOR] = "lab"
    build_utils.build(".", args)
    args[FLAG_FLAVOR] = "lab-gpu"
    build_utils.build(".", args)
    build_utils.exit_process(0)

# unknown flavor -> try to build from subdirectory
if args[FLAG_FLAVOR] not in ["lab", "lab-gpu"]:
    # assume that flavor has its own directory with build.py
    build_utils.build(args[FLAG_FLAVOR], args)
    build_utils.exit_process(0)

# Add flavor suffix to image name
# service_name += "-" + args.flavor
COMPONENT_NAME = f"{COMPONENT_NAME}-{args[FLAG_FLAVOR]}"

# Set base workspace image
base_image = "mltooling/ml-workspace-r:0.9.1"
if args[FLAG_FLAVOR] == "lab-gpu":
    base_image = "mltooling/ml-workspace-gpu:0.9.1"

# docker build
git_rev = "unknown"
try:
    git_rev = (
        subprocess.check_output(["git", "rev-parse", "--short", "HEAD"])
        .decode("ascii")
        .strip()
    )
except Exception:
    pass

build_date = datetime.datetime.utcnow().isoformat("T") + "Z"
try:
    build_date = (
        subprocess.check_output(["date", "-u", "+%Y-%m-%dT%H:%M:%SZ"])
        .decode("ascii")
        .strip()
    )
except Exception:
    pass

vcs_ref_build_arg = " --build-arg ARG_VCS_REF=" + str(git_rev)
build_date_build_arg = " --build-arg ARG_BUILD_DATE=" + str(build_date)
base_image_build_arg = " --build-arg ARG_WORKSPACE_BASE_IMAGE=" + str(base_image)
flavor_build_arg = " --build-arg ARG_WORKSPACE_FLAVOR=" + str(args[build_utils.FLAG_FLAVOR])
version_build_arg = " --build-arg ARG_WORKSPACE_VERSION=" + str(args[build_utils.FLAG_VERSION])

if args[build_utils.FLAG_MAKE]:
    build_args = (
        version_build_arg
        + " "
        + flavor_build_arg
        + " "
        + base_image_build_arg
        + " "
        + vcs_ref_build_arg
        + " "
        + build_date_build_arg
    )
    completed_process = build_utils.build_docker_image(
        COMPONENT_NAME, version=args[build_utils.FLAG_VERSION], build_args=build_args
    )
    if completed_process.returncode > 0:
        build_utils.exit_process(1)

if args[build_utils.FLAG_RELEASE]:
    build_utils.release_docker_image(COMPONENT_NAME, args[build_utils.FLAG_VERSION], args[build_utils.FLAG_DOCKER_IMAGE_PREFIX])
