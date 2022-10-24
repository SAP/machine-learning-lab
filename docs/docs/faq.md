# FAQ

??? question "I have problems with my containers. What should I do?"
    The range of issues can be big. A good first thing would be to have a look into the container logs, either via the [Service Administration Dashboard](../administration/manage-services/) (accessible in the web app for admin users) or via command line on the host system. It might also help to just restart you server.

    For debugging you should remember that ML Lab is an ensemble of various containers, whereas all traffic must pass through the ML Lab backend container. All logs are printed to stdout of the container, so you can see the logs via `docker logs lab-backend`. This should give you already a good hint about the problem. When the error happens with certain functionalities, for example accessing a tool in the workspace, also have a look at the respective workspace container logs.

    More logs are shown if you start ML Lab with the environment variable `LAB_DEBUG=true` (e.g. in Kubernetes-mode, you will see the Kubernetes client requests & responses).
  
??? question "A user has a problem with their workspace"
    An ML Lab admin is able to access each user's workspace via the url `/workspace/id/{userid}`. This allows you to enter a user's workspace and perform debugging there.

??? question "What if a user reports "Cannot save file" error?"
    That error has likely to do something with the filesystem and the lack of available storage. Go to the host where the data is stored and check the available storage via `df -h`. 
    Afterwards, try to dig deeper with the `du -sh /some/path/*` (sum the size of the directories) and/or the `docker ps -sa` (show the sizes of the containers) commands.

??? question "What to do about unreachable workspaces?"
    Sometimes we observed that a workspace cannot be reached. The logs hint into the direction that not all processes inside the container might have started. So, use the ML Lab's `/reset` API to remove and recreate the container. If something does not work in Kubernetes, check whether the Deployment and the Service resource belonging to a Workspace still exist, and remove them manually. Then execute the `/reset` API method again or reload the webapp page  to trigger recreation.  
    Please not that the reset method removes the container and re-creates it. It is not the same as restarting. Everything not stored in the workspace's `/workspace` path will be reset to the workspace image state (including installed libraries etc.).

??? question "What do I have to know about Data Storage?"
    The ML Lab container does not persist data, but the Minio, Mongo, and workspace containers.
    In Docker-local, by default, for all of them named Docker volumes are created and mounted. You can pass a host path instead via the `HOST_ROOT_DATA_MOUNT_PATH` environment variable. If this is set, also the mounts for workspaces have the same host path root; this can be overridden via `LAB_DATA_WORKSPACE_ROOT`.

    In managed Kubernetes, a PersistentVolume is created for Minio, Mongo, and the workspace containers. The amount of storage available has to be defined during ML Lab startup via an environment variable.

    In manual Kubernetes, the node has to be tainted where the Minio, Mongo, and NFS pods always start. They always start on the same node so that they can mount directories from the host file system as in Docker-local. The only difference is that workspaces do not mount the host file system directly but they mount a subdirectory from the started NFS pod.
    
    Everything that a user stores in the workspace container under the `/workspace` directory is persisted on the file system. Everything else is persisted within the container and also consumes disk storage. However, it is deleted when the container is removed; a Docker restart is not enough (ML Lab has a /reset Endpoint for triggering a removal & recreation)!

??? question "How to create new users (in case self-registration is disabled)?"
    When ML Lab was started via the environment flag `ALLOW_SELF_REGISTRATIONS=false`, only the admin user can login. Via the REST API (e.g. by using the explorer), the admin can create new users. The endpoint is `POST /api/auth/users`. Note that the JWT secret which was used for the ML Lab deployment has to be passed here.

??? question "Does ML Lab capture any metrics?"
    The ML Lab webapp has a management dashboard at `/app/#/management` containing some metrics. However, the underlying implementation requires a multiple load of the page until all metrics are up to date: load the page, wait a little bit, and reload it again - if needed, repeat a few times.

??? question "How to clean up ML Lab?"
    The `DELETE /api/auth/users/{user}` endpoint removes a user from Mongo, its workspace, and its private Minio bucket. Though, it does not remove the persisted volume previously mounted into the user's /workspace ; this has to be done manually on the host. If ML Lab was started with the `$LAB_DATA_ROOT` or `$LAB_DATA_WORKSPACE_ROOT` variables, no Docker-volumes are used but an absolute host path that has to be cleaned.
    The `DELETE /api/projects/{projectName}` endpoint deletes a project, all belonging experiments and the data from Minio.

??? question "I cannot login to ML Lab"
    If you get an error message like "Request has been terminated" on the login page, this is an indicator that you might have network issues such as being in the wrong network, e.g. when a VPN would be required.
 