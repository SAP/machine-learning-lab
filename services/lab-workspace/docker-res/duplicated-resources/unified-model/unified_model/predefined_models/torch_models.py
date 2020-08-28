import os
import types
from collections import defaultdict
import pandas as pd
import numpy as np
import pickle
from unified_model.model_types import TextClassificationModel
from unified_model.utils import SCORE_COLUMN, ITEM_COLUMN, overrides
import torch
import torch.nn as nn


def default_transformer(text, **kwargs):
    # tokenize text as default
    if isinstance(text, (list, pd.core.series.Series, np.ndarray)):
        tokenized_text = text
    else:
        tokenized_text = str(text).strip().split()  # split by whitespace
    return tokenized_text


class TorchTextClassifier(TextClassificationModel):
    """TorchTextClassifier supporting text classifier models in PyTorch."""
    
    REQUIREMENTS = ["torch==1.0.0"
                   ]

    def __init__(self, model: nn.Module, token_to_id: dict, id_to_label: dict,  transform_func=default_transformer,
                 special_tokens: set = {'xxpad', 'xxunk'}, **kwargs):
        """
        Arguments
            model: A PyTorch model instance (input: sequence of IDs, output: distribution over clas labels)
            token_to_id: A mapping from token strings to token IDs. It must also contain special tokens for 
                         padding and unknwown words. See argument `special_tokens`.
            id_to_label: A mapping from label IDs to label strings (number of entries and classes are equal).
            special_tokens: Set of special tokens in vocabulary (used for numericalizing text. (such as padding and unknown token).
            requirements (optional): List of dependencies. Can be either an imported module or pip installable.
        
        Important:
        Attribute `model` is required to implement a `forward` method which outputs a torch.Tensor of shape (1, num_classes).
        
        """
        super().__init__(**kwargs)

        if not isinstance(model, torch.nn.Module):
            raise TypeError(
                "Argument `model` has to inherit from torch.nn.Module.")
        self.model = model.cpu()  # move to cpu
        
        if not isinstance(token_to_id, defaultdict):
            raise TypeError("Argument `token_to_id` has to be a defaultdict.")
        if not special_tokens.issubset(set(token_to_id.keys())):
            raise ValueError(
                f"Keys {special_tokens} has to be elements of `token_to_id`.")
        self.token_to_id = token_to_id

        if not isinstance(id_to_label, dict):
            raise TypeError("Argument `id_to_label` has to be a dict.")
        self.id_to_label = id_to_label

        self.add_requirements(TorchTextClassifier.REQUIREMENTS)

        self.transform_func = transform_func
     
    @classmethod
    def from_export(cls, model:nn.Module, export_path:str, device=None,
                   special_tokens:set = {'xxpad', 'xxunk'}):
                
        """Creates a `TorchTextClassifier` from cache. Assumes to have PyTorch model saved at `model`,
           and string-to-int/int-to-label mappings pickled at `stoi` and `itol`, respectively.
        
        Arguments:
            model: Instantiated PyTorch module defining the architecture.
            export_path: Path to exported model. Export is a pickled dictionary, which *must* contain
                         keys {'model_state', 'token_to_id', 'id_to_label'}, where `model_state` is the
                         __dict__ attribute of the trained model, 'token_to_id' is the mapping from tokens to ints,
                        and 'id_to_label' maps from ints to label strings.
            special_tokens: Set of default tokens in the model's vocabulary (such as padding and unknown).

        """

        # device = torch.device('cpu') if device is None else device
        # export = torch.load(export_path, map_location=device)
        
        with open(export_path, 'rb') as fin:
            export = pickle.load(fin)
        assert "model_state" in export.keys(), "Key `model_state` not found in pickled export."
        assert "token_to_id" in export.keys(), "Key `token_to_id` not found in pickled export."
        assert "id_to_label" in export.keys(), "Key `id_to_label` not found in pickled export."
        model.__dict__ = export['model_state']

        return cls(model, export['token_to_id'], export['id_to_label'], special_tokens=special_tokens)


    @overrides
    def _predict(self, data: str, limit: int = None, lower=True, **kwargs):
        
        if not isinstance(data, str):
            raise ValueError("Argument `data` has to be a string.")

        if limit is None:
            limit = len(self.id_to_label)

        if lower:
            data = data.lower()

        if self.transform_func:
            if isinstance(self.transform_func, types.FunctionType):
                data = self.transform_func(data, model=self, **kwargs)
            else:
                self._log.warning("Provided data transformer is not a function.")

        self.model.eval()  # turn on eval mode
        query = torch.unsqueeze(torch.LongTensor([self.token_to_id[w]
                                  for w in data]), dim=0)  # convert to token IDs, add batch dim
        
        try:
            y_pred = self.model(query)  # forward
        except RuntimeError:
            raise RuntimeError("Forward propagation has failed."
                               "Please check your model's forward method.")
        
        if len(self.id_to_label) != (y_pred.shape[0] and y_pred.shape[1]):
            raise RuntimeError("No. of labels in `id_to_label` does not match the number of outputs of `model`.")
        
        # logits -> softmax -> detach from graph -> numpy array
        y_proba = nn.functional.softmax(y_pred, dim=1).detach().numpy()
        if limit == 1:
            pred, prob = y_proba.argmax(), y_proba.max()
            result = [[self.id_to_label[pred], prob]]
        elif limit > 1:
            result = []
            items = np.squeeze(
                np.fliplr(np.argsort(y_proba, axis=-1)[:, -limit:]))
            scores = np.squeeze(
                np.fliplr(np.sort(y_proba, axis=-1)[:, -limit:]))
            for item, score in zip(items, scores):
                result.append([self.id_to_label[item], score])
        
        return pd.DataFrame(result, columns=[ITEM_COLUMN, SCORE_COLUMN])   
   

