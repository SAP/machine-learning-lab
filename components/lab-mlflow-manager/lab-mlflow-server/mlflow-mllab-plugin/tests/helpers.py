import socket
import subprocess
import time

LOCALHOST = "localhost"


def get_safe_port():
    """Returns an ephemeral port that is very likely to be free to bind to."""
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.bind((LOCALHOST, 0))
    port = sock.getsockname()[1]
    sock.close()
    return port


def launch_artifact_repository_test_server(artifacts_destination, port=5001):
    cmd = [
        "mlflow",
        "server",
        "--artifacts-destination",
        artifacts_destination,
        "--default-artifact-root",
        "mlflow-artifacts:/projects/zohair/services/pylab-p-zohair-s-ml-flow-1c457/access/5001",
        "--serve-artifacts",
        "--port",
        str(port),
    ]

    process = subprocess.Popen(cmd)
    await_server_up_or_die(port)
    return process


def launch_tracking_store_test_server(store_uri, port=5001):
    cmd = [
        "mlflow",
        "server",
        "--backend-store-uri",
        store_uri,
        "--artifacts-destination",
        "./mlruns",
        "--serve-artifacts",
        "--port",
        str(port),
    ]

    process = subprocess.Popen(cmd)
    await_server_up_or_die(port)
    return process


def await_server_up_or_die(port, timeout=60):
    """Waits until the local flask server is listening on the given port."""
    start_time = time.time()
    connected = False
    while not connected and time.time() - start_time < timeout:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(2)
        result = sock.connect_ex((LOCALHOST, port))
        if result == 0:
            connected = True
        else:
            time.sleep(0.5)
    if not connected:
        raise Exception("Failed to connect on %s:%s after %s seconds" %
                        (LOCALHOST, port, timeout))
