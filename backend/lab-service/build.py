
import os, sys, re
import subprocess
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('--maven', help="only maven build", action='store_true')
parser.add_argument('--docker', help="only docker build", action='store_true')
parser.add_argument('--name', help='name of docker container', default='lab-service')
parser.add_argument('--version', help='version of build (MAJOR.MINOR.PATCH-TAG)')
parser.add_argument('--notests', help="deactivate integration tests", action='store_true')
parser.add_argument('--deploy', help="deploy docker container to remote", action='store_true')

REMOTE_IMAGE_PREFIX = "mltooling/"

args, unknown = parser.parse_known_args()
if unknown:
    print("Unknown arguments "+str(unknown))

# Wrapper to print out command
def call(command):
    print("Executing: "+command)
    return subprocess.call(command, shell=True)

# get service name
service_name = args.name
if not service_name:
    # get service name by folder name
    service_name = os.path.basename(os.path.dirname(os.path.realpath(__file__)))

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
    with open('pom.xml', 'r') as pom_file:
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

# maven build
if args.maven or (not args.maven and not args.docker):
    # Compile to generate newest swagger docs
    failed = call("mvn clean compile")

    if failed:
        print("Failed to compile project")
        sys.exit()

    # Package or Deploy project
    failed = 0
    if args.deploy:
        if args.notests:
            failed = call("mvn clean package -Dskiptests=true -DskipITs")
        else:
            failed = call("mvn clean package")
    else:
        failed = call("mvn clean package -Dskiptests=true -DskipITs")

    if failed:
        print("Failed to build project")
        sys.exit()

# docker build
if args.docker or (not args.maven and not args.docker):

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