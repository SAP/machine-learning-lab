from subprocess import call, check_output, check_call, CalledProcessError
from notebook.base.handlers import IPythonHandler
from notebook.utils import url_path_join
import itertools, urllib
import json
import os
import tornado
import subprocess
from datetime import datetime
import logging
import threading
import time 

from tornado.web import RequestHandler
from pathlib import Path
from urllib.parse import unquote
import socket
import sys

# The instance of the notebook server will be stored in web_app after the plugin is loaded.
web_app = None

log = logging.getLogger(__name__)

WORKSPACE_PATH = '/workspace'
WORKSPACE_CONFIG_FOLDER = os.path.join(WORKSPACE_PATH, ".workspace")

# -------------- HANDLER -------------------------
class HelloWorldHandler(IPythonHandler):

    def data_received(self, chunk):
        pass

    def get(self):
        result = self.request.protocol + "://" + self.request.host
        if 'base_url' in self.application.settings:
            result = result + "   " + self.application.settings['base_url']
        self.finish(result)

def get_last_usage_date(path):
    date = None

    if not os.path.exists(path):
        log.info("Path does not exist: " + path)
        return date

    try:
        date = datetime.fromtimestamp(os.path.getmtime(path))
    except:
        pass

    try:
        compare_date = datetime.fromtimestamp(os.path.getatime(path))
        if date.date() < compare_date.date():
            # compare date is newer
            date = compare_date
    except:
        pass

    try:
        compare_date = datetime.fromtimestamp(os.path.getctime(path))
        if date.date() < compare_date.date():
            # compare date is newer
            date = compare_date
    except:
        pass

    return date

def update_workspace_metadata():
    size_in_kb = int(subprocess.check_output(['du', '-s', WORKSPACE_PATH]).split()[0].decode('utf-8'))
    workspace_metadata = {
        "update_timestamp": str(datetime.now()),
        "folder_size_in_kb": size_in_kb
    }
    
    if not os.path.exists(WORKSPACE_CONFIG_FOLDER):
        os.makedirs(WORKSPACE_CONFIG_FOLDER)
    
    with open(os.path.join(WORKSPACE_CONFIG_FOLDER, "metadata.json"), 'w') as file:
        json.dump(workspace_metadata, file, sort_keys=True, indent=4)
    
def cleanup_folder(folder_path: str, max_file_size_mb: int = 50, last_file_usage: int = 3,
                   replace_with_info: bool = True, excluded_folders: list = None):
    """
    Cleanup folder to reduce disk space usage.
    # Arguments
        folder_path (str): Folder that should be cleaned.
        max_file_size_mb (int): Max size of files in MB that should be deleted. Default: 50.
        replace_with_info (bool): Replace removed files with `.removed.txt` files with file removal reason. Default: True.
        last_file_usage (int): Number of days a file wasn't used to allow the file to be removed. Default: 3.
        excluded_folders (list[str]): List of folders to exclude from removal (optional)
    """
    total_cleaned_up_mb = 0
    removed_files = 0

    for dirname, subdirs, files in os.walk(folder_path):
        if excluded_folders:
            for excluded_folder in excluded_folders:
                if excluded_folder in subdirs:
                    log.debug("Ignoring folder because of name: " + excluded_folder)
                    subdirs.remove(excluded_folder)
        for filename in files:
            file_path = os.path.join(dirname, filename)

            file_size_mb = int(os.path.getsize(file_path) / (1024.0 * 1024.0))
            if max_file_size_mb and max_file_size_mb > file_size_mb:
                # File will not be deleted since it is less than the max size
                continue

            last_file_usage_days = None
            if get_last_usage_date(file_path):
                last_file_usage_days = (datetime.now() - get_last_usage_date(file_path)).days

            if last_file_usage_days and last_file_usage_days <= last_file_usage:
                continue

            current_date_str = datetime.now().strftime("%B %d, %Y")
            removal_reason = "File has been removed during folder cleaning (" + folder_path + ") on " + current_date_str + ". "
            if file_size_mb and max_file_size_mb:
                removal_reason += "The file size was " + str(file_size_mb) + " MB (max " + str(max_file_size_mb) + "). "

            if last_file_usage_days and last_file_usage:
                removal_reason += "The last usage was " + str(last_file_usage_days) + " days ago (max " + str(
                    last_file_usage) + "). "

            log.info(filename + ": " + removal_reason)

            # Remove file
            try:
                os.remove(file_path)

                if replace_with_info:
                    with open(file_path + ".removed.txt", "w") as file:
                        file.write(removal_reason)

                if file_size_mb:
                    total_cleaned_up_mb += file_size_mb

                removed_files += 1

            except Exception as e:
                log.info("Failed to remove file: " + file_path, e)

    # check diskspace and update workspace metadata
    update_workspace_metadata()
    log.info("Finished cleaning. Removed " + str(removed_files) + " files with a total disk space of " + str(total_cleaned_up_mb) + " MB.")

def get_workspace_size():
    # read workspace size from metadata file
    size_in_kb = 0
    metadata_file_path = os.path.join(WORKSPACE_CONFIG_FOLDER , "metadata.json")
    if os.path.isfile(metadata_file_path):
        try:
            with open(metadata_file_path, 'rb') as file:
                workspace_metadata = json.load(file)
                size_in_kb = int(workspace_metadata["folder_size_in_kb"])
        except:
            pass
    # return 0 as fallback
    return size_in_kb / 1024 / 1024

def get_inactive_days():
    # read inactive days from metadata timestamp (update when user is actively using the workspace)
    metadata_file_path = os.path.join(WORKSPACE_CONFIG_FOLDER , "metadata.json")
    if os.path.isfile(metadata_file_path):
        try:
            with open(metadata_file_path, 'rb') as file:
                workspace_metadata = json.load(file)
                update_timestamp_str = workspace_metadata["update_timestamp"]
                
                if not update_timestamp_str:
                    return 0
                updated_date = datetime.strptime(update_timestamp_str, '%Y-%m-%d %H:%M:%S.%f')
                inactive_days = (datetime.now() - updated_date).days
                return inactive_days
        except:
            return 0
    # return 0 as fallback
    return 0

class StorageCheckHandler(IPythonHandler):

    def post(self) -> None:
        """
            POST: returns size of passed directory
        """
        
        # print('------------- Disk storage checker entered-------------')
        env_storage_limit = "WORKSPACE_STORAGE_LIMIT"

        try:
            result = json.loads(self.request.body.decode('utf-8'))
            max_disk_storage_gb = os.environ.get(env_storage_limit)
            #max_disk_storage_gb = 0

            if max_disk_storage_gb != None:
                max_disk_storage_gb = int(max_disk_storage_gb)

                # run update in background -> somtimes it might need to much time to run
                thread = threading.Thread(target=update_workspace_metadata)
                thread.daemon = True
                thread.start()

                size_in_gb = get_workspace_size()

                if size_in_gb > max_disk_storage_gb:
                    # sleep 50 ms -> metadata file should have been updated, otherwise use old metadata
                    time.sleep(0.05)
                    size_in_gb = get_workspace_size()

                if size_in_gb > (max_disk_storage_gb * 1.5):
                    # Automatic cleanup
                    try:
                        from lab_client import Environment
                        Environment().cleanup()
                    except Exception as ex:
                        log.info("Failed to cleanup enviornment", ex)
                        # TODO: Do not do a full workspace cleanup -> bad side effects
                        # Fallback - clean full workspace folder
                        # cleanup_folder(WORKSPACE_PATH)
                
                number_shortened = size_in_gb
                if "." in str(size_in_gb):  # quick check if it is decimal
                    number_shortened = str(size_in_gb).split(".")[0] + '.' + str(size_in_gb).split(".")[1][:2]

                result["workspaceSize"] = number_shortened
                result["restrictedSize"] = max_disk_storage_gb 
                result["status"] = 1

                if (size_in_gb > max_disk_storage_gb):
                    print(
                        "You have exceeded the limit of available disk storage assigned to your workspace. Please clean up.")
                    result["status"] = 0

            self.finish(json.dumps(result))

        except Exception as e:
            result['status'] = 3
            print("No Environment variable set for", env_storage_limit)
            self.finish(json.dumps(result))

class StorageCleanupHandler(IPythonHandler):

    def put(self) -> None:
        try:
            from lab_client import Environment
            Environment().cleanup()
        except:
            # Fallback - clean full workspace folder
            cleanup_folder(WORKSPACE_PATH)

def load_jupyter_server_extension(nb_server_app) -> None:
    """
    registers all handlers as a REST interface
    :param nb_server_app:
    :return:
    """
    global web_app

    import logging
    logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')

    web_app = nb_server_app.web_app
    host_pattern = '.*$'
    #print('baseurl', web_app.settings['base_url'])
    web_app.add_handlers(host_pattern, [(url_path_join(web_app.settings['base_url'], '/storage/check'), StorageCheckHandler)])
    web_app.add_handlers(host_pattern, [(url_path_join(web_app.settings['base_url'], '/storage/cleanup'), StorageCleanupHandler)])
    # print('Handler added to REST Interface.')

    # sys.exit("failed to open file: %s" % (str(e)))
    # logfile.close()

    nb_server_app.log.info('Extension jupyter-disk storage checker loaded successfully.')


# Test routine. Can be invoked manually
if __name__ == "__main__":
    application = tornado.web.Application([
        (r'/test', HelloWorldHandler)
        # ,
        # (r'/gitlist', StorageCheckHandler),
    ])

    application.listen(555)
    tornado.ioloop.IOLoop.current().start()
