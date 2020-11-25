#!/bin/bash

# Stops script execution if a command has an error
set -e

# set default build args if not provided
export SERVICE_HOST="$_HOST_IP"

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

# (while true; do
#               df -h
#               sleep 30
#               done) & python -u build.py $INPUT_BUILD_ARGS $BUILD_SECRETS

# Call the original build-environment entrypoint (doing so, the logic does not have to be copied)
/bin/bash /entrypoint.sh "$@"

echo "Cleanup Phase"
if [[ $INPUT_BUILD_ARGS == *"--test"* ]]; then
    # || true => don't make the cleanup fail the pipeline
    kind delete cluster --name $kind_cluster_name || true
    docker volume rm $kube_config_volume || true
fi
