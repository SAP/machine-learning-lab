import os
from typing import Any

from contaxy.operations.components import ComponentOperations
from contaxy.schema.auth import USER_ID_PARAM
from contaxy.schema.exceptions import CREATE_RESOURCE_RESPONSES
from contaxy.utils import fastapi_utils
from fastapi import Depends, FastAPI, status
from loguru import logger
from starlette.middleware.cors import CORSMiddleware

from insert_component_name_here.utils import CONTAXY_API_ENDPOINT, get_component_manager

app = FastAPI()
# Patch FastAPI to allow relative path resolution.
fastapi_utils.patch_fastapi(app)
# Allow CORS configuration
if "BACKEND_CORS_ORIGINS" in os.environ:
    app.add_middleware(
        CORSMiddleware,
        allow_origins=os.environ["BACKEND_CORS_ORIGINS"].split(","),
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )


@app.get(
    "/users/{user_id}/test",
    summary="Test endpoint",
    status_code=status.HTTP_200_OK,
    responses={**CREATE_RESOURCE_RESPONSES},
)
def example_endpoint(
    user_id: str = USER_ID_PARAM,
    component_manager: ComponentOperations = Depends(get_component_manager),
) -> Any:
    logger.info(user_id)
    user = component_manager.get_auth_manager().get_user(user_id)
    return user.username


if __name__ == "__main__":
    import uvicorn

    if not CONTAXY_API_ENDPOINT:
        raise RuntimeError("CONTAXY_API_ENDPOINT must be set")

    # Prevent duplicated logs
    log_config = uvicorn.config.LOGGING_CONFIG
    log_config["loggers"]["uvicorn"]["propagate"] = False
    uvicorn.run(
        "insert_component_name_here.app:app",
        host="localhost",
        port=int(os.getenv("PORT", 8080)),
        log_level="info",
        reload=True,
        log_config=log_config,
    )
