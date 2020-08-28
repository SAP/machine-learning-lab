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

call("pydocmd simple "
     "lab_client.environment.Environment+ "
     "lab_client.handler.experiment_handler.Experiment+ "
     "lab_client.handler.lab_file_handler.FileHandler+ "
     "lab_client.handler.lab_job_handler.LabJobHandler+> docs/docs.md")


