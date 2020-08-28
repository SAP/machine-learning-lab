def add_monitoring(flask_app):
    # TODO not completly relativ since base url cannot be change to relativ
    # either use env variable or change base to suburl in nginx
    import flask

    # fix to make urls in webapp relativ
    original_url_for = flask.url_for

    def url_for(endpoint, **values):
        if endpoint == "dashboard.static":
            url = original_url_for(endpoint, **values)
            if url and str(url).startswith("/"):
                url = ".." + url
            return url
        elif endpoint == "dashboard.index":\
            # TODO change url to real base url based on env variable?
            pass
        return original_url_for(endpoint, **values)

    # flask.url_for = url_for
    # flask.helpers.url_for = url_for
    # only the urlfor in app is used for template rendering
    flask.app.url_for = url_for

    import flask_monitoringdashboard as dashboard
    dashboard.bind(flask_app)


def add_flask_profiler(flask_app):
    # https://github.com/muatik/flask-profiler
    from flask_profiler import Profiler
    # You need to declare necessary configuration to initialize
    # flask-profiler as follows:
    flask_app.config["flask_profiler"] = {
        "enabled": True,
        "storage": {
            "engine": "sqlite"
        },
        "basicAuth": {
            "enabled": True,
            "username": "admin",
            "password": "admin"
        },
        "ignore": [
            "^/static/.*"
        ]
    }

    profiler = Profiler()
    profiler.init_app(flask_app)