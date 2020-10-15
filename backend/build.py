import os, sys, re
import subprocess
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('--maven', help="only maven build", action='store_true')
parser.add_argument('--docker', help="only docker build", action='store_true')
parser.add_argument('--version', help='version of build (MAJOR.MINOR.PATCH-TAG)')
parser.add_argument('--notests', help="deactivate integration tests", action='store_true')
parser.add_argument('--deploy', help="deploy docker container to remote", action='store_true')

args, unknown = parser.parse_known_args()
if unknown:
    print("Unknown arguments "+str(unknown))

# Wrapper to print out command
def call(command):
    print("Executing: "+command)
    return subprocess.call(command, shell=True)

# calls build scripts in every module with same flags
def build(module):
    build_command = "python -u build.py"
    # if args.maven:
    #     build_command += " --maven"

    # if args.docker:
    #     build_command += " --docker"

    # if args.version:
    #     build_command += " --version="+str(args.version)

    # if args.deploy:
    #     build_command += " --deploy"

    # if args.notests:
    #     build_command += " --notests"
    for arg in vars(args):
        arg_value = getattr(args, arg)
        if arg_value:
            # For boolean types, the existence of the flag is enough
            if type(arg_value) == bool:
                build_command += f" --{arg}"
            else:
                build_command += f" --{arg}={arg_value}"

    working_dir = os.path.dirname(os.path.realpath(__file__))
    full_command = "cd '"+module+"' && "+build_command+" && cd '"+working_dir+"'"
    print("Building "+module+" with: "+full_command)
    failed = call(full_command)
    if failed:
        print("Failed to build module "+module)
        sys.exit()

# TODO: refactor and replace with our version-detection logic!
if not args.version:
    args.version = "1.0.0-dev"

# Version Handling
if args.deploy and not args.version:
    print("Please provide a version for deployment (--version=MAJOR.MINOR.PATCH-TAG)")
    sys.exit()
elif args.deploy:
    # for deployment, use version as it is provided
    args.version = str(args.version)
elif not args.version:
    # for not deployment builds, no version provided, make sure it is a SNAPSHOT build
    # read project build version from local pom.xml
    with open('pom.xml', 'r') as pom_file:
        data=pom_file.read().replace('\n', '')
        current_version = re.search('<version>(.+?)</version>', data).group(1)

    if current_version:
        current_version = current_version.strip()
        if "SNAPSHOT" not in current_version:
            args.version =  current_version+"-SNAPSHOT"
        else:
            args.version = str(current_version)
    else:
        print("Failed to detect the current version")
else:
    args.version = str(args.version)
    if "SNAPSHOT" not in args.version:
        # for not deployment builds, add snapshot tag
        args.version += "-SNAPSHOT"

# update version in all maven submodules
if args.version:
    failed = call("mvn versions:set -DnewVersion="+args.version)
    if failed:
        print("Failed to apply version "+args.version)
        call("mvn versions:revert")
        sys.exit()
    call("mvn versions:commit")


if args.maven or (not args.maven and not args.docker):
    # Check if all project can be build, otherwise exit build script
    failed = call("mvn clean package")
    if failed:
        print("Failed to build project")
        sys.exit()

    # Do not deploy maven artifacts:
    # call("mvn -N clean deploy")
    
    call("mvn -N clean install")
else:
    print("Only docker build is selected. Nothing to build here.")

# call build.py of every sub-project with same flags

# libraries
build("environment-lib")
build("service-lib")

# services
build("lab-service")

