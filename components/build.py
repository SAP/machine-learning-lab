import os
from pathlib import Path

from universal_build import build_utils

HERE = os.path.abspath(os.path.dirname(__file__))
ml_lab_components = [f.name for f in Path('.').iterdir() if f.is_dir()]

args = build_utils.parse_arguments()

build_utils.log("Building all ML Lab components")
for component in ml_lab_components:
    build_utils.build(component, args)
