#!/usr/bin/python
"""
Container init script
"""

from subprocess import call
import threading

import os
import logging, sys
from unified_model import model_handler
from unified_model.server.api_server import run

from lab_client import Environment

logging.basicConfig(stream=sys.stdout, format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)
log = logging.getLogger(__name__)

log.info("Starting model service...")

env = Environment()
env.print_info()

default_model_key = os.getenv("MODEL_KEY", "/default_model")

def key_resolver(model_key):
    if os.path.exists(model_key):
        # model key == path
        return model_key
    else:
        # resolve with lab 
        # also unpack if possible? , unpack=True
        return env.get_file(model_key)

# set key resolver
model_handler.key_resolver = key_resolver
model_handler.init(default_model_key=default_model_key, install_requirements=True)
run(port=8080, host="0.0.0.0")