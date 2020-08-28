import os
from pathlib import Path
from collections import defaultdict
import pandas as pd
import numpy as np
import pickle
from unified_model.model_types import TextClassificationModel
from unified_model.utils import SCORE_COLUMN, ITEM_COLUMN, overrides
from fastai.text import load_learner, Learner

class FastaiTextClassifier(TextClassificationModel):
    """FastaiTextClassifier supporting models exported from Fastai. 
    
    """  
    REQUIREMENTS = ["fastai>=1.0.45"]

    def __init__(self, learner: Learner, **kwargs):
        
        super().__init__(**kwargs)  
        self.learn = learner
        c2i = self.learn.data.single_ds.y.c2i
        self.i2c = {v:k for k,v in c2i.items()}
        self.classes = self.learn.data.single_ds.y.classes
        self.add_requirements(FastaiTextClassifier.REQUIREMENTS)
   
    
    @classmethod
    def from_export(cls, export_path:str, export_name:str = 'export.pkl'):
        """Creates a `FastaiTextClassifier` from an export pickle dump.
        
        Attributes:
            export_path: Path for the Learner's cache folder.
            export_name: Export file name (.pkl).
        """
        learn = load_learner(Path(export_path), fname=export_name)
        return cls(learn)

    @overrides
    def _predict(self, data:str, limit:int = None, lower:bool=True, **kwargs):
        
        if not isinstance(data, str):
            raise ValueError("Argument `data` has to be a string.")
        
        if lower: data = data.lower()
        
        if limit is None: limit = len(self.classes)
        else:
            if limit > len(self.classes): limit=len(self.classes)

        # get prediction in the forms of {class, label, predictions}
        _, label, probs = self.learn.predict(data)
        
        if limit == 1:
            result = [[self.i2c.get(label.item()), probs.max().item()]]
        elif limit > 1:
            result = []
            probs = {c:probs[i].item() for i, c in enumerate(self.classes)}
            probs_sorted = sorted(probs.items(), key=lambda x: x[1], reverse=True)
            for item, score in probs_sorted[:limit]:
                result.append([item, score])
        
        return pd.DataFrame(result, columns=[ITEM_COLUMN, SCORE_COLUMN])   
    