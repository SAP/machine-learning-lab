import subprocess
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('--version', help='version of build (MAJOR.MINOR.PATCH-TAG)')
parser.add_argument('--deploy', help="deploy the documentation", action='store_true')

args, unknown = parser.parse_known_args()
if unknown:
    print("Unknown arguments "+str(unknown))

# Wrapper to print out command
def call(command):
    print("Executing: "+command)
    return subprocess.call(command, shell=True)

call("pip install --upgrade mkdocs")
call("pip install mkdocs-material==4.6.3")
call("pip install --upgrade pygments")
call("pip install --upgrade pymdown-extensions")

call("mkdocs build")

if args.deploy:
    call("mkdocs gh-deploy --clean")