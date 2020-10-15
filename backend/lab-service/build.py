
import os, sys, re
import subprocess
import argparse


parser = argparse.ArgumentParser()

 
parser.add_argument('--maven', help="only maven build", action='store_true')
parser.add_argument('--docker', help="only docker build", action='store_true')
parser.add_argument('--name', help='name of docker container', default='lab-service')
# parser.add_argument('--version', help='version of build (MAJOR.MINOR.PATCH-TAG)')
parser.add_argument('--notests', help="deactivate integration tests", action='store_true')
# parser.add_argument('--deploy', help="deploy docker container to remote", action='store_true')

# NEW FLAGS
parser.add_argument('--build', help="Build all artefacts", action='store_true')
parser.add_argument('--test', help="Run unit and integration tests", action='store_true')
parser.add_argument('--deploy', help="Deploy all artefacts to remote registries", action='store_true')
parser.add_argument('--version', help='Version of build (MAJOR.MINOR.PATCH-TAG)')
parser.add_argument('--force', help='Ignore all enforcements and warnings and run the action')

REMOTE_IMAGE_PREFIX = "mltooling/"

args, unknown = parser.parse_known_args()
if unknown:
    print("Unknown arguments "+str(unknown))

# Wrapper to print out command
def call(command):
    print("Executing: "+command)
    return subprocess.call(command, shell=True)

# get service name
service_name = args.name #TODO not yet provided as script argument
if not service_name:
    # get service name by folder name
    service_name = os.path.basename(os.path.dirname(os.path.realpath(__file__)))

# get version
if args.deploy and (not args.version or not args.test or not args.build):
    print("Please provide a version for deployment (--version=MAJOR.MINOR.PATCH-TAG)")
    print("Test must be executed before the deployment (--test)")
    print("Build must be executed before the deployment (--build)")
    sys.exit()

if args.build:
    failed = call("mvn package")
    if failed:
        print("Failed to compile project")
        sys.exit()

    version_build_arg = " --build-arg service_version=" + str(args.version)

    # call("docker rm -f "+service_name)
    versioned_image = service_name+":"+str(args.version)
    latest_image = service_name+":latest"
    failed = call("docker build -t "+versioned_image+" -t "+latest_image+" " + version_build_arg + " ./")

    if failed:
        print("Failed to build container")
        sys.exit()

if args.test:
    failed = call("mvn verify")
    if failed:
        print("Tests failed")
        sys.exit()

# TODO: remove args.docker again
if args.deploy or args.docker:
    version_build_arg = " --build-arg service_version=" + str(args.version)

    # call("docker rm -f "+service_name)
    versioned_image = service_name+":"+str(args.version)
    latest_image = service_name+":latest"
    failed = call("docker build -t "+versioned_image+" -t "+latest_image+" " + version_build_arg + " ./")

    if failed:
        print("Failed to build container")
        sys.exit()

    remote_versioned_image = REMOTE_IMAGE_PREFIX + versioned_image
    call("docker tag " + versioned_image + " " + remote_versioned_image)

    remote_latest_image = REMOTE_IMAGE_PREFIX + latest_image
    call("docker tag " + latest_image + " " + remote_latest_image)

    if args.deploy:
        call("docker push " + remote_versioned_image)

        if "SNAPSHOT" not in args.version:
            # do not push SNAPSHOT builds as latest version
            call("docker push " + remote_latest_image)
