import os
import re

import requests
import tqdm as tqdm
from loguru import logger

from lab_client.utils.request_utils import url2filename


def download_file(
    url: str, folder_path: str, file_name: str = None, force_download: bool = False
) -> str and bool:
    # NOTE the stream=True parameter below

    with requests.get(url, stream=True, allow_redirects=True) as r:
        content_type = r.headers.get("Content-Type")
        if content_type and "html" in content_type.lower():
            logger.warning(
                "The url is pointing to an HTML page. Are you sure you want to download this as file?"
            )
        try:
            total_length = int(r.headers.get("Content-Disposition").split("size=")[1])
        except:
            try:
                total_length = int(r.headers.get("Content-Length"))
            except:
                logger.warning("Failed to figure out size of file.")
                total_length = 0

        if not file_name:
            # if file name is not provided use filename from url
            file_name = url2filename(url)  # url.split('/')[-1]
            try:
                # Try to use filename from content disposition
                file_name = re.findall(
                    "filename=(.+)", r.headers.get("Content-Disposition")
                )[0]
            except:
                pass

        file_path = os.path.join(folder_path, file_name)
        if not force_download and os.path.isfile(file_path):
            if total_length == os.path.getsize(file_path):
                logger.info(
                    "File "
                    + file_name
                    + " already exists with same size and will not be downloaded."
                )
                # file already exists and has same size -> do not download
                return file_path, False

        if not os.path.exists(os.path.dirname(file_path)):
            os.makedirs(os.path.dirname(file_path))

        with open(file_path, "wb") as f:
            pbar = tqdm.tqdm(
                total=total_length,
                initial=0,
                mininterval=0.3,
                unit="B",
                unit_scale=True,
                desc="Downloading " + str(file_name),
                file=sys.stdout,
            )
            for chunk in r.iter_content(chunk_size=8192):
                if chunk:  # filter out keep-alive new chunks
                    pbar.update(len(chunk))
                    f.write(chunk)
            pbar.close()
        return file_path, True
