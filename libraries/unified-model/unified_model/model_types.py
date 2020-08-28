import abc

import pandas as pd
import six

from unified_model import UnifiedModel
from unified_model.evaluation_utils import evaluate_classifier
from unified_model.utils import ITEM_COLUMN, SCORE_COLUMN, overrides

DEFAULT_LIMIT = 5


class RecommendationModel(UnifiedModel):
    __metaclass__ = abc.ABCMeta

    def __init__(self, **kwargs):
        super(RecommendationModel, self).__init__(**kwargs)

    @overrides
    @abc.abstractmethod
    def _predict(self, data, limit=DEFAULT_LIMIT, **kwargs) -> pd.DataFrame:
        raise NotImplementedError('Method not implemented')

    @overrides
    def _validate_prediction_result(self, result):
        if not isinstance(result, pd.DataFrame):
            raise ValueError("Result data is not a pandas dataframe.")

        if ITEM_COLUMN not in result.columns:
            raise ValueError("Result dataframe does not contain the " + ITEM_COLUMN + " column.")

        if SCORE_COLUMN not in result.columns:
            raise ValueError("Result dataframe does not contain the " + SCORE_COLUMN + " column.")

    @overrides
    def _update_default_metadata(self):
        super(RecommendationModel, self)._update_default_metadata()

        output_sign = {
            "type": "dataframe",
            "columns": [
                ITEM_COLUMN,
                SCORE_COLUMN
            ]
        }

        self._info_dict["signature"]["output"] = output_sign

    @overrides
    def predict(self, data, limit: int = DEFAULT_LIMIT, **kwargs) -> pd.DataFrame:
        """
        Make a prediction on the given data item.

        # Arguments
            data (string or bytes): Input data.
            limit (integer): Limit the number of returned predictions.
            **kwargs: Provide additional keyword-based parameters.

        # Returns
        Predictions for the input data.

        # Raises
            NotImplementedError: Method is not implemented (please implement).
        """

        if limit:
            limit = int(limit)

        data = self._validate_and_transform_input(data)
        prediction = self._predict(data, limit=limit, **kwargs)
        self._validate_prediction_result(prediction)
        return prediction

    @overrides
    def evaluate(self, test_data: list, target_predictions: list, k: list = None, per_label=False, **kwargs):
        """
        Evaluate this model with given test dataset.

        # Arguments
            test_data (list): List of data items used for the evaluations
            target_predictions (list): List of true predictions for test data
            k (list): List of k values used for calculating metrics (optional)
            per_label (boolean): If 'True', also metrics per label are returned in addition to the overall metrics (optional)
            **kwargs: Provide additional keyword-based parameters.

        # Returns
        Dictionary of evaluation metrics
        """
        return evaluate_classifier(self, test_data, target_predictions, k, per_label)


class ClassificationModel(RecommendationModel):
    __metaclass__ = abc.ABCMeta

    def __init__(self, **kwargs):
        super(ClassificationModel, self).__init__(**kwargs)


class TextClassificationModel(ClassificationModel):
    __metaclass__ = abc.ABCMeta

    def __init__(self, **kwargs):
        super(TextClassificationModel, self).__init__(**kwargs)

    @overrides
    def predict(self, data: str, limit: int = DEFAULT_LIMIT, **kwargs) -> pd.DataFrame:
        """
        Make a classification on the given input text.

        # Arguments
            data (string): Input text.
            limit (integer): Limit the number of returned predictions.
            **kwargs: Provide additional keyword-based parameters.

        # Returns
        Predictions for the input data.

        # Raises
            NotImplementedError: Method is not implemented (please implement).
        """

        return super(TextClassificationModel, self).predict(str(data), limit=limit, **kwargs)

    @overrides
    def _update_default_metadata(self):
        super(TextClassificationModel, self)._update_default_metadata()

        input_sign = {
            "type": "string"
        }

        self._info_dict["signature"]["input"] = input_sign

    @overrides
    def _validate_and_transform_input(self, data):
        data = super(TextClassificationModel, self)._validate_and_transform_input(data)
        if not isinstance(data, six.string_types):
            self._log.warning(
                "Input data is not a string. Will be automatically converted, but this might cause trouble.")
        return str(data)
