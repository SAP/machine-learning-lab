"""
Build ML Lab React Webapp
"""

import os
import subprocess
import argparse

# Wrapper to print out command
def call(command):
    print("Executing: "+command)
    return subprocess.call(command, shell=True)

call("npm run setup")
