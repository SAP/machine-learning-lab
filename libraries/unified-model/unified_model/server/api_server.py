import logging
import sys

from unified_model.server.api_endpoint import app

DEFAULT_PORT = 5000
DEFAULT_HOST = '127.0.0.1'


def run_waitress(port: int = DEFAULT_PORT, host: str = DEFAULT_HOST):
    from waitress import serve
    serve(app, host=host, port=port)


def run_gevent(port: int = DEFAULT_PORT, host: str = DEFAULT_HOST):
    from gevent.pywsgi import WSGIServer
    http_server = WSGIServer((host, port), app)
    http_server.serve_forever()


def run_tornado(port: int = DEFAULT_PORT, host: str = DEFAULT_HOST):
    from tornado.wsgi import WSGIContainer
    from tornado.httpserver import HTTPServer
    from tornado.ioloop import IOLoop

    http_server = HTTPServer(WSGIContainer(app))
    http_server.listen(port)
    IOLoop.instance().start()


def run_flask(port: int = DEFAULT_PORT, host: str = DEFAULT_HOST):
    app.run(debug=False, threaded=True, port=port, host=host)


def run_gunicorn(port: int = DEFAULT_PORT, host: str = DEFAULT_HOST):
    import gunicorn.app.base
    from gunicorn.six import iteritems

    # http://docs.gunicorn.org/en/latest/custom.html
    class StandaloneApplication(gunicorn.app.base.BaseApplication):

        def __init__(self, app, options=None):
            self.options = options or {}
            self.application = app
            super(StandaloneApplication, self).__init__()

        def load_config(self):
            config = dict([(key, value) for key, value in iteritems(self.options)
                           if key in self.cfg.settings and value is not None])
            for key, value in iteritems(config):
                self.cfg.set(key.lower(), value)

        def load(self):
            return self.application

    # Or Run with command line: gunicorn -w 4 -b 0.0.0.0:5000 api_server:app -k gevent
    # http://flask.pocoo.org/docs/0.12/deploying/wsgi-standalone/#gunicorn
    # probably best for production - maybe couple with nginx
    options = {
        'bind': '%s:%s' % (host, str(port)),
        'worker_class': 'gevent',
        'workers': 5,  # (multiprocessing.cpu_count() * 2) + 1
    }

    StandaloneApplication(app, options).run()


def run(port: int = DEFAULT_PORT, host: str = DEFAULT_HOST):
    if not port:
        port = DEFAULT_PORT

    if not host:
        host = DEFAULT_HOST

    run_flask(port=port, host=host)  # change to gunicorn


if __name__ == '__main__':
    logging.basicConfig(stream=sys.stdout, format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)
    run()
