"""Utilities for Math operations."""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import numpy as np


def outlier_bounds_std(data: list, m: int = 2):
    data = np.array(data)
    max_boundary = np.mean(data) + (m * np.std(data))
    min_boundary = np.mean(data) - (m * np.std(data))
    return min_boundary, max_boundary


def outlier_bounds_double_mad(data: list, thresh: float = 3.5):
    data = np.array(data)
    median = np.median(data)
    abs_dev = np.abs(data - median)
    left_mad = np.median(abs_dev[data <= median])
    right_mad = np.median(abs_dev[data >= median])
    min_boundary = thresh / 0.6745 * left_mad
    max_boundary = thresh / 0.6745 * right_mad
    return min_boundary, max_boundary


def outlier_bounds_mad(data: list, thresh: float = 3.5):
    data = np.array(data)
    if len(data.shape) == 1:
        data = data[:, None]
    median = np.median(data, axis=0)
    diff = np.sum((data - median) ** 2, axis=-1)
    diff = np.sqrt(diff)
    med_abs_deviation = np.median(diff)
    max_boundary = thresh / 0.6745 * med_abs_deviation
    min_boundary = 0
    return min_boundary, max_boundary


def outlier_bounds_percentile(data: list, thresh: int = 99):
    data = np.array(data)
    diff = (100 - thresh) / 2.0
    min_boundary, max_boundary = np.percentile(data, [diff, 100 - diff])
    return min_boundary, max_boundary


def outlier_bounds_iqr(data: list):
    data = np.array(data)
    p25 = np.percentile(data, 25)
    p75 = np.percentile(data, 75)
    min_boundary = p25 - 1.5 * (p75 - p25)
    max_boundary = p75 + 1.5 * (p75 - p25)
    return min_boundary, max_boundary
