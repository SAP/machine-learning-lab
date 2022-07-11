from typing import List, Union

from pydantic import BaseSettings, Field, validator


class SecretStoreSettings(BaseSettings):
    # Settings passed by contaxy
  
    # Settings that can be configured by the user when creating a workspace
    # Should contain a list of allowed values either as json or comma separated list
    SECRETSTORE_VAULT_URL: str = 'http://localhost:8200/'
    SECRETSTORE_VAULT_TOKEN: str = 'myroot'
    SECRETSTORE_USING_VAULT: bool = False

settings = SecretStoreSettings()
