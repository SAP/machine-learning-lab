from __future__ import absolute_import, print_function

import logging
import sys

import click

from unified_model import compatibility_utils
from unified_model import model_handler
from unified_model.server import api_server

MODEL_PATH = click.option("--model-path", "-m", metavar="PATH", required=False, type=click.Path(exists=True),
                          help="Path to the unified model. If not provided, it assumes that it is started from within a model.")

log = logging.getLogger(__name__)


@click.group()
@click.version_option()
def cli():
    # log to sys out
    logging.basicConfig(stream=sys.stdout, format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)


def serve(model_path: str = None, port: int = None, host: str = None):
    """Serve a Unified Model via a REST API server."""
    try:
        model_handler.init(default_model_key=model_path)
    except:
        log.exception("Failed to initialize model handler.")
    api_server.run(port=port, host=host)


@click.command("serve")
@MODEL_PATH
@click.option("--port", "-p", default=5000, required=False, type=click.IntRange(1, 65535),
              help="Server port. [default: 5000]")
@click.option("--host", "-h", default="127.0.0.1", required=False, type=click.STRING,
              help="Server host. [default: localhost]")
def _serve_cli(model_path, port, host):
    serve(model_path, port, host)


def predict(model_path: str = None, input_data=None, input_path: str = None, output_path: str = None, **kwargs):
    """Make a prediction on the given data item."""

    try:
        # TODO add predict batch support?
        # TODO real logging
        model_handler.init(default_model_key=model_path)
        log.info(model_handler.predict(input_data, **kwargs))
    except:
        log.exception("Failed predict with model.")


@click.command("predict",
               context_settings=dict(
                   ignore_unknown_options=True,
                   allow_extra_args=True))
@MODEL_PATH
@click.option("--input-data", "-d", help="Provide the input data as an cli argument.",
              required=False)
@click.option("--input-path", "-i", help="Input file containing the data to predict against.",
              required=False, type=click.Path(exists=True))
@click.option("--output-path", "-o",
              help="Results will be output to this file. If not provided, stdout will be used.",
              type=click.Path(exists=False), required=False)
@click.pass_context
def _predict_cli(ctx, model_path, input_data, input_path, output_path):
    """Make a prediction on the given data item."""

    # Allow additional arguments to be used in predict
    kwargs = {}
    for item in ctx.args:
        item = str(item)
        if item.startswith("--") and "=" in item:
            arg_split = item.replace("--", "").split("=")
            kwargs[arg_split[0].strip()] = arg_split[1].strip()

    predict(model_path, input_data, input_path, output_path, **kwargs)


def convert(model_path: str, model_format: str, output_path: str):
    """Convert a Unified Model into another format."""
    try:
        model_handler.init(default_model_key=model_path)

        if model_format == 'pex':
            compatibility_utils.convert_to_pex(model_handler.get_model(), output_path)
        elif model_format == "mlflow":
            compatibility_utils.convert_to_mlflow(model_handler.get_model(), output_path)
        elif model_format == 'pipelineai':
            compatibility_utils.convert_to_pipelineai(model_handler.get_model(), output_path)
    except:
        log.exception("Failed to convert model.")


@click.command("convert")
@MODEL_PATH
@click.option("--model-format", "-f", required=True, type=click.Choice(['pex', 'mlflow', 'pipelineai']),
              help="The format to convert the model to.")
@click.option("--output-path", "-o",
              help="Output path were the converted model is saved to.",
              type=click.Path(exists=False), required=True)
def _convert_cli(model_path, model_format, output_path):
    convert(model_path, model_format, output_path)


cli.add_command(_convert_cli)
cli.add_command(_predict_cli)
cli.add_command(_serve_cli)

if __name__ == '__main__':
    cli()
