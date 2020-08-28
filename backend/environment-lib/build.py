import os
import subprocess
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('--maven', help="only maven build", action='store_true')
parser.add_argument('--docker', help="only docker build", action='store_true')
parser.add_argument('--deploy', help="deploy docker container to remote", action='store_true')

args, unknown = parser.parse_known_args()
if unknown:
    print("Unknown arguments "+str(unknown))

module_name = os.path.basename(os.path.dirname(os.path.realpath(__file__)))

# Wrapper to print out command
def call(command):
    print("Executing: "+command)
    return subprocess.call(command, shell=True)

# maven build
if args.maven or (not args.maven and not args.docker):
    call("mvn clean install")
else:
    print("No docker build available for this module ("+module_name+")")