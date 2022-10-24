# ML Lab Installation

## Preparation

!!! info "Install Docker"
    ML Lab requires Docker to be installed on your host machine.

### Choose Service Runtime

ML Lab installs and orchestrates various services in the form of Docker container. At the moment, we offer the following installation modes:

- **Docker Local Mode:** Deploys all services on the same machine as ML Lab. Easy to setup and manage, but does not scale.
- **Kubernetes Mode:** Distributes all services across a cluster of nodes via Kubernetes. For more information about Kubernetes, please refer to the [official guide](https://kubernetes.io/docs/home/).

!!! tip
    If you are not sure which mode to use, we recommend the **local mode**.

## Installation

If you have special requirements (e.g. data persistance, ssl, hardware restrictions), please consult the [configuration section](#configuration) before installing ML Lab.

### Configuration

#### Parameters

The container can be configured with following environment variables (`--env`):

<table>
    <tr>
        <th>Variable</th>
        <th>Description</th>
        <th>Default</th>
    </tr>
     <tr>
        <td>LAB_ACTION</td>
        <td>Available actions: install, uninstall, serve, update, update-full</td>
        <td>install</td>
    </tr>
    <tr>
        <td>LAB_PORT</td>
        <td>Main port that the ML Lab instance is accesible from.</td>
        <td>8091</td>
    </tr>
    <tr>
        <td>LAB_BASE_URL</td>
        <td>
            If you deploy ML Lab behind a proxy, you can define a base url. It must not end with a slash. For example, if the web app should be accessible behind <i>/test/app</i> instead of <i>/app</i>, the <i>LAB_BASE_URL</i> would be defined as <i>/test</i>.
        </td>
        <td></td>
    </tr>
    <tr>
        <td>SERVICES_RUNTIME</td>
        <td>Determines the technology used for container orchastration. Currently supported: local, kubernetes</td>
        <td>local</td>
    </tr>
    <tr>
        <td>JWT_SECRET</td>
        <td>The secret used for the authentication layer.</td>
        <td>(required); at least 32 characters long</td>
    </tr>
    <tr>
        <td>SERVICE_SSL_ENABLED</td>
        <td>Set to true to enable ssl encryption (HTTPS support). If no certificate is provided, a self-signed certificate is generated; this certificate expires within a year, so the ML Lab container has to be re-created for fresh certificates.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>LAB_SSL_ROOT</td>
        <td>The path on the host system to the folder containing a custom ssl certificate. The folder must contain a cert.crt and cert.key file. This folder is mounted into the ML Lab container, so to renew the certificate it must be replaced on the host system and the ML Lab container has to be restarted. If the <i>LAB_SSL_ROOT</i> variable is not set, ML Lab will look for a volume named lab_ssl. If no such named volume exists, a self signed certificate will be generated. The <i>LAB_SSL_ROOT</i> variable is only valid for docker local mode as secretes are used on Kubernetes.
        <td>(optional)</td>
    </tr>
    <tr>
        <td>LAB_NAMESPACE</td>
        <td>The namespace used for the ML Lab installation. At the moment, we suggest to not change this value.</td>
        <td>lab</td>
    </tr>
     <tr>
        <td>SERVICES_MEMORY_LIMIT</td>
        <td>The memory limit (in GB) that every ML Lab managed service, including workspaces, is restricted to.</td>
        <td>100</td>
    </tr>
    <tr>
        <td>SERVICES_CPU_LIMIT</td>
        <td>The CPU limit (number of CPUs) that every ML Lab managed service, including workspaces, is restricted to.</td>
        <td>8</td>
    </tr>
    <tr>
        <td>SERVICES_STORAGE_LIMIT</td>
        <td>The storage limit (in GB) that every ML Lab managed service, including workspaces, is restricted to. For Docker-local and Kubernetes custom cluster setup, there is only a soft enforcement in the workspace. In case of the Kubernetes managed cluster setup, a volume with this size is created on the respective infrastructure and mounted into the service pod (check this as this can lead to substantial costs!).</td>
        <td>100</td>
    </tr>
    <tr>
        <td>MAX_CONTAINER_SIZE</td>
        <td>The maximum size a ML Lab managed container is allowed to grow in Gigabytes. If you add data to a container's writeable layer - basically a path where no volume is mounted - the container size grows. If not set, a container can theoretically consume all of the host's storage. For Docker-local, ML Lab contains a REST-method (/containers/shutdown-disk-exceeding) that will remove all non-core containers that exceed this limit (in Docker-local mode, currently only workspaces are removed). In Kubernetes mode, the native functionality of <i>ephemeral-storage limit</i> is used (for all non-core pods). Set to '-1' to disable it.</td>
        <td>100</td>
    </tr>
    <tr>
        <td>LAB_DATA_ROOT</td>
        <td>Basic mount path where all data is stored.</td>
        <td>(optional)</td>
    </tr>
    <tr>
        <td>LAB_DATA_WORKSPACE_ROOT</td>
        <td>Basic mount path where all workspace data is stored. Overwrites the LAB_DATA_ROOT variable for workspace mount.</td>
        <td>(optional)</td>
    </tr>
     <tr>
        <td>LAB_DEBUG</td>
        <td>If true, ML Lab will expose all ports and print out debug logs.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>LAB_SSH_ENABLED</td>
        <td>Enable ssh jumphost if ML Lab should publish port 22 on startup and start an SSH server. The jumphost functionality can be used so that users can ssh into their own workspace. SSHing into the workspace container itself is not possible.</td>
        <td>true</td>
    </tr>
    <tr>
        <td>ALLOW_SELF_REGISTRATIONS</td>
        <td>If true, ML Lab will allow user self registrations via register dialog or automatically create users if external OIDC authentication is enabled.</td>
        <td>true</td>
    </tr>
    <tr>
        <td>WORKSPACE_BACKUP</td>
        <td>If true, workspaces will be automatically backuped every day to the ML Lab Storage and restored if necessary.</td>
        <td>false</td>
    </tr>
   <tr>
        <td>WORKSPACE_IMAGE</td>
        <td>Docker image used for user workspaces. Should be build on top of the ml-workspace base image.</td>
        <td>(optional)</td>
    </tr>
    <tr>
        <td>LAB_MANAGED_KUBERNETES</td>
        <td>Specifies whether it is running on a managed Kubernetes cluster instance. For more information, please have a look at the Section about Kubernetes managed cluster setup.</td>
        <td>false</td>
    </tr>
     <tr>
        <td>LAB_STORAGE_CLASS</td>
        <td>Only relevant for Kubernetes managed cluster setup. The storage class name that is used by the persistent volume claims for issuing the volumes mounted into the pods of Minio, Mongo, and the Workspaces.</td>
        <td>lab-storageclass</td>
    </tr>
    <tr>
        <td>LAB_PVC_MINIO_STORAGE_LIMIT</td>
        <td>Only relevant for Kubernetes managed cluster setup. It defines the size of the volume created and mounted into the Minio pod in GB. Minio is the storage for uploaded files such as datasets and models.</td>
        <td>100</td>
    </tr>
    <tr>
        <td>LAB_PVC_MONGO_STORAGE_LIMIT</td>
        <td>Only relevant for Kubernetes managed cluster setup. It defines the size of the volume created and mounted into the Mongo pod in GB. Mongo contains the data for experiments and users.</td>
        <td>5</td>
    </tr>
    <tr>
        <td>LAB_IMAGE_REGISTRY</td>
        <td>
            The registry prefix from where the images <i>lab-service</i> and <i>lab-model-service</i> are loaded. If you, for example, deploy it in Azure you might not have access to the internal Artifactory registry. Make sure to push the images to the defined registry. If you don't use the one-click model-deployment feature, you don't have to push the <i>lab-model-service</i> image.
        </td>
        <td>Default DockerHub registry</td>
    </tr>
    <tr>
        <td>LAB_EXTERNAL_OIDC_AUTH_URL</td>
        <td>The authorization endpoint used for external OIDC authentication. The client will be redirected to this page to authenticate with the external authentication provider. For detailed information see: <a href="../../administration/external-oidc-authentication">External OIDC Authentication</a></td>
        <td>(optional)</td>
    </tr>
    <tr>
        <td>LAB_EXTERNAL_OIDC_TOKEN_URL</td>
        <td>The token endpoint used for external OIDC authentication. It will be used by the backend to obtain the OIDC identity token in exchange for an authorization code. For detailed information see: <a href="../../administration/external-oidc-authentication">External OIDC Authentication</a></td>
        <td>(optional)</td>
    </tr>
    <tr>
        <td>LAB_EXTERNAL_OIDC_CLIENT_ID</td>
        <td>The OAuth 2.0 client identifier used for external OIDC authentication. For detailed information see: <a href="../../administration/external-oidc-authentication">External OIDC Authentication</a></td>
        <td>(optional)</td>
    </tr>
    <tr>
        <td>LAB_EXTERNAL_OIDC_CLIENT_SECRET</td>
        <td>The OAuth 2.0 client secret used for external OIDC authentication. For detailed information see: <a href="../../administration/external-oidc-authentication">External OIDC Authentication</a></td>
        <td>(optional)</td>
    </tr>
</table>

#### Proxy

If a proxy is required, you can pass the proxy configuration via the `http_proxy` and `no_proxy` environment variables. For example: `--env http_proxy=http://myproxy:1234`

### Install with Docker Local Mode

*Note: The install commands here are currently based on the scenario where you built the Docker images locally yourself. For seeing how to use ready images, please check the Readme of the GitHub repository [here](https://github.com/SAP/machine-learning-lab#getting-started)*

To start  ML Lab in a single-host (local) deployment execute:

``` bash
docker run --rm --env LAB_PORT=8091 -v /var/run/docker.sock:/var/run/docker.sock --env LAB_ACTION=install lab-service:latest
```

After the installation is finished (after several minutes depending on internet speed), visit `http://<HOSTIP>:8091` and login with `admin:admin`.

**Enable SSL**

For SSL setup, create the certificate (files must be called cert.crt and cert.key) and specify their path on the host machine via the `LAB_SSL_ROOT` environment variable. Additionally you need to set `SERVICE_SSL_ENABLED` to true:
``` bash
docker run --rm --env LAB_PORT=8091 \
    --env SERVICE_SSL_ENABLED=true \
    --env LAB_SSL_ROOT=/workspace/ssl \
    -v /var/run/docker.sock:/var/run/docker.sock \
    lab-service:latest
```

Alternatively, instead of specifying `LAB_SSL_ROOT`, the certificate can be provided in a docker volume named `lab_ssl`.
If you don't provide a custom certificate, a self-signed certificate is generated and used. Be aware that applications such as your browser might not trust the certificate.

### Install with Kubernetes Mode (Own Cluster)

These steps are for a custom cluster that does not use a hyperscaler / automated infrastructure in the background. If you have a managed cluster (such as AWS EKS), then please follow the steps in the next Section.

!!! info "Install Kubernetes"
    For Kubernetes Mode, please make sure that your server/cluster is installed with Docker and Kubernetes (Version >=1.11). You can find more information about Kubernetes installation [here](./installation/install-kubernetes/linux).

**Preparation**

For the Kubernetes deployment, a few minor steps to prepare the host have to be made such as creating a directory where the data is stored etc. since Kubernetes does not have the concept of Docker's named volumes. The Kubernetes version of the ML Lab needs the kube config of the cluster as well as the access to the Docker socket.

```bash
# label the master node with role=master, as we use it in ML Lab
kubectl label nodes <name-of-master-node> role=master

# install nfs-common for mounting nfs-service in workspace
apt-get install nfs-common

# create the data root directory that will be used by Kubernetes
# Default is `/workspace/lab/<namespace>/data`
# Hence, when creating a Lab for default namespace 'lab':
mkdir -p /workspace/lab/lab/data
```

**Installation**

``` bash
# On Mac: in ~/.kube/config for 'server' field replace 'localhost' with 'docker.for.mac.localhost'
docker run --rm --env LAB_PORT=30001 \
    -v /root/.kube/config:/root/.kube/config \
    -v /var/run/docker.sock:/var/run/docker.sock \
    --env SERVICES_RUNTIME=k8s \
    --env LAB_DATA_ROOT=/workspace/lab/stulabdio/data \
    lab-service:latest
```

After installation is finished (after several minutes depending on intranet speed), visit `http://<HOSTIP>:30001` and login with `admin:admin`.

!!! tip
    When a container, e.g. the Workspace container, is launched on a node the first time, it's Docker image has to be pulled to that node. This can take some time in which the user sees a loading screen. To prevent this, you can pull the images manually for each node beforehand.

**Enable SSL**

For SSL setup, create the certificates and mount them into the container's directory at `/resources/ssl` (`-v /workspace/ssl:/resources/ssl:ro`) and start with `--env SERVICE_SSL_ENABLED=true`:

``` bash
docker run --rm --env LAB_PORT=30001 \
    -v /root/.kube/config:/root/.kube/config \
    -v /var/run/docker.sock:/var/run/docker.sock \
    --env SERVICES_RUNTIME=k8s \
    --env LAB_DATA_ROOT=/workspace/lab/lab/data \
    --env SERVICE_SSL_ENABLED=true \
    -v /workspace/ssl:/resources/ssl:ro \
    lab-service:latest
```

The files have to be named `cert.crt` and `cert.key` .

### Install with Kubernetes (Managed Cluster such as AWS EKS)

TODO

#### Difference to Custom Cluster Setup

In the Managed Cluster scenario, the biggest difference is that you don't have to take care of the volumes as here we leverage persistent volume (claims) to automatically issue volumes in the background. Hence, no need for the NFS service here.  
In this setup mode, ML Lab accesses the cluster via its ServiceAccount permissions and *not* via a mounted kube-config.

!!! warn "Costs"
    As volumes are created automatically on the infrastructure based on the Kubernetes persistent volume claims, make sure to have an eye on your costs and set the size for the volume via the respective environment variables accordingly. Even when deleting the Kuberentes PersistentVolume and PersistentVolumeClaim resources, the actual volumes might still exist on the cloud provider and have to be deleted manually.  

## After Installation

Please makes sure to change the `admin` password (`User-menu -> Change Password`) after the installation was successful. We also recommend to activate a data backup job as described [here](../../administration/backup-data/). Check out the administration section in this documentation for more information on how to [update](../../administration/update-lab/)/[uninstall](../../administration/uninstall-lab/) Lab, or [manage services](../../administration/manage-services/).
