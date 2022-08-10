# Update Lab

Updating the ML Lab landscape is a tricky process, as it consists of a variety of Docker containers. To make it simple, we provide a convenient `docker run` action (`LAB_ACTION=update`) to update a Lab instance to a specific version.

!!! attention "Use Installation Configuration!"
    For the update action, you should use the same configuration as used for the installed Lab instance. Especially, the `JWT_SECRET`, `SERVICES_RUNTIME`, and `SERVICE_SSL_ENABLED` are required to be the same values as used by the installed instance. Refer to the [installation section](../../installation/install-lab/#configuration) for all possible configuration parameters.

To update, figure out the configuration of your installed Lab instance and run:

``` bash
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock --env LAB_ACTION=update lab-service:<NEW_VERSION>
```
