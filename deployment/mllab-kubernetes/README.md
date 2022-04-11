# ML Lab installation via helm chart
This folder contains a helm chart for installing ML Lab with all its required components.
The default values in the values.yaml should already be good for a test deployment but need to be adjusted for productive deployments.
The secret-values.yaml file contains all passwords and secret keys that need to be set.
For a productive deployment these values should be changed!

To install ML Lab using this helm chart make sure that you navigate to this folder and execute:
```
helm install ml-lab -n ml-lab -f values.yaml -f secret-values.yaml --create-namespace .
```
