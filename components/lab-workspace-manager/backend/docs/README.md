<!-- markdownlint-disable -->

# API Overview

## Modules

- [`lab_workspace_manager.app`](./lab_workspace_manager.app.md#module-lab_workspace_managerapp)
- [`lab_workspace_manager.config`](./lab_workspace_manager.config.md#module-lab_workspace_managerconfig)
- [`lab_workspace_manager.schema`](./lab_workspace_manager.schema.md#module-lab_workspace_managerschema)
- [`lab_workspace_manager.utils`](./lab_workspace_manager.utils.md#module-lab_workspace_managerutils)

## Classes

- [`config.WorkspaceImage`](./lab_workspace_manager.config.md#class-workspaceimage)
- [`config.WorkspaceManagerSettings`](./lab_workspace_manager.config.md#class-workspacemanagersettings)
- [`schema.Workspace`](./lab_workspace_manager.schema.md#class-workspace)
- [`schema.WorkspaceBase`](./lab_workspace_manager.schema.md#class-workspacebase)
- [`schema.WorkspaceCompute`](./lab_workspace_manager.schema.md#class-workspacecompute)
- [`schema.WorkspaceConfigOptions`](./lab_workspace_manager.schema.md#class-workspaceconfigoptions)
- [`schema.WorkspaceInput`](./lab_workspace_manager.schema.md#class-workspaceinput)
- [`schema.WorkspaceUpdate`](./lab_workspace_manager.schema.md#class-workspaceupdate)

## Functions

- [`app.compute_min_cpu`](./lab_workspace_manager.app.md#function-compute_min_cpu)
- [`app.compute_min_memory`](./lab_workspace_manager.app.md#function-compute_min_memory)
- [`app.create_ws_from_service`](./lab_workspace_manager.app.md#function-create_ws_from_service)
- [`app.create_ws_service_input`](./lab_workspace_manager.app.md#function-create_ws_service_input)
- [`app.create_ws_service_update`](./lab_workspace_manager.app.md#function-create_ws_service_update)
- [`app.delete_workspace`](./lab_workspace_manager.app.md#function-delete_workspace)
- [`app.deploy_workspace`](./lab_workspace_manager.app.md#function-deploy_workspace): Create a new personal workspace by creating a Contaxy service with a workspace image in the personal project.
- [`app.get_workspace`](./lab_workspace_manager.app.md#function-get_workspace)
- [`app.get_workspace_config`](./lab_workspace_manager.app.md#function-get_workspace_config)
- [`app.is_ws_service`](./lab_workspace_manager.app.md#function-is_ws_service)
- [`app.list_workspaces`](./lab_workspace_manager.app.md#function-list_workspaces)
- [`app.request_user_token`](./lab_workspace_manager.app.md#function-request_user_token)
- [`app.start_workspace`](./lab_workspace_manager.app.md#function-start_workspace)
- [`app.update_workspace`](./lab_workspace_manager.app.md#function-update_workspace)
- [`schema.check_if_in_options`](./lab_workspace_manager.schema.md#function-check_if_in_options): Create a pydantic validator that checks if the given field is one of the valid options.
- [`utils.get_component_manager`](./lab_workspace_manager.utils.md#function-get_component_manager): Returns the initialized component manager.


---

_This file was automatically generated via [lazydocs](https://github.com/ml-tooling/lazydocs)._
