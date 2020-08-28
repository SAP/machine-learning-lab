"""Utilities for text/string operations."""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import re

_SIMPLIFY_STRING_PATTERN = re.compile(r"[^a-zA-Z0-9-]")


def safe_str(obj) -> str:
    try:
        return str(obj)
    except UnicodeEncodeError:
        return obj.encode('ascii', 'ignore').decode('ascii')


def simplify(text) -> str:
    text = safe_str(text)
    return _SIMPLIFY_STRING_PATTERN.sub("-", text.strip()).lower()


def simplify_list_to_str(items: list) -> str:
    simple_str = ""
    if not items:
        return simple_str
    # sort alphabetically
    items = sorted(items)
    for item in items:
        simple_str += simplify(item) + "-"
    return simple_str[:-1]


def simplify_dict_to_str(dictionary: dict) -> str:
    simple_str = ""
    if not dictionary:
        return simple_str
    # sort alphabetically
    keys = sorted(dictionary.keys())
    for key in keys:
        value = dictionary[key]
        if isinstance(value, list):
            value = simplify_list_to_str(value)
        else:
            value = simplify(value)
        simple_str += simplify(key) + "=" + value + "-"
    return simple_str[:-1]


def simplify_duration(duration_ms: int) -> str:
    if not duration_ms:
        return ""

    from dateutil.relativedelta import relativedelta as rd
    intervals = ['days', 'hours', 'minutes', 'seconds']
    rel_date = rd(microseconds=duration_ms * 1000)
    return ' '.join('{} {}'.format(getattr(rel_date, k), k) for k in intervals if getattr(rel_date, k))


def simplify_bytes(num, suffix: str = 'B') -> str:
    for unit in ['', 'Ki', 'Mi', 'Gi', 'Ti', 'Pi', 'Ei', 'Zi']:
        if abs(num) < 1024.0:
            return "%3.1f%s%s" % (num, unit, suffix)
        num /= 1024.0
    return "%.1f%s%s" % (num, 'Yi', suffix)


def resolve_camel_case(name: str) -> str:
    s1 = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', name)
    return re.sub('([a-z0-9])([A-Z])', r'\1_\2', s1).lower()


def truncate_middle(s, n):
    if len(s) <= n:
        # string is already short-enough
        return s
    # half of the size, minus the 3 .'s
    n_2 = int(n) / 2 - 3
    # whatever's left
    n_1 = n - n_2 - 3
    return '{0}...{1}'.format(s[:int(n_1)], s[-int(n_2):])
