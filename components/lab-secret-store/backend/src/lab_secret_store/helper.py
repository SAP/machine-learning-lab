import base64

from cryptography.fernet import Fernet


def display_name_to_id(name: str) -> str:
    return name.replace(" ", "-").lower()


def get_key(keyfile: str = "key.txt") -> bytes:
    try:

        with open(keyfile) as file:
            key = file.readline()
        key_bytes = key.encode()
        return key_bytes
    except IOError:
        with open(keyfile, "w") as file:
            key_bytes = Fernet.generate_key()
            file.write(key_bytes.decode())
        return key_bytes


key = get_key()


def decrypt(val: str, key: bytes = key) -> str:
    fernet = Fernet(key)
    val_encryped_bytes = base64.b64decode(val.encode())
    return fernet.decrypt(val_encryped_bytes).decode()


def encrypt(val: str, key: bytes = key) -> str:
    fernet = Fernet(key)
    val_bytes = val.encode()
    val_encryped_bytes = fernet.encrypt(val_bytes)
    base64_encryped_bytes = base64.b64encode(val_encryped_bytes)
    return base64_encryped_bytes.decode()
