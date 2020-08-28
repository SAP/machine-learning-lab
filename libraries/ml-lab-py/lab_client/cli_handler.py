from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import logging
import sys

import click

log = logging.getLogger(__name__)


@click.group()
@click.version_option()
def cli():
    # log to sys out
    logging.basicConfig(stream=sys.stdout, format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)


@click.command("cleanup")
@click.option("--project", "-p", required=False, type=click.STRING, help="Selected project")
def cleanup(project):
    """ Cleanup environment folder to reduce disk space usage.
    Removes all files with more than 50 MB that haven't been used for the last 3 days. """
    try:
        from lab_client import Environment
        Environment(project=project).cleanup()
    except:
        log.exception("Failed to cleanup environment.")


@click.command("get-file")
@click.option("--project", "-p", required=False, type=click.STRING, help="Selected project")
@click.option("--unpack", "-u", required=False, type=click.BOOL, help="If True, unpack the file.")
@click.argument('key')
def get_file(project, unpack, key):
    """ Returns path to the file for the given KEY (either an storage key or url). If the file is not available locally,
    download it from the remote storage. """
    try:
        if not unpack:
            unpack = False
        from lab_client import Environment
        file_path = Environment(project=project).get_file(key, unpack=unpack)
        if file_path:
            log.info("Downloaded file to " + str(file_path))
    except:
        log.exception("Failed to get file with key: " + str(key) + " from project: " + str(project))


@click.command("upload-file")
@click.option("--project", "-p", required=False, type=click.STRING,
              help="Selected project")
@click.option("--type", "-t", required=True, type=click.Choice(['model', 'dataset', 'experiment']),
              help="Data type of the file.")
@click.argument('path', type=click.Path(exists=True))
def upload_file(project, path, type):
    """ Uploads a file from the PATH to the remote storage. """
    try:
        from lab_client import Environment
        Environment(project=project).upload_file(path, type)
    except:
        log.exception("Failed to upload file " + str(path) + " to project: " + str(project))


cli.add_command(get_file)
cli.add_command(upload_file)
cli.add_command(cleanup)

if __name__ == '__main__':
    cli()
