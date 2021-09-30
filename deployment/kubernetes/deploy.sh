# For a local deployment, you can for example use 'kind'. For network policies to take effect, install the Calico plugin as well.
# Important note: it might be possible that you get an Image pull issue, when your images are not pushed to a registry the Kubernetes cluster has access to. In case of 'kind', a way to load the images into the cluster is via `kind load docker-image ...` (e.g. `kind load docker-image test_deployment_contaxy`)

kubectl create namespace contaxy
sleep 2
kubectl -n contaxy apply -f service_accounts.yaml
kubectl -n contaxy apply -f services.yaml
sleep 2
kubectl -n contaxy apply -f deployments.yaml

# After deployment, seed the database by making a GET request to the endpoint '/api/seed/default'

