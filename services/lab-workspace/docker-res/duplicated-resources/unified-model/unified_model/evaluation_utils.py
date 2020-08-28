import logging
import timeit

import numpy as np
import pandas as pd
from tqdm import tqdm

from unified_model import UnifiedModel
from unified_model.utils import truncate_middle, ITEM_COLUMN, SCORE_COLUMN

log = logging.getLogger(__name__)

UNKNOWN_ITEM = '<UNK>'

# https://en.wikipedia.org/wiki/Evaluation_measures_(information_retrieval)
# http://scikit-learn.org/stable/modules/model_evaluation.html

def f1_score(precision, recall):
    return 2 * precision * recall / (precision + recall)


def evaluate_classifier(unified_model, test_data: list, target_predictions: list, k: list = None, per_label=False):
    # TODO multithreaded evaluation
    k = [1, 5] if k is None else [k] if isinstance(k, int) else k  # set default value for k
    k.sort()
    pred_labels, pred_scores, in_top_k, avg_pred_time = _process_predictions(unified_model,
                                                                             test_data,
                                                                             target_predictions, k)
    scored_labels = _score_labels(target_predictions, k, pred_labels, in_top_k)

    metrics = _calculate_metrics(scored_labels, k)
    metrics['avg_prediction_time'] = avg_pred_time

    if per_label:
        return metrics, scored_labels
    else:
        return metrics


def _calculate_metrics(scored_labels, k):
    metrics = {}
    for i in k:
        i = str(i)
        try:
            metrics['micro_precision@k' + i] = scored_labels['true_positives@k' + i].sum() / scored_labels[
                'predicted_count@k' + i].sum()
        except ZeroDivisionError:
            metrics['micro_precision@k' + i] = 0

        metrics['micro_recall@k' + i] = scored_labels['true_positives@k' + i].sum() / scored_labels['count'].sum()

        try:
            metrics['micro_f1@k' + i] = f1_score(metrics['micro_precision@k' + i], metrics['micro_recall@k' + i])
        except ZeroDivisionError:
            metrics['micro_f1@k' + i] = 0

        metrics['macro_precision@k' + i] = scored_labels['precision@k' + i].mean()
        metrics['macro_recall@k' + i] = scored_labels['recall@k' + i].mean()
        metrics['macro_f1@k' + i] = scored_labels['f1@k' + i].mean()

    return metrics


def _score_labels(target_predictions, k, pred_labels, in_top_k):
    unique_labels = list(set(target_predictions))
    target_predictions = np.array(target_predictions)  # convert true predictions to no array

    columns = ['count']  # tp + fn
    for i in k:
        i = str(i)
        columns.append('predicted_count@k' + i)  # tp + fp
        columns.append('true_positives@k' + i)
        columns.append('precision@k' + i)
        columns.append('recall@k' + i)
        columns.append('f1@k' + i)

    df = pd.DataFrame(0, columns=columns, index=unique_labels)
    for label in unique_labels:
        df['count'][label] = np.sum(target_predictions == label)
        for i in k:
            df['predicted_count@k' + str(i)][label] = np.sum(pred_labels[:, :i].flatten() == label)
            df['true_positives@k' + str(i)][label] = np.sum(in_top_k[i][target_predictions == label])

    for i in k:
        i = str(i)
        df['precision@k' + i] = df['true_positives@k' + i] / df['predicted_count@k' + i]
        df['recall@k' + i] = df['true_positives@k' + i] / df['count']
        df['f1@k' + i] = f1_score(df['precision@k' + i], df['recall@k' + i])

    df = df.fillna(0)
    return df.sort_values(by='count', ascending=False)


def _fill_missing_predictions(df: pd.DataFrame, max_k: int) -> pd.DataFrame:
    for i in range(max_k - df.shape[0]):
        df = df.append({ITEM_COLUMN: UNKNOWN_ITEM,
                        SCORE_COLUMN: 0}, ignore_index=True)
    return df


def _process_predictions(unified_model, test_data, target_predictions, k):
    # allow target_predictions to also contain a list of true labels per prediction
    target_predictions = np.array(target_predictions)  # convert true predictions to no array
    start_time = timeit.default_timer()

    predictions = []
    for data in tqdm(test_data, desc="Calculating metrics..."):

        try:
            prediction_result = unified_model.predict(data, limit=np.amax(k))
            if prediction_result.shape[0] < np.amax(k):
                log.warning("Model returned " + str(prediction_result.shape[0]) + " predictions, "
                            + str(np.amax(k)) + " were expected.")
                log.debug("Model data: " + str(data))
                prediction_result = _fill_missing_predictions(prediction_result, np.amax(k))
            if prediction_result is None:
                log.warning("Model returned no prediction (None).")
                log.debug("Model data: " + str(data))
                # add empty predictions
                prediction_result = _fill_missing_predictions(pd.DataFrame(columns=[ITEM_COLUMN, SCORE_COLUMN]),
                                                              np.amax(k))
        except Exception as ex:
            log.warning("Exception during prediction: " + str(ex))
            log.debug("Model data: " + str(data))
            prediction_result = _fill_missing_predictions(pd.DataFrame(columns=[ITEM_COLUMN, SCORE_COLUMN]), np.amax(k))

        predictions.append(prediction_result)

    avg_pred_time = ((timeit.default_timer() - start_time) / len(test_data) * 1000)

    pred_labels = np.array([prediction[ITEM_COLUMN].tolist() for prediction in predictions])
    pred_scores = np.array([prediction[SCORE_COLUMN].tolist() for prediction in predictions])

    in_top_k = {}
    for i in k:
        in_top_k[i] = np.array(
            [true_label in k_predictions[:i] for true_label, k_predictions in zip(target_predictions, pred_labels)])

    return pred_labels, pred_scores, in_top_k, avg_pred_time


def compare_models(unified_models: list, data_list: list, target_predictions: list, styled=True,
                   **kwargs) -> pd.DataFrame:
    """
    Compare evaluation metrics for the given list of models.

    # Arguments
        data_list (list): List of data items used for the evaluations.
        target_predictions (list): List of true predictions for test data.
        styled (boolean): If 'True', a styled DataFrame will be returned (with coloring, etc.)
        **kwargs: Provide additional keyword-based parameters.

    # Returns
    DataFrame that summarizes the metrics of all of the given models.
    """

    model_names = []
    metrics_per_model = []

    for model in unified_models:
        print("Calculating metrics for " + str(model))
        model_names.append(truncate_middle(str(model), 40))
        metrics_per_model.append(model.evaluate(data_list, target_predictions, **kwargs))

    ## compare evaluation df, also use color to show best and worst values
    # add random baseline and combined score
    df = pd.DataFrame(metrics_per_model, index=model_names)

    # https://pandas.pydata.org/pandas-docs/stable/style.html
    if styled:
        # return df.style.bar(color='#f0fbff')
        return df.style.background_gradient(cmap='BuGn', low=0.1, high=0.8, axis=0)
    else:
        return df


def test_unified_model(model_instance: UnifiedModel, data=None, conda_environment=False):
    """
    Helps to test whether your model instance can be successfully loaded in another python environment.
    This method saves the model instance, loads the model file in another python process,
    and (optionally) calls `predict()` with the provided test data.

    # Arguments
        model_instance (UnifiedModel): Unified model instance.
        data (string or bytes): Input data to test the model (optional).
        conda_environment (bool): If `True`, a clean conda environment will be created for the test (optional).
    """

    import sys
    import os
    import tempfile
    import subprocess
    import shutil

    log.info("Starting model test.")
    temp_test_folder = tempfile.mkdtemp()
    saved_model_path = model_instance.save(os.path.join(temp_test_folder, "test_model"))

    python_runtime = sys.executable

    CONDA_ENV = "model-test-env"
    if conda_environment:
        log.info("Creating clean conda environment.")
        try:
            log.info(subprocess.check_output("conda create -n " + CONDA_ENV + " python=3.6 cython -y",
                                             stderr=subprocess.STDOUT, shell=True).decode("utf-8"))
            log.info("Installing unified model.")
            log.info(
                subprocess.check_output("/opt/conda/envs/"
                                        + CONDA_ENV
                                        + "/bin/pip install --upgrade unified-model",
                                        stderr=subprocess.STDOUT,
                                        shell=True).decode("utf-8"))

            python_runtime = "/opt/conda/envs/" + CONDA_ENV + "/bin/python"
        except subprocess.CalledProcessError as e:
            log.info("Failed to create conda environment: \n" + e.output.decode("utf-8"))

    test_command = python_runtime + " " + saved_model_path + ' predict'
    if data:
        test_command += ' --input-data "' + str(data) + '"'

    log.info("Executing " + test_command)

    try:
        log.info(subprocess.check_output(test_command, stderr=subprocess.STDOUT, shell=True).decode("utf-8"))
        log.info("Finished model test successfully!")
    except subprocess.CalledProcessError as e:
        log.info("Test failed: \n" + e.output.decode("utf-8"))

    shutil.rmtree(temp_test_folder)

    if conda_environment:
        log.info("Removing conda environment.")
        subprocess.call("conda remove --name " + CONDA_ENV + " --all -y", shell=True)
