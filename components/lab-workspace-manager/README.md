Installation:

```json
{
  "api_extension_endpoint": "8080/api",
  "ui_extension_endpoint": "8080/app#/users/{env.userId}/workspace",
  "extension_type": "project-extension",
  "container_image": "lab_workspace_manager:latest",
  "parameters": {
    "WORKSPACE_IMAGE": "mltooling/ml-workspace-minimal"
  },

  "endpoints": [
    "8080"
  ],
  "display_name": "Workspace Extension",
  "description": "Extension that adds personal ML workspaces."
}
```
