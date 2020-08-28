import atexit
import operator
import os
import shutil
import tempfile

import pandas as pd
from unified_model import UnifiedModel, NotSupportedException
from unified_model.model_types import DEFAULT_LIMIT, RecommendationModel
from unified_model.utils import ITEM_COLUMN, SCORE_COLUMN, overrides


class EnsembleStrategy:
    RELATIVE_SCORE = "relative_score"
    ONE_VOTE = "one_vote"
    TOTAL_SCORE = "total_score"
    RANK_VOTE = "rank_vote"
    RANK_AVERAGING = "rank_averaging"
    HIGHEST_SCORES = "highest_scores"


def combine_predictions(models: list, data, limit: int = DEFAULT_LIMIT, strategy: str = EnsembleStrategy.RELATIVE_SCORE,
                        multiply_limit: float = 1):
    predicted_labels = {}
    for model in models:
        model_weight = 1
        if isinstance(model, tuple):
            model_weight = model[1]
            model = model[0]

        if limit:
            request_limit = int(limit * multiply_limit)
        else:
            request_limit = None

        if strategy == 'rank_averaging':
            request_limit = None

        predictions = model.predict(data, limit=request_limit)
        total_score = 0

        for index, row in predictions.iterrows():
            total_score += row[SCORE_COLUMN]

        for index, row in predictions.iterrows():
            item = row[ITEM_COLUMN]
            score = row[SCORE_COLUMN]

            if item not in predicted_labels:
                predicted_labels[item] = 0

            if strategy == EnsembleStrategy.ONE_VOTE:
                predicted_labels[item] += 1 * model_weight
            elif strategy == EnsembleStrategy.RELATIVE_SCORE:
                predicted_labels[item] += (score / total_score) * model_weight
            elif strategy == EnsembleStrategy.HIGHEST_SCORES:
                if score > predicted_labels[item]:
                    predicted_labels[item] = score
            elif strategy == EnsembleStrategy.TOTAL_SCORE:
                predicted_labels[item] += score * model_weight
            elif strategy == EnsembleStrategy.RANK_VOTE:
                min_value = 0.5
                predicted_labels[item] += min_value + (len(predictions) - (index - 1)) * (
                        (1 - min_value) / len(predictions)) * model_weight
            elif strategy == EnsembleStrategy.RANK_AVERAGING:
                predicted_labels[item] += (len(predictions) - (index - 1)) * (
                        1 / len(predictions)) * model_weight
            else:
                raise NotSupportedException("Voting strategy " + strategy + " is not supported")

    if limit is None:
        limit = len(predicted_labels)

    sorted_predictions = sorted(predicted_labels.items(), key=operator.itemgetter(1), reverse=True)[:limit]

    if strategy == EnsembleStrategy.RANK_AVERAGING:
        temp_sorted_predictions = sorted_predictions
        sorted_predictions = []
        for prediction in temp_sorted_predictions:
            # Average scores:
            sorted_predictions.append([prediction[0], prediction[1] / len(models)])

    return pd.DataFrame(sorted_predictions,
                        columns=[ITEM_COLUMN, SCORE_COLUMN])


class VotingEnsemble(RecommendationModel):
    """
    Initialize voting ensemble.

    # Arguments
        models (list): List of unified models
        strategy (str): Ensemble Strategy (relative_score, one_vote, total_score, rank_vote, rank_averaging, highest_scores)
        multiply_limit (float): Will determine how much more data is requested for every model prediction (optional)
        **kwargs: Provide additional keyword-based parameters.
    """

    def __init__(self, models: list, strategy: str = EnsembleStrategy.RELATIVE_SCORE, multiply_limit: float = 1,
                 **kwargs):
        super(VotingEnsemble, self).__init__(**kwargs)

        self.models = models

        for model in models:
            if not isinstance(model, RecommendationModel):
                self._log.warn("Model " + str(model) + " is not a recommendation model and might fail.")

        if not self.name:
            self.name = "voting_ensemble_" + strategy

        self.strategy = strategy
        self.multiply_limit = multiply_limit

        self.model_keys = []

    @overrides
    def _init_model(self):
        self.models = []

        try:
            # Check if install requirements was set
            self._install_requirements
        except AttributeError:
            # Otherwise set false as default
            self._install_requirements = False

        for model_key in self.model_keys:
            self.models.append(UnifiedModel.load(self.get_file(model_key),
                                                 install_requirements=self._install_requirements))

    @overrides
    def _save_model(self, output_path):
        temp_folder = tempfile.mkdtemp()

        # automatically remove temp directory if process exits
        def cleanup():
            shutil.rmtree(temp_folder)

        atexit.register(cleanup)

        for model in self.models:
            model_key = str(model) + "_" + str(id(model))
            model_path = model.save(os.path.join(temp_folder, model_key))
            self.add_file(os.path.basename(model_path), model_path)
            self.model_keys.append(os.path.basename(model_path))

        del self.models

    @overrides
    def _predict(self, data, limit=None, **kwargs):
        return combine_predictions(self.models, data, limit=limit,
                                   strategy=self.strategy,
                                   multiply_limit=self.multiply_limit)


def get_empty_dataframe(row_size):
    return pd.DataFrame(
        columns=[ITEM_COLUMN, SCORE_COLUMN],
        data=[["", ""]] * row_size)


class StackedEnsembleModel(UnifiedModel):

    def __init__(self, first_stage_model: RecommendationModel, second_stage_models: dict, **kwargs):
        super(StackedEnsembleModel, self).__init__(**kwargs)

        self.first_stage_model = first_stage_model
        self.second_stage_models = second_stage_models

    @overrides
    def _init_model(self, **kwargs):

        try:
            # Check if install requirements was set
            self._install_requirements
        except AttributeError:
            # Otherwise set false as default
            self._install_requirements = False

        self.second_stage_models = {}

        self.first_stage_model = UnifiedModel.load(
            self.get_file(self.first_stage_model_key),
            install_requirements=self._install_requirements)

        for category, key in self.second_stage_model_keys.items():
            self.second_stage_models[category] = UnifiedModel.load(
                self.get_file(key),
                install_requirements=self._install_requirements)

    @overrides
    def _save_model(self, output_path):
        temp_folder = tempfile.mkdtemp()

        # automatically remove temp directory if process exits
        def cleanup():
            shutil.rmtree(temp_folder)

        atexit.register(cleanup)

        self.second_stage_model_keys = {}

        self._store_model(
            self.first_stage_model,
            temp_folder,
            second_stage_category=None)

        for category, model in self.second_stage_models.items():
            self._store_model(
                model,
                temp_folder,
                second_stage_category=category)

        del self.first_stage_model
        del self.second_stage_models

    def _predict(self, data, limit=None, **kwargs):
        if limit:
            limit = int(limit)

        first_stage_pred = self.first_stage_model.predict(data, limit=1)

        prediction = first_stage_pred[ITEM_COLUMN][0]

        if prediction not in self.second_stage_models:
            return get_empty_dataframe(1)

        second_stage_model = self.second_stage_models[prediction]
        return second_stage_model.predict(data, limit=limit)

    def predict_batch(self, data, limit=None, **kwargs):
        # TODO update for new batch prediction
        first_stage_pred = self.first_stage_model.predict_batch(
            data,
            limit=1)

        first_stage_pred[ITEM_COLUMN] = first_stage_pred.apply(lambda x: x[ITEM_COLUMN][0], axis=1)
        first_stage_pred["data"] = data
        unique_first_stage_pred = first_stage_pred[ITEM_COLUMN].unique()

        result = []
        for unique in unique_first_stage_pred:
            select_df = first_stage_pred.loc[first_stage_pred[ITEM_COLUMN] == unique]
            if unique in self.second_stage_models:
                second_stage = self.second_stage_models[unique].predict_batch(select_df["data"], limit=limit)
            else:
                # TODO empty list for prediction X
                second_stage = get_empty_dataframe(len(select_df))
                # second_stage = give_default_preds(len(select_df),limit)
            second_stage.index = select_df.index
            result.append(second_stage)

        return pd.concat(result).sort_index(ascending=True)

    def _store_model(self, model, temp_folder, second_stage_category=None):
        model_key = str(model) + "_" + str(id(model))
        model_path = model.save(os.path.join(temp_folder, model_key))
        model_basename = os.path.basename(model_path)

        if not second_stage_category:
            self.first_stage_model_key = model_basename
        else:
            self.second_stage_model_keys[second_stage_category] = model_basename

        self.add_file(model_basename, model_path)

    @overrides
    def evaluate(self, test_data: list, target_predictions: list, **kwargs) -> dict:
        # Not easily done since we do not know the types of the second stage models
        raise NotImplementedError('Method not implemented')
