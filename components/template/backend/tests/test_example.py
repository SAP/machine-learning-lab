from unittest.mock import Mock

import pytest

from insert_component_name_here.app import example_endpoint


@pytest.mark.unit
class TestExampleOperations:
    def test_example_endpoint(self):
        component_manager = Mock()

        example_endpoint("test-user", component_manager)
        component_manager.get_auth_manager().get_user.assert_called_with("test-user")
