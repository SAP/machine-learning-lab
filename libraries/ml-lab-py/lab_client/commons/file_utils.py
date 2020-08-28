"""Utilities for file operations."""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import csv
import gzip
import json
import os
import pickle
import shutil
import subprocess
from datetime import datetime

import six
from six import iteritems


def get_filename(file_path: str, exclude_extension: bool = True) -> str:
    """
    Extracts the filename from a path.

    # Arguments
        file_path (string): Path to a file.
        exclude_extension (bool): If True, the file extension is removed.

    Returns:
    The filename extracted from the path.
    """
    if exclude_extension:
        return str(os.path.basename(file_path)).split('.', 1)[0]
    else:
        return os.path.basename(file_path)


def remove_file_extension(path: str) -> str:
    return str(path).split('.', 1)[0]


def get_folder_name(folder_path: str) -> str:
    """
    Extracts the folder name from a path.

    # Arguments
        folder_path (string): Path to a folder.

    Returns:
    The folder name extracted from the path.
    """
    return os.path.basename(os.path.normpath(folder_path))


def md5(file_path: str) -> str:
    """Returns the md5 hash of a given file."""
    import hashlib
    hash_md5 = hashlib.md5()
    with open(file_path, "rb") as f:
        for chunk in iter(lambda: f.read(4096), b""):
            hash_md5.update(chunk)
    return hash_md5.hexdigest()


def save_string(file_path: str, content: str):
    """
    Saves a string to a file.

    # Arguments
        file_path (string): Path to save file.
        content (string): Content to be saved.
    """

    if not os.path.exists(os.path.dirname(file_path)):
        os.makedirs(os.path.dirname(file_path))

    if not content:
        content = ""

    if not isinstance(content, six.string_types):
        content = str(content)

    with open(file_path, 'w') as fp:
        fp.write(content)


def save_object(obj, filename):
    """
    Pickles the passed data (with the highest available protocol) to disk using the passed filename.
    If the filename ends in '.gz' then the data will additionally be GZIPed before saving.
    If filename ends with '.feather' or '.fthr', it will try to save the file using feather (for dataframes).
    Note that feather does not support compression.

    # Arguments
        obj: The python object to pickle to disk (use a dict or list to save multiple objects)
        filename (str): String with the relative filename to save the data to. By convention should end in '.pkl' or 'pkl.gz' or '.feather'
    """

    folders = os.path.dirname(filename)
    if folders:
        os.makedirs(folders, exist_ok=True)

    fl = filename.lower()
    if fl.endswith('.gz'):
        if fl.endswith('.feather.gz') or fl.endswith('.fthr.gz'):
            # Since feather doesn't support writing to the file handle, we can't easily point it to gzip.
            raise NotImplementedError('Saving to compressed .feather not currently supported.')
        else:
            fp = gzip.open(filename, 'wb')
            pickle.dump(obj, fp, protocol=pickle.HIGHEST_PROTOCOL)
    else:
        if fl.endswith('.feather') or fl.endswith('.fthr'):
            if str(type(obj)) != "<class 'pandas.core.frame.DataFrame'>":
                raise TypeError('.feather format can only be used to save pandas DataFrames')
            import feather
            feather.write_dataframe(obj, filename)
        else:
            fp = open(filename, 'wb')
            pickle.dump(obj, fp, protocol=pickle.HIGHEST_PROTOCOL)


def load_object(filename):
    """Loads data saved with save() (or just normally saved with pickle). Autodetects gzip if filename ends in '.gz'
    Also reads feather files denoted .feather or .fthr.

    # Arguments
        filename (str): String with the relative filename of the pickle/feather to load.
    """
    fl = filename.lower()
    if fl.endswith('.gz'):
        if fl.endswith('.feather.gz') or fl.endswith('.fthr.gz'):
            raise NotImplementedError('Compressed feather is not supported.')
        else:
            fp = gzip.open(filename, 'rb')
            return pickle.load(fp)
    else:
        if fl.endswith('.feather') or fl.endswith('.fthr'):
            import feather
            return feather.read_dataframe(filename)
        else:
            fp = open(filename, 'rb')
            return pickle.load(fp)


def load_object(filename):
    """Loads data saved with save() (or just normally saved with pickle). Autodetects gzip if filename ends in '.gz'
    Also reads feather files denoted .feather or .fthr.

    # Arguments
        filename (str): String with the relative filename of the pickle/feather to load.
    """
    fl = filename.lower()
    if fl.endswith('.gz'):
        if fl.endswith('.feather.gz') or fl.endswith('.fthr.gz'):
            raise NotImplementedError('Compressed feather is not supported.')
        else:
            fp = gzip.open(filename, 'rb')
            return pickle.load(fp)
    else:
        if fl.endswith('.feather') or fl.endswith('.fthr'):
            import feather
            return feather.read_dataframe(filename)
        else:
            fp = open(filename, 'rb')
            return pickle.load(fp)


def save_dict_json(file_path: str, dictionary: dict):
    """
    Saves a dictionary to a JSON file.

    # Arguments
        file_path (string): Path to save JSON file.
        dictionary (dict): Dictionary to be saved.
    """

    if not os.path.exists(os.path.dirname(file_path)):
        os.makedirs(os.path.dirname(file_path))

    with open(file_path, 'w+') as fp:
        json.dump(dictionary, fp, sort_keys=True, indent=4)


def load_dict_json(file_path: str) -> dict:
    """
    Loads a JSON file as a dictionary.

    # Arguments
        file_path (string): Path to the JSON file.

    # Returns
    Dictionary with the data from the JSON file.
    """
    with open(file_path, 'rb') as fp:
        return json.load(fp)


def save_dict_csv(file_path: str, dictionary: dict, delimiter: str = ';'):
    """
    Saves a dictionary to a CSV file.

    # Arguments
        file_path (string): Path to save CSV file.
        dictionary (dict): Dictionary to be saved.
        delimiter (string): Delimiter for CSV (default=;).
    """
    if not os.path.exists(os.path.dirname(file_path)):
        os.makedirs(os.path.dirname(file_path))

    writer = csv.writer(open(file_path, "w+"), delimiter=delimiter)
    for key, value in iteritems(dictionary):
        writer.writerow([key, value])


def load_dict_csv(file_path: str, delimiter: str = ';') -> dict:
    """
    Loads a CSV file as a dictionary.

    # Arguments
        file_path (string): Path to the CSV file.
        delimiter (string): Delimiter for CSV (default=;).

    # Returns
    Dictionary with the data from the CSV file.
    """
    reader = csv.reader(open(file_path, "rb"), delimiter=delimiter)
    return dict(reader)


def folder_size(path: str) -> str:
    """Disk usage of a specified folder."""
    return subprocess.check_output(['du', '-sh', '-B1', path]).split()[0].decode('utf-8')


def is_subdir(path: str, directory: str) -> bool:
    """Checks if a `directory` is the subdirectory for a given `path`."""
    path = os.path.realpath(path)
    directory = os.path.realpath(directory)
    relative = os.path.relpath(path, directory)
    return not (relative == os.pardir or relative.startswith(os.pardir + os.sep))


def remove_folder_content(folder):
    for the_file in os.listdir(folder):
        file_path = os.path.join(folder, the_file)
        if os.path.isfile(file_path):
            os.unlink(file_path)
        elif os.path.isdir(file_path):
            shutil.rmtree(file_path)


def get_last_usage_date(path: str) -> datetime:
    """Returns last usage date for a given file."""
    date = None

    if not os.path.exists(path):
        raise FileNotFoundError("Path does not exist: " + path)

    try:
        date = datetime.fromtimestamp(os.path.getmtime(path))
    except:
        pass

    try:
        compare_date = datetime.fromtimestamp(os.path.getatime(path))
        if date.date() < compare_date.date():
            # compare date is newer
            date = compare_date
    except:
        pass

    try:
        compare_date = datetime.fromtimestamp(os.path.getctime(path))
        if date.date() < compare_date.date():
            # compare date is newer
            date = compare_date
    except:
        pass

    return date


def identify_compression(file_path: str) -> str or None:
    sign_dict = {
        b"\x1f\x8b\x08": "gz",
        b"\x42\x5a\x68": "bz2",
        b"\x50\x4b\x03\x04": "zip",
        b"\x37\x7a\xbc\xaf\x27\x1c": "7z",
        b"\x75\x73\x74\x61\x72": "tar",
        b"\x52\x61\x72\x21\x1a\x07\x00": "rar"
    }

    max_len = max(len(x) for x in sign_dict)
    with open(file_path, "rb") as f:
        file_start = f.read(max_len)
    for magic, filetype in sign_dict.items():
        if file_start.startswith(magic):
            return filetype
    return None
