#!/bin/bash

# Stops script execution if a command has an error
set -e

# set default build args if not provided
export SERVICE_HOST="$_HOST_IP"
if [ -z "$INPUT_BUILD_ARGS" ]; then
    INPUT_BUILD_ARGS="--check --make --test"
fi

BUILD_SECRETS=""

if [ -n "$GITHUB_TOKEN" ]; then
    # Use the github token to authenticate the git interaction (see this Stackoverflow answer: https://stackoverflow.com/a/57229018/5379273)
    git config --global url."https://api:$GITHUB_TOKEN@github.com/".insteadOf "https://github.com/"
    git config --global url."https://ssh:$GITHUB_TOKEN@github.com/".insteadOf "ssh://git@github.com/"
    git config --global url."https://git:$GITHUB_TOKEN@github.com/".insteadOf "git@github.com:"

    BUILD_SECRETS="$BUILD_SECRETS --github-token=$GITHUB_TOKEN"
fi

if [ -n "$INPUT_CONTAINER_REGISTRY_USERNAME" ] && [ -n "$INPUT_CONTAINER_REGISTRY_PASSWORD" ]; then
    docker login $INPUT_CONTAINER_REGISTRY_URL -u "$INPUT_CONTAINER_REGISTRY_USERNAME" -p "$INPUT_CONTAINER_REGISTRY_PASSWORD"
    BUILD_SECRETS="$BUILD_SECRETS --container-registry-url=$INPUT_CONTAINER_REGISTRY_URL"
    BUILD_SECRETS="$BUILD_SECRETS --container-registry-username=$INPUT_CONTAINER_REGISTRY_USERNAME"
    BUILD_SECRETS="$BUILD_SECRETS --container-registry-password=$INPUT_CONTAINER_REGISTRY_PASSWORD"
fi

# Navigate to working directory, if provided
if [ -n "$INPUT_WORKING_DIRECTORY" ]; then
    cd $INPUT_WORKING_DIRECTORY
else
    cd $GITHUB_WORKSPACE
fi

if [ -n "$INPUT_PYPI_TOKEN" ]; then
    BUILD_SECRETS="$BUILD_SECRETS --pypi-token=$INPUT_PYPI_TOKEN"
fi

if [ -n "$INPUT_PYPI_REPOSITORY" ]; then
    BUILD_SECRETS="$BUILD_SECRETS --pypi-repository=$INPUT_PYPI_REPOSITORY"
fi

if [[ $INPUT_BUILD_ARGS == *"--test"* ]]; then
    echo "Start kind cluster for test phase"
    export kind_cluster_name="ml-lab-testcluster"
    kind create cluster --config=/kind-config.yaml --name $kind_cluster_name
    sed -i -E 's/[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+/'"$_HOST_IP"'/g' ~/.kube/config
    sed -i '6i\    insecure-skip-tls-verify: true' ~/.kube/config
    sed -i 's/certificate-authority-data/#certificate-authority-data/g' ~/.kube/config
    cat ~/.kube/config
    kube_config_volume="kube-config"
    docker run --rm -v $kube_config_volume:/kube-config --env KUBE_DATA_CONFIG="$(cat ~/.kube/config | base64)" ubuntu:20.04 /bin/bash -c 'touch /kube-config/config && echo "$KUBE_DATA_CONFIG" | base64 --decode >> /kube-config/config'
fi

(while true; do
              df -h
              sleep 30
              done) & python -u build.py $INPUT_BUILD_ARGS $BUILD_SECRETS
echo "Cleanup Phase"
if [[ $INPUT_BUILD_ARGS == *"--test"* ]]; then
    # || true => don't make the cleanup fail the pipeline
    kind delete cluster --name $kind_cluster_name || true
    docker volume rm $kube_config_volume || true
fi
