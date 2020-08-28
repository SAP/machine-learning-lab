import os, sys, re
import subprocess
import argparse
import re

parser = argparse.ArgumentParser()
parser.add_argument('--port', help='exposed port of the service', default=8090, type=int)
parser.add_argument('--endpoint', help='Absolute URL of ML Lab in the form of "protocol://host:port". \
    Default uses the URL where the site is hosted by just setting url-path.', default="", type=str)
args, unknown = parser.parse_known_args()

if unknown:
    print("Unknown arguments "+str(unknown))

if not args.endpoint:
    args.endpoint = ""

FILE_PATH = './package.json'

with open(FILE_PATH, 'r') as f:
    original_file = f.read()
    replaced_config = re.sub("REACT_APP_LAB_ENDPOINT=[^\s]*", "REACT_APP_LAB_ENDPOINT=" + args.endpoint + "/api", original_file)

with open(FILE_PATH, 'w') as f:
    f.write(replaced_config)

# Wrapper to print out command
def call(command):
    print("Executing: " + command)
    return subprocess.call(command, shell=True)

print("Access ML Lab on localhost: " + str(args.port) + "/app.")
call("npm run start")
