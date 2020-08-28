"""Utilities for HTTP request operations."""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import os
import posixpath
import re

try:
    from urlparse import urlsplit, urlparse
    from urllib import unquote
except ImportError:  # Python 3
    from urllib.parse import urlsplit, unquote, urlparse

url_validator = re.compile(
    r'^(?:http|ftp)s?://'  # http:// or https://
    r'(?:(?:[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?\.)+(?:[A-Z]{2,6}\.?|[A-Z0-9-]{2,}\.?)|'  # domain...
    r'localhost|'  # localhost...
    r'\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})'  # ...or ip
    r'(?::\d+)?'  # optional port
    r'(?:/?|[/?]\S+)$', re.IGNORECASE)


def is_valid_url(url: str) -> bool:
    """Check is the provided URL is valid."""
    return re.match(url_validator, url) is not None


def is_downloadable(url: str) -> bool:
    """
    Does the url is valid and contain a downloadable resource
    """
    try:
        import requests
        h = requests.head(url, allow_redirects=True)
        header = h.headers
        content_type = header.get('content-type')
        if content_type and 'html' in content_type.lower():
            return False
        return True
    except:
        return False


def url2filename(url: str) -> str:
    """Return basename corresponding to url.
    >>> print(url2filename('http://example.com/path/to/file%C3%80?opt=1'))
    fileÀ
    >>> print(url2filename('http://example.com/slash%2fname')) # '/' in name
    Traceback (most recent call last):
    ...
    ValueError
    """
    urlpath = urlsplit(url).path
    basename = posixpath.basename(unquote(urlpath))
    if (os.path.basename(basename) != basename or
            unquote(posixpath.basename(urlpath)) != basename):
        raise ValueError  # reject '%2f' or 'dir%5Cbasename.ext' on Windows
    return basename
