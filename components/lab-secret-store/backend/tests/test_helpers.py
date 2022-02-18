import os

from cryptography.fernet import Fernet

from lab_secret_store.helper import decrypt, encrypt, get_key


def test_get_key():
    try:
        os.remove("test-key.txt")
    except FileNotFoundError:
        pass
    key = get_key("test-key.txt")
    key2 = get_key("test-key.txt")
    assert key == key2
    Fernet(key)


def test_encrypt_decrypt():
    key = get_key("test-key.txt")
    message = "test"
    encryped = encrypt(message, key)
    assert decrypt(encryped, key) == message
