import os, sys, re
import subprocess
import argparse


parser = argparse.ArgumentParser()

args, unknown = parser.parse_known_args()
if unknown:
    print("Unknown arguments "+str(unknown))

# Wrapper to print out command
def call(command):
    print("Executing: "+command)
    return subprocess.call(command, shell=True)

call("python setup.py develop")

# pip uninstall . && pip install --ignore-installed --no-cache -U -e .