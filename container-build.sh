# Create the build container that has everything installed to build the project and run the tests
version="${1:-latest}"
docker build -t mllab-container-build:$version ./build

docker run \
    -v $(pwd)/.m2/:/root/.m2/:delegated \
    -v $(pwd):/resources:delegated \
    -v /var/run/docker.sock:/var/run/docker.sock \
    --network lab-core \
    --env SERVICE_HOST=lab-backend \
    --env SERVICE_PORT=8091 \
    mllab-container-build:$version --build --test
