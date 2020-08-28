import os, sys, re
import subprocess
import argparse


parser = argparse.ArgumentParser()
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
    build_command = "python build.py"

    if args.version:
        build_command += " --version="+str(args.version)

    if args.deploy:
        build_command += " --deploy"

    if args.notests:
        build_command += " --notests"

    working_dir = os.path.dirname(os.path.realpath(__file__))
    full_command = "cd '"+module+"' && "+build_command+" && cd '"+working_dir+"'"
    print("Building "+module+" with: "+full_command)
    failed = call(full_command)
    if failed:
        print("Failed to build module "+module)
        sys.exit()


call("python setup.py develop")
call("python generate_docs.py")

# build docker image
build("docker")


# pip uninstall . && pip install --ignore-installed --no-cache -U -e .