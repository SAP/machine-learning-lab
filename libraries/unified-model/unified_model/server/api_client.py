class UnifiedModelClient(object):
    def __init__(self, endpoint_url):  # how to provide token, etc..?
        pass

    def info(self):
        return {}

    def predict(self, data, **kwargs):
        return None

    def predict_batch(self, data_batch: list, **kwargs):
        return None
