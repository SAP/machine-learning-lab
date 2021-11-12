from contaxy.schema import OAuthTokenIntrospection

import re

_SIMPLIFY_STRING_PATTERN = re.compile(r"[^a-zA-Z0-9-]")

def check_token_information(token_information: OAuthTokenIntrospection, project: str):
    if not token_information.active:
        raise ConnectionError("Token is not longer not active")
    if token_information.scope.split('#')[0].split('/')[-1].lower() != project.lower():
        raise ConnectionError(f"The token used doesn't have access to project {project}")
    # TODO check expiration date
    # TODO check needed scope is given #admin or #write?

#Text utils

def safe_str(obj) -> str:
    try:
        return str(obj)
    except UnicodeEncodeError:
        return obj.encode('ascii', 'ignore').decode('ascii')


def simplify(text) -> str:
    text = safe_str(text)
    return _SIMPLIFY_STRING_PATTERN.sub("-", text.strip()).lower()
