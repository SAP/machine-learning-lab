import os, sys, re
import subprocess
import argparse

parser = argparse.ArgumentParser()
# parser.add_argument('--version', help='version of build (MAJOR.MINOR.PATCH-TAG)')
parser.add_argument('--notests', help="deactivate integration tests", action='store_true')
# parser.add_argument('--deploy', help="deploy docker container to remote", action='store_true')

# NEW FLAGS
parser.add_argument('--build', help="Build all artefacts", action='store_true')
parser.add_argument('--test', help="Run unit and integration tests", action='store_true')
parser.add_argument('--deploy', help="Deploy all artefacts to remote registries", action='store_true')
parser.add_argument('--version', help='Version of build (MAJOR.MINOR.PATCH-TAG)')
parser.add_argument('--force', help='Ignore all enforcements and warnings and run the action')

# parser.add_argument('--tests', help='run tests', default=False)

args, unknown = parser.parse_known_args()
if unknown:
    print("Unknown arguments "+str(unknown))

# calls build scripts in every module with same flags
# TODO: refactor build function to remove duplicated code. Somehow it should be possible to just pass all flags as passed to this script here
def build(module):
    build_command = "python -u build.py"

    for arg in vars(args):
        arg_value = getattr(args, arg)
        if arg_value:
            # For boolean types, the existence of the flag is enough
            if type(arg_value) == bool:
                build_command += f" --{arg}"
            else:
                build_command += f" --{arg}={arg_value}"

    # if args.version:
    #     build_command += " --version="+str(args.version)
    
    # if args.notests:
    #     build_command += " --notests"

    # if args.deploy:
    #     build_command += " --deploy"

    

    working_dir = os.path.dirname(os.path.realpath(__file__))
    full_command = "cd '"+module+"' && "+build_command+" && cd '"+working_dir+"'"
    print("Building "+module+" with: "+full_command)
    failed = call(full_command)
    if failed:
        print("Failed to build module "+module)
        sys.exit()

# Wrapper to print out command
def call(command):
    print("Executing: "+command)
    return subprocess.call(command, shell=True)

# get version
if args.deploy and not args.version:
    print("Please provide a version for deployment (--version=MAJOR.MINOR.PATCH-TAG)")
    sys.exit()
elif args.deploy:
    # for deployment, use version as it is provided
    args.version = str(args.version)
elif not args.version:
    # for not deployment builds, no version provided, make sure it is a SNAPSHOT build
    # read project build version from backend pom.xml
    with open('backend/pom.xml', 'r') as pom_file:
        data=pom_file.read().replace('\n', '')
        current_version = re.search('<version>(.+?)</version>', data).group(1)

    if current_version:
        current_version = current_version.strip()
        if "SNAPSHOT" not in current_version:
            args.version =  current_version+"-SNAPSHOT"
        else:
            # do not change the version, since it already has the right version
            change_version = False
            args.version = str(current_version)
    else:
        print("Failed to detect the current version")
else:
    args.version = str(args.version)
    if "SNAPSHOT" not in args.version:
        # for not deployment builds, add snapshot tag
        args.version += "-SNAPSHOT"

# Move libraries to 
call("rm -r -f services/lab-workspace/docker-res/duplicated-resources/")
call("mkdir services/lab-workspace/docker-res/duplicated-resources/")
call("cp -R libraries/* services/lab-workspace/docker-res/duplicated-resources/")

# build base images

# For just testing, the lab-workspace does not have to be built as it is not covered by tests yet
if args.test == False:
    build("services/lab-workspace")
    build("services/simple-workspace-service")

build("services/lab-model-service")

# build demo services/jobs
build("services/simple-demo-job")
build("services/simple-demo-service")
build("services/simple-fastapi-service")

# build webapp and move build into backend service
build("webapp")
# Move webapp build into resources
call("rm -r -f backend/lab-service/src/main/resources/app/")
call("mkdir backend/lab-service/src/main/resources/app/")
call("cp -R webapp/build/* backend/lab-service/src/main/resources/app/")

# build documentation
build("docs")
# Move documentation build into resources
call("rm -r -f backend/lab-service/src/main/resources/docs/")
call("mkdir backend/lab-service/src/main/resources/docs/")
call("cp -R docs/site/* backend/lab-service/src/main/resources/docs/")

# build main application
build("backend")


