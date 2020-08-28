import random

import pandas as pd

from unified_model.model_types import RecommendationModel, DEFAULT_LIMIT
from unified_model.utils import SCORE_COLUMN, ITEM_COLUMN


class RandomClassifierBaseline(RecommendationModel):
    def __init__(self, items, **kwargs):
        super(RandomClassifierBaseline, self).__init__(**kwargs)

        self.items = list(set(items))

    def _predict(self, data, limit=DEFAULT_LIMIT, **kwargs) -> pd.DataFrame:
        if limit is None:
            limit = len(self.items)

        result = []
        for item in random.sample(self.items, limit):
            result.append([str(item), 0])

        return pd.DataFrame(result, columns=[ITEM_COLUMN, SCORE_COLUMN])

    def _save_model(self, output_path: str):
        pass

    def _init_model(self):
        pass
