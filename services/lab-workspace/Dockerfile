

ARG ARG_WORKSPACE_BASE_IMAGE="mltooling/ml-workspace:latest"
FROM $ARG_WORKSPACE_BASE_IMAGE

ARG ARG_WORKSPACE_FLAVOR="lab"
ENV WORKSPACE_FLAVOR=$ARG_WORKSPACE_FLAVOR
# argument needs to be initalized again
ARG ARG_WORKSPACE_BASE_IMAGE="mltooling/ml-workspace:latest"
ENV WORKSPACE_BASE_IMAGE=$ARG_WORKSPACE_BASE_IMAGE

# SSH configuration - is automatically provided to connected runtimes
RUN \
    printf "\nSendEnv LAB_ENDPOINT" | sudo tee -a /root/.ssh/config && \
    printf "\nSendEnv LAB_API_TOKEN" | sudo tee -a /root/.ssh/config && \
    printf "\nAcceptEnv LAB_ENDPOINT"| sudo tee -a /etc/ssh/sshd_config && \
    printf "\nAcceptEnv LAB_API_TOKEN"| sudo tee -a /etc/ssh/sshd_config

# Install our own python libraries and extensions
COPY docker-res/jupyter-plugins $RESOURCES_PATH/jupyter-plugins
COPY docker-res/duplicated-resources/ml-lab-py $RESOURCES_PATH/ml-lab-py
COPY docker-res/duplicated-resources/unified-model $RESOURCES_PATH/unified-model

RUN \
    pip install --no-cache-dir $RESOURCES_PATH/jupyter-plugins/diskstorage-plugin/ && \
    pip install --no-cache-dir $RESOURCES_PATH/ml-lab-py && \
    pip install --no-cache-dir $RESOURCES_PATH/unified-model && \
    # Cleanup
    clean-layer.sh

# Include Tutorials - will be copied to workspace folder on start
COPY docker-res/tutorials $RESOURCES_PATH/tutorials

# Overwrite scripts
COPY docker-res/scripts $RESOURCES_PATH/scripts

# Overwrite & add Labels
ARG ARG_BUILD_DATE="unknown"
ARG ARG_VCS_REF="unknown"
ARG ARG_WORKSPACE_VERSION="latest"

ENV WORKSPACE_VERSION=$ARG_WORKSPACE_VERSION

LABEL \
    "workspace.version"=$WORKSPACE_VERSION \
    "workspace.flavor"=$WORKSPACE_FLAVOR \
    "workspace.baseimage"=$WORKSPACE_BASE_IMAGE \
    "org.opencontainers.image.version"=$WORKSPACE_VERSION \
    "org.opencontainers.image.revision"=$ARG_VCS_REF \
    "org.opencontainers.image.created"=$ARG_BUILD_DATE \ 
    "org.label-schema.version"=$WORKSPACE_VERSION \
    "org.label-schema.vcs-ref"=$ARG_VCS_REF \
    "org.label-schema.build-date"=$ARG_BUILD_DATE

ENV LAB_BACKUP="false"
    