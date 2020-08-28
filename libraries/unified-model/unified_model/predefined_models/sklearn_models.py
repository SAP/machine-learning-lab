import types

import numpy as np
import pandas as pd

from unified_model.model_types import TextClassificationModel
from unified_model.utils import ITEM_COLUMN, SCORE_COLUMN, overrides


def default_analyzer(x):
    return x


def default_transformer(text, **kwargs):
    # tokenize text as default
    if isinstance(text, (list, pd.core.series.Series, np.ndarray)):
        tokenized_text = text
    else:
        tokenized_text = str(text).strip().split()  # split by whitespace
    return tokenized_text


class SklearnTextClassifier(TextClassificationModel):
    REQUIREMENTS = [
        "sklearn"
    ]

    def __init__(self, sklearn_classifier, transform_func=default_transformer, init_func=None, **kwargs):
        super(SklearnTextClassifier, self).__init__(**kwargs)

        self.sklearn_classifier = sklearn_classifier

        self.add_requirements(SklearnTextClassifier.REQUIREMENTS)

        self.transform_func = transform_func

        self.init_func = init_func

        self._init_model()

    @overrides
    def _init_model(self):
        if self.init_func:
            self.init_func(model=self)

    @overrides
    def _predict(self, data, limit=None, **kwargs):
        if limit is None:
            limit = len(self.sklearn_classifier.classes_)

        if self.transform_func:
            if isinstance(self.transform_func, types.FunctionType):
                data = self.transform_func(data, model=self, **kwargs)
            else:
                self._log.warning("Provided data transformer is not a function.")

        result = []
        if hasattr(self.sklearn_classifier, 'predict_proba'):
            class_confidences = self.sklearn_classifier.predict_proba([data])
            for index in np.argsort(class_confidences)[:, :-limit - 1:-1][0]:
                item = self.sklearn_classifier.classes_[index]
                score = class_confidences[0][index]
                result.append([str(item), score])
        else:
            item = self.sklearn_classifier.predict([data])[0]
            score = 1.0
            result.append([str(item), score])

        return pd.DataFrame(result, columns=[ITEM_COLUMN, SCORE_COLUMN])

    @overrides
    def predict_batch(self, data, limit=None, **kwargs):
        if limit is None:
            limit = len(self.sklearn_classifier.classes_)

        if self.transform_func:
            if isinstance(self.transform_func, types.FunctionType):
                # transform is only on a single item
                data = [self.transform_func(item, model=self, **kwargs) for item in data]
            else:
                self._log.warning("Provided data transformer is not a function.")

        result = pd.DataFrame()

        if hasattr(self.sklearn_classifier, 'predict_proba'):
            class_confidences = self.sklearn_classifier.predict_proba(data)

            order = np.argsort(class_confidences, axis=1)
            top_predictions = self.sklearn_classifier.classes_[order[:, -limit:]]

            ind = np.repeat(np.arange(0, len(class_confidences)), limit).reshape(len(class_confidences), limit)
            top_probabilities = class_confidences[ind[:, :limit], order[:, -limit:]]

            result[ITEM_COLUMN] = top_predictions.tolist()
            result[SCORE_COLUMN] = top_probabilities.tolist()
        else:
            preds = self.sklearn_classifier.predict(data)

            result[ITEM_COLUMN] = preds
            result[SCORE_COLUMN] = [1.0] * len(preds)

        return result
