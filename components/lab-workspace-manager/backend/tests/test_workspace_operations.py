from typing import List
from unittest.mock import Mock

import pytest
from contaxy.schema import ClientValueError, Service, ServiceInput

from lab_workspace_manager.app import (
    LABEL_EXTENSION_DEPLOYMENT_TYPE,
    deploy_workspace,
    delete_workspace,
    get_workspace,
    list_workspaces, WorkspaceInput, WorkspaceCompute, Workspace,
)


def create_test_workspace_input(display_name: str) -> WorkspaceInput:
    return WorkspaceInput(
        container_image="mltooling/ml-workspace-minimal",
        display_name=display_name,
        compute=WorkspaceCompute(
            cpus=1,
            memory=1000,
        )
    )


class ServiceManagerMock:
    def __init__(self):
        self._fake_services = [
            Service(
                container_image="ws-image",
                id="my-workspace",
                metadata={LABEL_EXTENSION_DEPLOYMENT_TYPE: "workspace"},
                display_name="my-workspace"
            ),
            Service(
                container_image="service-image",
                id="my-service",
                metadata={LABEL_EXTENSION_DEPLOYMENT_TYPE: "service"},
                display_name="my-service",
            ),
            Service(container_image="service-image", id="my-service-2", metadata={}, display_name="my-service-2"),
            Service(container_image="other-image", id="other-service", display_name="other-service"),
        ]
        self.delete_service = Mock()

    def deploy_service(self, project_id: str, service: ServiceInput) -> Service:
        return Service(id=service.display_name, **service.dict())

    def list_services(self, project_id: str) -> List[Service]:
        return self._fake_services

    def get_service_metadata(self, project_id, service_id) -> Service:
        return next(
            (service for service in self._fake_services if service.id == service_id)
        )


class ComponentManagerMock:
    def __init__(self):
        self._service_manager = ServiceManagerMock()

    def get_service_manager(self):
        return self._service_manager


@pytest.mark.unit
class TestWorkspaceOperations:
    def test_create_workspace(self):
        workspace_input = create_test_workspace_input("my-workspace")
        component_manager = ComponentManagerMock()

        workspace: Workspace = deploy_workspace(workspace_input, "test-user-id", component_manager)

        assert workspace.container_image == workspace_input.container_image
        assert workspace_input.display_name in workspace.display_name

    def test_list_workspaces(self):
        component_manager = ComponentManagerMock()

        workspaces = list_workspaces("test-user-id", component_manager)

        assert len(workspaces) == 1
        assert workspaces[0].id == "my-workspace"

    def test_get_workspace(self):
        component_manager = ComponentManagerMock()

        workspace = get_workspace("test-user-id", "my-workspace", component_manager)

        assert workspace.id == "my-workspace"

    def test_get_workspace_that_is_no_workspace(self):
        component_manager = ComponentManagerMock()

        with pytest.raises(ClientValueError):
            get_workspace("test-user-id", "my-service", component_manager)

    def test_delete_workspace(self):
        component_manger = ComponentManagerMock()

        delete_workspace("test-user-id", "my-workspace", False, component_manger)

        component_manger.get_service_manager().delete_service.assert_called_with(
            "test-user-id", "my-workspace", False
        )

    def test_delete_workspace_that_is_no_workspace(self):
        component_manger = ComponentManagerMock()

        with pytest.raises(ClientValueError):
            delete_workspace("test-user-id", "my-service", False, component_manger)
