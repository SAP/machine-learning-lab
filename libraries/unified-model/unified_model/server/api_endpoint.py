import logging
import time
import traceback

import flask

# TODO Temp solution to make swagger support relativ url:
original_rendering = flask.render_template


def render_template(template_name_or_list, **context):
    if template_name_or_list == 'flasgger/index.html':
        context["specs"][0]["url"] = "swagger.json"
    return original_rendering(template_name_or_list, **context)


flask.render_template = render_template


from flask import Flask, jsonify, g, make_response, request, redirect, url_for, send_from_directory, send_file
from flasgger import Swagger
import unified_model
from unified_model import model_handler
from unified_model.server.response_format import get_error_response, get_response, to_json

log = logging.getLogger(__name__)

app = Flask(__name__)

# TODO add monitoring?
# from unified_model.server import api_utils
# api_utils.add_monitoring(app)
# or
# api_utils.add_flask_profiler(app)

# Swagger configuration
app.config['SWAGGER'] = {
    'title': 'Unified Model',
    'uiversion': 3
}

swagger_template = dict(
    info={
        'title': "Unified Model API",
        'version': unified_model.__version__,
        'description': "Unified Model API to invoke model predictions and get model information."
    }
)

swagger_config = {
    "specs": [
        {
            "endpoint": 'swagger',
            "route": '/docs/swagger.json'
        }
    ],
    "swagger_ui": True,
    "specs_route": "/docs/",
    # make static resource path relativ
    'swagger_ui_css': "../flasgger_static/swagger-ui.css",
    'jquery_js': "../flasgger_static/lib/jquery.min.js",
    'swagger_ui_bundle_js': "../flasgger_static/swagger-ui-bundle.js",
    'swagger_ui_standalone_preset_js': "../flasgger_static/swagger-ui-standalone-preset.js",
    'favicon': "../flasgger_static/favicon-32x32.png"
}

swagger = Swagger(app, template=swagger_template, config=Swagger.DEFAULT_CONFIG.update(swagger_config))
start = 0


@app.before_request
def before_request():
    g.start = time.time()


@app.teardown_request
def teardown_request(exception=None):
    log.debug("Time: " + str(time.time() - g.start))


@app.route('/')
def index():
    # redirect to docs - relativ url
    response = redirect('./docs/')
    response.autocorrect_location_header = False
    return response


@app.route('/info', methods=['GET'])
def info():
    """Get model metadata.
       ---
       parameters:
         - name: model
           in: query
           description: Key of the selected model. If not provided, the default model will be used.
           required: false
           type: string
         - name: authorization
           in: header
           description: Authorization Token
           required: false
           type: string
       produces:
         - "application/json"
       responses:
         200:
           description: Metadata of selected model.
           schema:
            id: ModelMetadata
            type: object
            properties:
                data:
                  type: string
                  format: binary
                metadata:
                  $ref: '#/definitions/UnifiedFormatMetadata'
                errors:
                  $ref: '#/definitions/UnifiedFormatError'
         500:
            description: Server error.

       definitions:
          UnifiedFormatMetadata:
            type: object
            properties:
              message:
                type: string
              time:
                type: integer
                format: int64
              status:
                type: integer
                format: int32
              type:
                type: string
              query:
                type: string
          UnifiedFormatError:
            type: object
            properties:
              message:
                type: string
              code:
                type: integer
                format: int32
              type:
                type: string
              description:
                type: string
    """

    try:
        return to_json(get_response(model_handler.info(model=request.args.get('model'))))

    except Exception as e:
        traceback.print_exc()
        return to_json(get_error_response(code=500,
                                          message=getattr(e, 'message', repr(e)),
                                          description=traceback.format_exc(),
                                          execution_time=get_execution_time()))


@app.route('/predict', methods=['POST'])
def predict():
    """Make a prediction on the given data item.
       ---
       parameters:
         - in: body
           name: body
           description: Input data for prediction.
           required: false
           schema:
             type: string
         - name: model
           in: query
           description: Key of the selected model. If not provided, the default model will be used.
           required: false
           type: string
         - name: limit
           in: query
           description: Limit the number of returned predictions. Only applied if supported by the given model.
           required: false
           type: integer
           format: int32
         - name: file
           in: formData
           description: Upload a file as input data. You also need to select multipart/form-data as content type.
           required: false
           type: file
         - name: authorization
           in: header
           description: Authorization Token
           required: false
           type: string
       consumes:
         - "text/plain"
         - "application/json"
         - "application/xml"
         - "application/octet-stream"
         - "multipart/form-data"
       produces:
         - "application/json"
       responses:
         200:
           description: Predictions for the input data.
           schema:
            id: PredictionResult
            type: object
            properties:
                data:
                  type: string
                  format: binary
                metadata:
                  $ref: '#/definitions/UnifiedFormatMetadata'
                errors:
                  $ref: '#/definitions/UnifiedFormatError'
         500:
           description: Server error.
    """

    try:
        mime_type = request.mimetype

        if mime_type is None:
            log.debug("No content type provided. Assuming text/plain.")
            mime_type = "text/plain"

        if mime_type in ["text/plain", "application/json", "application/xml"]:
            # convert data to unicode text, alternative: data = request.data.decode(request.charset)
            data = request.get_data(as_text=True)
        elif mime_type in ["multipart/form-data"]:
            if 'file' in request.files:
                file = request.files['file']
                data = file.read()
            else:
                return to_json(get_error_response(code=400,
                                                  message='No file was uploaded.',
                                                  execution_time=get_execution_time()))
        elif mime_type in ["application/octet-stream"]:
            # just use body data as is
            data = request.data
        else:
            return to_json(get_error_response(code=400,
                                              message='Unknown content type: ' + request.content_type,
                                              execution_time=get_execution_time()))

        if not data:
            # body should be provided
            return to_json(get_error_response(code=400,
                                              message='The body parameter should not be empty.',
                                              execution_time=get_execution_time()))

        return to_json(get_response(data=model_handler.predict(data, **request.args.to_dict()),
                                    execution_time=get_execution_time()))

    except Exception as e:
        traceback.print_exc()
        return to_json(get_error_response(code=500,
                                          message=getattr(e, 'message', repr(e)),
                                          description=traceback.format_exc(),
                                          execution_time=get_execution_time()))


@app.before_first_request
def _run_on_start():
    log.debug("Server is about to start")
    if not model_handler.initialized:
        model_handler.init()


@app.errorhandler(404)
def not_found(error):
    return make_response(jsonify({'error': 'Not found'}), 404)


def get_execution_time():
    return time.time() - g.start


if __name__ == '__main__':
    app.run(debug=True, threaded=True)
