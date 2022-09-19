# Backup Data

To prevent full data loss, it is important to have periodic backups of the volumes or directories mounted into the following services:

- Minio Storage (`lab-minio`: `/data` directory)
- Mongo Storage (`lab-mongo`: `/data/db` directory)

!!! info "Workspace Backups"
    The `/workspace` directory within all Lab-managed Workspaces is automatically backed up to the Minio storage every day at approx. 4am. However, all files in the `/environment` folder, as well as all files with a size of more than 50MB, will be ignored in the automatic backup. Lab will keep only the last three Workspace backups for every user. In the case of data loss, the last backup - if available - will be automatically restored in the startup process of the Workspace.

We recommend using [Volumerize](https://github.com/blacklabelops/volumerize) to backup the persisted data of the Minio and Mongo Storage to one of the [many supported backends](http://duplicity.nongnu.org/index.html) (e.g. filesystem, ssh, rsync, s3). With Volumerize, it is also possible to schedule periodic backups. Please refer to the [Volumerize documentation](https://github.com/blacklabelops/volumerize) for all features and usage information. In the following two sections, we will show examples on how to use Volumerize to backup persisted data from a Docker-local and Kubernetes Lab instance.

## Example: Docker Local

Backup `lab-minio` and `lab-mongo` volumes every 2 days at 2am into the local folder that the following command is executed from (`$(pwd)`):

``` bash
docker run -d --restart always --name lab-backup \
    -v lab-minio:/source/lab-minio:ro \
    -v lab-mongo:/source/lab-mongo:ro \
    -v lab-backup-cache:/volumerize-cache \
    -v $(pwd):/backup \
    -e "VOLUMERIZE_SOURCE=/source" \
    -e "VOLUMERIZE_TARGET=file:///backup" \
    -e "VOLUMERIZE_DUPLICITY_OPTIONS=--progress" \
    -e "TZ=Europe/Berlin" \
    -e "VOLUMERIZE_JOBBER_TIME=0 0 3 */2 * *" \
    blacklabelops/volumerize:1.5.0
```

## Example: Kubernetes

In a Kubernetes Lab instance, the persisted data is on the manager node's filesystem in the folder specified with `LAB_DATA_ROOT` during the installation process. In the following example, the `LAB_DATA_ROOT` is `/lab/data/` which means that the Minio data is persisted at `/lab/data/lab-minio` and the Mongo data at `/lab/data/lab-mongo` on the file system of the manager node.

The backup is scheduled for every 2 days at 3am as well. Instead of backing up the data on the local filesystem (as shown in the Docker local example) we will use rsync for this example to move the data to a remote machine specified via `rsync://10.12.345.678//lab/backup`. In order to be able to connect, valid key information needs to be provided (`id_rsa` and `known_hosts`) as demonstrated below:

``` bash
docker run -d -restart always --name lab-backup  \
    --name volumerize-backup \
    -v /lab/data/lab-minio:/source/lab-minio:ro \
    -v /lab/data/lab-mongo:/source/lab-mongo:ro \
    -v lab-backup-cache:/volumerize-cache \
    -v /lab/backup/backup_key:/root/.ssh/id_rsa \
    -v /root/.ssh/known_hosts:/root/.ssh/known_hosts \
    -e "VOLUMERIZE_SOURCE=/source" \
    -e "VOLUMERIZE_TARGET=rsync://10.12.345.678//lab/backup" \
    -e "VOLUMERIZE_DUPLICITY_OPTIONS=--progress" \
    -e "TZ=Europe/Berlin" \
    -e "VOLUMERIZE_JOBBER_TIME=0 0 3 */2 * *" \
    blacklabelops/volumerize:1.5.0
```