from typing import Dict

from pydantic import BaseModel, Field


class SecretInput(BaseModel):
    """Input data provided by the client when creating a new secret."""

    display_name: str
    metadata: Dict[str, str] = Field(
        {}, example={"username": ""}, description="optional meta data values"
    )
    secret_text: str = Field(description="password or key")
    pass


class SecretUpdate(BaseModel):
    """Update data provided by the client when updating a secret."""

    metadata: Dict[str, str] = Field(
        {}, example={"username": ""}, description="optional meta data values"
    )
    secret_text: str = Field("", description="password or key")
    pass


class SecretMetadata(BaseModel):
    """Non sensitive secret data without the secret text."""

    id: str
    display_name: str
    metadata: Dict[str, str]
    pass


class Secret(SecretMetadata):
    secret_text: str

    pass
