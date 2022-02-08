from pydantic import BaseModel


class SecretInput(BaseModel):
    """Input data provided by the client when creating a new secret."""

    # secret_text
    # display_name
    # metadata
    pass


class SecretUpdate(BaseModel):
    """Update data provided by the client when updating a secret."""

    # secret_text
    # metadata
    pass


class SecretMetadata(BaseModel):
    """Non sensitive secret data without the secret text."""

    # id
    # display_name
    # metadata
    pass


class Secret(SecretMetadata):
    """Secret data including the sensitive secret text."""

    # secret_text
    pass
