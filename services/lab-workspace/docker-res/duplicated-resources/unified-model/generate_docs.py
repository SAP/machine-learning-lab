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

call("pydocmd simple unified_model.UnifiedModel+ "
     "unified_model.model_types++ "
     "unified_model.cli_handler++ "
     "unified_model.model_handler+ "
     "unified_model.compatibility_utils++ "
     "unified_model.evaluation_utils+ "
     "unified_model.ensemble_utils.VotingEnsemble+> docs/docs.md")
