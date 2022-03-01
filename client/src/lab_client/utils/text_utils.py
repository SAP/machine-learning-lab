import re

_SIMPLIFY_STRING_PATTERN = re.compile(r"[^a-zA-Z0-9-]")


def safe_str(obj) -> str:
    try:
        return str(obj)
    except UnicodeEncodeError:
        return obj.encode("ascii", "ignore").decode("ascii")


def simplify(text) -> str:
    text = safe_str(text)
    return _SIMPLIFY_STRING_PATTERN.sub("-", text.strip()).lower()
