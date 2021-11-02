# Files extension
This extension serves mainly a UI to explore specific files in Contaxy.
The files seen here are prefixed by a folder.

# How to install on Contaxy
To install it globally in Contaxy Make a post request to
`/projects/ctxy-global/extension`
And the body:
```json
{
  "api_extension_endpoint": "",
  "ui_extension_endpoint": "80/#",
  "extension_type": "project-extension",
  "container_image": "ctxy-files-ui:latest",
  "parameters": {
    "FOLDER": "datasets"
  },
  "endpoints": [
    "80"
  ],
  "display_name": "contaxy-extension-files",
  "description": "Extension to show filtered files"
}
```

# Develop
To run locally please do:
```shell
python build.py --make
```
This will build the webapp locally as well as the dockerfile.
