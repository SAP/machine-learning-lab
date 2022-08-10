# Uninstall ML Lab

All docker resources of an ML Lab installation are labeled by the used `LAB_NAMESPACE` (default=`lab`). These docker resources related to a ML Lab installation (regardless if it is installed in docker, or kubernetes mode) can be removed via a convenient `docker run` command:

``` bash
docker run --rm --env LAB_ACTION=uninstall -v /var/run/docker.sock:/var/run/docker.sock lab-service:latest
```

All resources started outside of the ML Lab instance, e.g. [Backup Jobs](../backup-data), need to be removed manually. If you have used another `LAB_NAMESPACE` than the default one (`lab`), you need to specify this namespace as well (`--env LAB_NAMESPACE="YOUR_NAMESPACE"`).

!!! warning
    This un-installation process will also remove all volumes and thereby delete all data of your installation.
