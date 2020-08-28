<h1 id="unified_model.UnifiedModel">UnifiedModel</h1>

```python
UnifiedModel(self, name:str=None, requirements:list=None, info_dict:dict=None, setup_script:str=None)
```

<h2 id="unified_model.UnifiedModel.load">load</h2>

```python
UnifiedModel.load(model_path:str, install_requirements:bool=False)
```

Load a pickled unified model.

__Arguments__

- __model_path (string)__: Path to pickled unified model.
- __install_requirements (boolean)__: If `True`, requirements will be automatically installed (optional).

__Returns__

Initialized unified model.

<h2 id="unified_model.UnifiedModel.save">save</h2>

```python
UnifiedModel.save(self, output_path:str, compress:bool=False, executable:bool=False) -> str
```

Save unified model as a single file to a given path.

__Arguments__

- __output_path (string)__: Path to save the model.
- __compress (boolean)__: If 'True', the model file will be compressed using zip deflated method (optional).
- __executable (boolean)__: If 'True', the model file will be converted to an executable pyz file (optional).
__Returns__

Full path to the saved model file.

<h2 id="unified_model.UnifiedModel.add_file">add_file</h2>

```python
UnifiedModel.add_file(self, file_key:str, file_path:str)
```

Add a file to the model. The file will be bundled into the model file when the model is saved.

__Arguments__

- __file_key (string)__: File name to identify the added file.
- __file_path (string)__: Path to the file that should be added.

<h2 id="unified_model.UnifiedModel.get_file">get_file</h2>

```python
UnifiedModel.get_file(self, file_key:str) -> str
```

Get a file by name that is bundled with the model.

__Arguments__

- __file_key (string)__: File name to identify the added file.

__Returns__

Full path to the requested file or `None` if the file does not exist.

<h2 id="unified_model.UnifiedModel.update_info">update_info</h2>

```python
UnifiedModel.update_info(self, info_dict:dict)
```

Update the info dictionary.

__Arguments__

- __info_dict (dict)__: Dictionary to add additional metadata about the model (optional)

__Returns__

Info dictionary.

<h2 id="unified_model.UnifiedModel.info">info</h2>

```python
UnifiedModel.info(self)
```

Get the info dictionary that contains additional metadata about the model.

__Returns__

Info dictionary.

<h2 id="unified_model.UnifiedModel.predict">predict</h2>

```python
UnifiedModel.predict(self, data, **kwargs)
```

Make a prediction on the given data item.

__Arguments__

- __data (string or bytes)__: Input data.
- __**kwargs__: Provide additional keyword-based parameters.

__Returns__

Predictions for the input data.

__Raises__

- `NotImplementedError`: Method is not implemented (please implement).

<h2 id="unified_model.UnifiedModel.predict_batch">predict_batch</h2>

```python
UnifiedModel.predict_batch(self, data_batch:list, **kwargs)
```

Make a predictions on a batch of data items.

__Arguments__

- __data_batch (list)__: List of data items
- __**kwargs__: Provide additional keyword-based parameters.

__Returns__

List of predictions for a batch of data items.

<h2 id="unified_model.UnifiedModel.evaluate">evaluate</h2>

```python
UnifiedModel.evaluate(self, test_data:list, target_predictions:list, **kwargs) -> dict
```

Evaluate this model with given test dataset.

__Arguments__

- __test_data (list)__: List of data items used for the evaluations
- __target_predictions (list)__: List of true predictions for test data
- __**kwargs__: Provide additional keyword-based parameters.

__Returns__

Dictionary of evaluation metrics

<h2 id="unified_model.UnifiedModel.add_requirements">add_requirements</h2>

```python
UnifiedModel.add_requirements(self, requirements:list)
```

Add requirements to model.

__Arguments__

- __requirements (list)__: List of dependencies. Can be either an imported module or a requirement installable via pip

<h1 id="unified_model.model_types">unified_model.model_types</h1>


<h2 id="unified_model.model_types.RecommendationModel">RecommendationModel</h2>

```python
RecommendationModel(self, **kwargs)
```

<h3 id="unified_model.model_types.RecommendationModel.predict">predict</h3>

```python
RecommendationModel.predict(self, data, limit:int=5, **kwargs) -> pandas.core.frame.DataFrame
```

Make a prediction on the given data item.

__Arguments__

- __data (string or bytes)__: Input data.
- __limit (integer)__: Limit the number of returned predictions.
- __**kwargs__: Provide additional keyword-based parameters.

__Returns__

Predictions for the input data.

__Raises__

- `NotImplementedError`: Method is not implemented (please implement).

<h3 id="unified_model.model_types.RecommendationModel.evaluate">evaluate</h3>

```python
RecommendationModel.evaluate(self, test_data:list, target_predictions:list, k:list=None, per_label=False, **kwargs)
```

Evaluate this model with given test dataset.

__Arguments__

- __test_data (list)__: List of data items used for the evaluations
- __target_predictions (list)__: List of true predictions for test data
- __k (list)__: List of k values used for calculating metrics (optional)
- __per_label (boolean)__: If 'True', also metrics per label are returned in addition to the overall metrics (optional)
- __**kwargs__: Provide additional keyword-based parameters.

__Returns__

Dictionary of evaluation metrics

<h2 id="unified_model.model_types.TextClassificationModel">TextClassificationModel</h2>

```python
TextClassificationModel(self, **kwargs)
```

<h3 id="unified_model.model_types.TextClassificationModel.predict">predict</h3>

```python
TextClassificationModel.predict(self, data:str, limit:int=5, **kwargs) -> pandas.core.frame.DataFrame
```

Make a classification on the given input text.

__Arguments__

- __data (string)__: Input text.
- __limit (integer)__: Limit the number of returned predictions.
- __**kwargs__: Provide additional keyword-based parameters.

__Returns__

Predictions for the input data.

__Raises__

- `NotImplementedError`: Method is not implemented (please implement).

<h1 id="unified_model.cli_handler">unified_model.cli_handler</h1>


<h2 id="unified_model.cli_handler.serve">serve</h2>

```python
serve(model_path:str=None, port:int=None, host:str=None)
```
Serve a Unified Model via a REST API server.
<h2 id="unified_model.cli_handler.predict">predict</h2>

```python
predict(model_path:str=None, input_data=None, input_path:str=None, output_path:str=None, **kwargs)
```
Make a prediction on the given data item.
<h2 id="unified_model.cli_handler.convert">convert</h2>

```python
convert(model_path:str, model_format:str, output_path:str)
```
Convert a Unified Model into another format.
<h1 id="unified_model.model_handler">unified_model.model_handler</h1>


<h2 id="unified_model.model_handler.init">init</h2>

```python
init(default_model_key:str=None, install_requirements:bool=True)
```

Initialize model handler. The model handler can be used to load, hold, and use multiple model instances.

__Arguments__

- __default_model_key (string)__: Key of the default model (optional)
- __install_requirements (boolean)__: If 'True', requirements of the model will be automatically installed (optional)

<h2 id="unified_model.model_handler.get_model">get_model</h2>

```python
get_model(model_key:str=None) -> unified_model.unified_model.UnifiedModel
```

Get the model instance for the given key.

__Arguments__

- __model_key (string)__: Key of the model. If 'None', return the default model.

__Returns__

Unified model instance.

__Raises__

- `Exception`: Model failed to load.

<h2 id="unified_model.model_handler.predict">predict</h2>

```python
predict(data, model:str=None, **kwargs)
```

Make a prediction on the given data item.

__Arguments__

- __data (string or bytes)__: Input data.
- __model (string)__: Key of the selected model. If 'None', the default model will be used (optional)
- __**kwargs__: Provide additional keyword-based parameters.

__Returns__

Predictions for the input data.

<h2 id="unified_model.model_handler.info">info</h2>

```python
info(model:str=None) -> dict
```

Get the info dictionary that contains additional metadata about the model.

__Arguments__

- __model (string)__: Key of the selected model. If 'None', the default model will be used (optional)

__Returns__

Info dictionary.

<h1 id="unified_model.compatibility_utils">unified_model.compatibility_utils</h1>


<h2 id="unified_model.compatibility_utils.SklearnWrapper">SklearnWrapper</h2>

```python
SklearnWrapper(self, unified_model)
```

<h3 id="unified_model.compatibility_utils.SklearnWrapper.predict">predict</h3>

```python
SklearnWrapper.predict(self, X)
```

Perform prediction task on X.

__Arguments__

- __X__: ({array-like, sparse matrix}, shape = [n_samples, n_features]) Input vectors, where n_samples is the number of samples and n_features is the number of features.
- __output_file_path (string)__: Path to save the model.

__Returns__

`y `: array, shape = [n_samples]  or [n_samples, n_outputs]
    Predicted target values for X.

<h3 id="unified_model.compatibility_utils.SklearnWrapper.predict_proba">predict_proba</h3>

```python
SklearnWrapper.predict_proba(self, X)
```

Return probability estimates for the tests vectors X.

__Arguments__

- __X__: {array-like, sparse matrix}, shape = [n_samples, n_features] Input vectors, where n_samples is the number of samples and n_features is the number of features.
__Returns__

`P `: array-like or list of array-lke of shape = [n_samples, n_classes] Returns the probability of the sample for each class in the model, where classes are ordered arithmetically, for each output.

<h2 id="unified_model.compatibility_utils.convert_to_mlflow">convert_to_mlflow</h2>

```python
convert_to_mlflow(unified_model, output_path:str) -> str
```

Convert the given unified model into a mlflow model.

__Arguments__

- __unified_model (UnifiedModel or str)__: Unified model instance or path to model file
- __output_path (string)__: Path to save the model.

__Returns__

Full path to the converted model

<h2 id="unified_model.compatibility_utils.convert_to_pipelineai">convert_to_pipelineai</h2>

```python
convert_to_pipelineai(unified_model, output_path:str) -> str
```

Convert the given unified model into a pipelineai model.

__Arguments__

- __unified_model (UnifiedModel or str)__: Unified model instance or path to model file
- __output_path (string)__: Path to save the model.

__Returns__

Full path to the converted model

<h2 id="unified_model.compatibility_utils.convert_to_pex">convert_to_pex</h2>

```python
convert_to_pex(unified_model, output_file_path:str) -> str
```

Convert the given unified model into an executable PEX file.

__Arguments__

- __unified_model (UnifiedModel or str)__: Unified model instance or path to model file
- __output_file_path (string)__: Path to save the model.

__Returns__

Full path to the converted model

<h1 id="unified_model.evaluation_utils">unified_model.evaluation_utils</h1>


<h2 id="unified_model.evaluation_utils.compare_models">compare_models</h2>

```python
compare_models(unified_models:list, data_list:list, target_predictions:list, styled=True, **kwargs) -> pandas.core.frame.DataFrame
```

Compare evaluation metrics for the given list of models.

__Arguments__

- __data_list (list)__: List of data items used for the evaluations.
- __target_predictions (list)__: List of true predictions for test data.
- __styled (boolean)__: If 'True', a styled DataFrame will be returned (with coloring, etc.)
- __**kwargs__: Provide additional keyword-based parameters.

__Returns__

DataFrame that summarizes the metrics of all of the given models.

<h2 id="unified_model.evaluation_utils.test_unified_model">test_unified_model</h2>

```python
test_unified_model(model_instance:unified_model.unified_model.UnifiedModel, data=None, conda_environment=False)
```

Helps to test whether your model instance can be successfully loaded in another python environment.
This method saves the model instance, loads the model file in another python process,
and (optionally) calls `predict()` with the provided test data.

__Arguments__

- __model_instance (UnifiedModel)__: Unified model instance.
- __data (string or bytes)__: Input data to test the model (optional).
- __conda_environment (bool)__: If `True`, a clean conda environment will be created for the test (optional).

<h1 id="unified_model.ensemble_utils.VotingEnsemble">VotingEnsemble</h1>

```python
VotingEnsemble(self, models:list, strategy:str='relative_score', multiply_limit:float=1, **kwargs)
```

Initialize voting ensemble.

__Arguments__

- __models (list)__: List of unified models
- __strategy (str)__: Ensemble Strategy (relative_score, one_vote, total_score, rank_vote, rank_averaging, highest_scores)
- __multiply_limit (float)__: Will determine how much more data is requested for every model prediction (optional)
- __**kwargs__: Provide additional keyword-based parameters.

