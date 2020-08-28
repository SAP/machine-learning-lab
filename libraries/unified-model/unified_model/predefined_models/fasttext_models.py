import types, os

import pandas as pd
from pyfasttext import FastText

from unified_model.model_types import TextClassificationModel
from unified_model.utils import get_file_name, simplify, SCORE_COLUMN, ITEM_COLUMN, overrides


class FasttextClassifier(TextClassificationModel):
    FT_MODEL_KEY = "ft_classifier.model"

    REQUIREMENTS = [
        "pyfasttext"
    ]

    def __init__(self, ft_classifier_path, transform_func=None, init_func=None, **kwargs):
        super(FasttextClassifier, self).__init__(**kwargs)

        if not os.path.isfile(ft_classifier_path):
            raise FileNotFoundError('File does not exist: %s' % ft_classifier_path)

        if self.name is None:
            # use filename without extension as name
            self.name = simplify(get_file_name(ft_classifier_path))

        self.init_func = init_func
        self.transform_func = transform_func

        self.add_requirements(FasttextClassifier.REQUIREMENTS)

        self.add_file(FasttextClassifier.FT_MODEL_KEY, ft_classifier_path)
        self._init_model()

    @overrides
    def _init_model(self):
        self.model_instance = FastText(self.get_file(FasttextClassifier.FT_MODEL_KEY))
        if self.init_func:
            self.init_func(model=self)

    @overrides
    def _save_model(self, output_path):
        del self.model_instance

    @overrides
    def _predict(self, data, limit=None, **kwargs):
        if not limit or limit > self.model_instance.nlabels:
            limit = self.model_instance.nlabels

        if self.transform_func:
            if isinstance(self.transform_func, types.FunctionType):
                data = self.transform_func(data, model=self, **kwargs)
            else:
                self._log.warning("Provided data transformer is not a function.")

        prediction = self.model_instance.predict_proba_single(data, k=limit)
        if prediction == []:
            # add no predictions of failed to predict something.
            # TODO is this the best solution
            new_labels = ["NO_PREDICTION"] * limit
            new_probabilities = [0.0] * limit
            prediction = list(zip(new_labels, new_probabilities))

        return pd.DataFrame(prediction, columns=[ITEM_COLUMN, SCORE_COLUMN])

    @overrides
    def predict_batch(self, data, limit=None, **kwargs):
        # Todo predict batch better function
        if not limit or limit > self.model_instance.nlabels:
            limit = self.model_instance.nlabels

        if self.transform_func:
            if isinstance(self.transform_func, types.FunctionType):
                # transform is only on a single item
                data = [self.transform_func(item, model=self, **kwargs) for item in data]
            else:
                self._log.warning("Provided data transformer is not a function.")

        prediction = self.model_instance.predict_proba(data, k=limit)

        labels = []
        probabilities = []

        for ind, entry in enumerate(prediction):
            if entry == []:
                new_labels = ["NO_PREDICTION"] * limit
                new_probabilities = [0.0] * limit
            else:
                new_labels, new_probabilities = list(zip(*entry))
            labels += [list(new_labels)]
            probabilities += [list(new_probabilities)]

        df = pd.DataFrame()
        df[ITEM_COLUMN] = labels
        df[SCORE_COLUMN] = probabilities

        return df
