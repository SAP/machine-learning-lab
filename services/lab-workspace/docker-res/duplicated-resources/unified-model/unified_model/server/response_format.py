import array
import typing

import numpy as np
import pandas as pd
import six
import logging
from flask import json

log = logging.getLogger(__name__)


class Metadata:
    STATUS = "status"
    QUERY = "query"
    MESSAGE = "message"
    DATA_TYPE = "type"
    EXECUTION_TIME = "time"


class Error:
    CODE = "code"
    TYPE = "type"
    MESSAGE = "message"
    DESCRIPTION = "description"


class UnifiedFormat:
    DATA = "data"
    METADATA = "metadata"
    ERRORS = "errors"


def get_response(data=None, code: int = 200, message: str = None, execution_time=None, metadata=None) -> dict:
    response = {UnifiedFormat.DATA: {},
                UnifiedFormat.METADATA: {}}

    data_type, data = encode_response(data)
    response[UnifiedFormat.DATA] = data

    meta = response[UnifiedFormat.METADATA]
    meta[Metadata.DATA_TYPE] = data_type
    meta[Metadata.STATUS] = code

    if message:
        meta[Metadata.MESSAGE] = message

    if execution_time:
        meta[Metadata.EXECUTION_TIME] = int(round(execution_time * 1000))

    if metadata:
        meta.update(metadata)

    return response


def get_error_response(code: int = 500, message: str = None, description: str = None, execution_time=None,
                       metadata: dict = None) -> dict:
    response = get_response(code=code, execution_time=execution_time, metadata=metadata)
    response[UnifiedFormat.ERRORS] = {}
    errors = response[UnifiedFormat.ERRORS]
    errors[Error.CODE] = code
    if message:
        errors[Error.MESSAGE] = message
    if description:
        errors[Error.DESCRIPTION] = description

    return response


def _get_as_json(o):
    try:
        return o.__dict__
    except:
        return "Failed to resolve object of type: " + type(o).__name__


def to_json(response: dict) -> str:
    # TODO fix info
    return json.dumps(response, default=lambda o: _get_as_json(o),
                      sort_keys=True, indent=4, separators=(',', ': '))


def from_json(json_str) -> dict:
    response = json.loads(json_str)
    # TODO decode data
    if UnifiedFormat.DATA in response:
        response[UnifiedFormat.DATA] = decode_response(response[UnifiedFormat.DATA],
                                                       response[UnifiedFormat.METADATA][Metadata.DATA_TYPE])
    return response


class DataTypes:
    LIST_SUFFIX = "-list"
    FLOAT = "float"
    INTEGER = "integer"
    BOOLEAN = "boolean"
    DATAFRAME = "dataframe"
    BYTES = "bytes"
    STRING = "string"
    DICT = "dict"
    EMPTY = "empty"


def encode_response(data):
    if data is None:
        return DataTypes.EMPTY, {}

    if isinstance(data, six.string_types):
        # string like objects
        # .decode("utf-8")
        return DataTypes.STRING, data

    if isinstance(data, pd.DataFrame):
        # pandas dataframe
        return DataTypes.DATAFRAME, data.to_dict(orient="records")

    if isinstance(data, np.integer) or isinstance(data, int):
        # integer item
        return DataTypes.INTEGER, int(data)

    if isinstance(data, np.floating) or isinstance(data, float):
        # float item
        return DataTypes.FLOAT, float(data)  # data.item()

    if isinstance(data, np.bool) or isinstance(data, bool):
        # boolean item
        return DataTypes.BOOLEAN, bool(data)

    if isinstance(data, dict):
        # dict item
        return DataTypes.DICT, data

    if isinstance(data, np.ndarray):
        data = data.tolist()

    if type(data) is list:
        if homogeneous_type(data) and len(data) > 0:
            data_list = []
            data_type = None
            for item in data:
                encoded_data = encode_response(item)
                if not data_type:
                    data_type = encoded_data[0]
                data_list.append(encoded_data[1])
            return data_type + DataTypes.LIST_SUFFIX, data_list

    elif isinstance(data, typing.ByteString) or isinstance(data, array.array):
        # byte like objects
        return DataTypes.BYTES, data.encode('base64')

    # unknown object type, might fail
    return type(data).__name__, data


def decode_response(data, data_type):
    # TODO implement decode
    return data


def homogeneous_type(seq):
    iseq = iter(seq)
    first_type = type(next(iseq))
    return first_type if all((type(x) is first_type) for x in iseq) else False
