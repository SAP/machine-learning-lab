from pydantic import BaseSettings
from typing import Optional
# from pyinstrument import Profiler

import pytest
import os


class TestSettings(BaseSettings):
    """Test Settings."""
    # ACTIVATE_TEST_PROFILING: bool = True
    LAB_BACKEND: str = ""
    LAB_PROJECT: str = ""
    LAB_TOKEN: str = ""

    class Config:
        # Support local .env files
        env_file = ".env"
        env_file_encoding = "utf-8"


test_settings = TestSettings()

# @pytest.fixture(autouse=True)
# def auto_profile_tests(request) -> None:  # type: ignore
#     """Activates automatic profiling."""
#     if not test_settings.ACTIVATE_TEST_PROFILING:
#         # Only execute if debug is activated
#         yield None
#     else:
#         profiler = Profiler()
#         profiler.start()
#         yield None
#         profiler.stop()
#         try:
#             output_file = "./prof/" + request.node.nodeid.replace("::", "/") + ".html"
#             os.makedirs(os.path.dirname(output_file), exist_ok=True)
#             with open(output_file, "w") as f:
#                 f.write(profiler.output_html())
#         except Exception:
#             # Fail silently
#             pass
