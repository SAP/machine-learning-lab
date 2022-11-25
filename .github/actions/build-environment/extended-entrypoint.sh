#!/bin/bash

# Stops script execution if a command has an error
set -e
# Print command
set -x

# set default build args if not provided
export CONTAXY_ENDPOINT=http://"$_HOST_IP":30010
# set the root path because in the contaxy container, nginx will handle the request
export CONTAXY_ROOT_PATH="/api"

if [[ $INPUT_BUILD_ARGS == *"--test"* ]]; then
    # Prepare the test landscape
    echo "Start kind cluster for test phase"
    export kind_cluster_name="contaxy-testcluster"
    export KUBE_AVAILABLE="true"
    # Delete kind cluster in case it was not cleaned up last time (in case it does not exist, the command just does nothing):
    kind delete cluster --name $kind_cluster_name
    kind create cluster --config=/kind-config.yaml --name $kind_cluster_name
    sed -i -E 's/[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+/'"$_HOST_IP"'/g' ~/.kube/config
    sed -i '6i\    insecure-skip-tls-verify: true' ~/.kube/config
    sed -i 's/certificate-authority-data/#certificate-authority-data/g' ~/.kube/config
    cat ~/.kube/config
    kube_config_volume="kube-config"
    docker run --rm -v $kube_config_volume:/kube-config --env KUBE_DATA_CONFIG="$(cat ~/.kube/config | base64)" ubuntu:20.04 /bin/bash -c 'touch /kube-config/config && echo "$KUBE_DATA_CONFIG" | base64 --decode >> /kube-config/config'

    # Install the Calico Network Plugin for networking so that Network Policies have an effect (see this GitHub issue: https://github.com/kubernetes-sigs/kind/issues/842#issuecomment-554775260)
    kubectl apply -f https://docs.projectcalico.org/v3.8/manifests/calico.yaml
    kubectl -n kube-system set env daemonset/calico-node FELIX_IGNORELOOSERPF=true

    # Check whether test-marker has the value integration (quotation marks are allowed)
    echo $INPUT_BUILD_ARGS
    echo "Docker compose up..."
    if [[ $INPUT_BUILD_ARGS =~ ^.*--test-marker[=|\ ][\'\"]?integration[\'\"]?([\ ].*$|$) ]]; then
        echo "In Docker compose up ..."
        # TODO: deploy Contaxy as Kubernetes
        # TODO: do this in the integration test's setup phase instead of here!
        cd test_deployment && docker-compose up -d && cd ..
    fi
fi

# (while true; do
#               df -h
#               sleep 30
#               done) & python -u build.py $INPUT_BUILD_ARGS $BUILD_SECRETS

# Install build requirements
pip install -r "$GITHUB_WORKSPACE/build_requirements.txt"


# Copy code from the original build-environment entrypoint
# Disable immediate stop so that the cleanup phase can run even if entrypoint-sh fails
set +e

echo "Run build.py"
# set default build args if not provided
BUILD_ARGS="$INPUT_BUILD_ARGS"
if [ -z "$BUILD_ARGS" ]; then
    BUILD_ARGS="--check --make --test"
fi
if [ -n "$GITHUB_TOKEN" ]; then
    echo "GitHub Token provided. Setting up GitHub URLs..."
    # Use the github token to authenticate the git interaction (see this Stackoverflow answer: https://stackoverflow.com/a/57229018/5379273)
    git config --global url."https://api:$GITHUB_TOKEN@github.com/".insteadOf "https://github.com/"
    git config --global url."https://ssh:$GITHUB_TOKEN@github.com/".insteadOf "ssh://git@github.com/"
    git config --global url."https://git:$GITHUB_TOKEN@github.com/".insteadOf "git@github.com:"

    BUILD_ARGS="$BUILD_ARGS --github-token=$GITHUB_TOKEN"
fi
# Navigate to working directory, if provided
if [ -n "$INPUT_WORKING_DIRECTORY" ]; then
    cd "$INPUT_WORKING_DIRECTORY"
else
    cd "$GITHUB_WORKSPACE"
fi
python -u build.py $BUILD_ARGS
exit_code=$?

echo "Cleanup Phase"
if [[ $INPUT_BUILD_ARGS == *"--test"* ]]; then
    # || true => don't make the cleanup fail the pipeline
    kind delete cluster --name $kind_cluster_name || true
    docker volume rm $kube_config_volume || true
    cd test_deployment && docker-compose down && cd ..
fi

exit $exit_code
