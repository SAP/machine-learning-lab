import os, sys, re
import subprocess
import argparse
import socket
import time
from sys import platform as _platform

parser = argparse.ArgumentParser()
parser.add_argument('--port', help='exposed port of the service', default=8090, type=int)
parser.add_argument('--name', help='name of docker container', default="lab-service")
parser.add_argument('--version', help='version tag of docker container', default="latest")
parser.add_argument('--swarm', help='Start in swarm mode.', action='store_true')
parser.add_argument('--kubernetes', help='Start in kubernetes mode.', action='store_true')
parser.add_argument('--env', action='append', help='environment variable provided to the container, e.g.: ENV_VARIABLE=value')
parser.add_argument('--k8sdataroot', help="set data root in k8s mode. Default is /workspace/data/")

args, unknown = parser.parse_known_args()
if unknown:
    print("Unknown arguments "+str(unknown))

# Wrapper to print out command
def call(command):
    print("Executing: " + command)
    return subprocess.call(command, shell=True)

def callAndRead(command):
    print("Executing: " + command)
    return str(subprocess.check_output(command, shell=True))

# get service name
service_name = args.name
if not service_name:
    # get service name from pom
    with open('pom.xml', 'r') as pom_file:
        data=pom_file.read().replace('\n', '')
        artifact_id = re.search('<artifactId>(.+?)</artifactId>', data).group(1)

    if artifact_id:
        service_name = artifact_id.strip()
    else:
        # get service name by folder name
        service_name = os.path.basename(os.path.dirname(os.path.realpath(__file__)))


# run in docker container
env_vars = ""
if args.env and len(args.env) > 0:
    for env_var in args.env:
        env_vars += " --env "+env_var

data_root = ""
if args.k8sdataroot:
    data_root = " --env LAB_DATA_ROOT=" + args.k8sdataroot

if args.kubernetes:
    call("docker rm -f lab-installation")
    call("docker run --name lab-installation -v /var/run/docker.sock:/var/run/docker.sock -v ~/.kube/config:/root/.kube/config --env LAB_ACTION=install --env LAB_PORT="+str(args.port)+" --env LAB_DEBUG=true --env SERVICES_RUNTIME=k8s " + data_root + env_vars + " " + service_name + ":" + str(args.version))
else:
    call("docker rm -f lab-installation")
    # Uninstall before call("docker run -v /var/run/docker.sock:/var/run/docker.sock  --env LAB_ACTION=uninstall " + service_name + ":" + str(args.version))
    call("docker run --name lab-installation -v /var/run/docker.sock:/var/run/docker.sock --env LAB_ACTION=install --env LAB_PORT="+str(args.port)+" --env LAB_DEBUG=true --env SERVICES_RUNTIME=local " + env_vars + " " + service_name + ":" + str(args.version))

print("Visit http://localhost:" + str(args.port))