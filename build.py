import pathlib as pathlib
import shutil
from typing import Dict, Union

import urllib3
from universal_build import build_utils


def check_and_download_swagger_cli(file_path: str) -> bool:
    if not pathlib.Path(file_path).is_file():
        # build_utils.run('wget https://repo1.maven.org/maven2/io/swagger/codegen/v3/swagger-codegen-cli/3.0.23/swagger-codegen-cli-3.0.23.jar -O ./swagger-codegen-cli.jar')
        swagger_codegen_cli_download_url = "https://repo1.maven.org/maven2/io/swagger/codegen/v3/swagger-codegen-cli/3.0.23/swagger-codegen-cli-3.0.23.jar"
        response = urllib3.PoolManager().request(
            "GET", swagger_codegen_cli_download_url
        )
        if response.status == 200:
            with open(file_path, "wb") as f:
                f.write(response.data)
        else:
            return False
    return True


def generate_and_copy_js_client() -> bool:
    temp_dir = "./temp"
    pathlib.Path(temp_dir).mkdir(exist_ok=True)
    swagger_codegen_cli = f"{temp_dir}/swagger-codegen-cli.jar"
    is_successful = check_and_download_swagger_cli(swagger_codegen_cli)
    if not is_successful:
        return False
    swagger_path = "./backend/lab-service/src/main/resources/swagger/swagger.json"
    output_path = f"{temp_dir}/client"
    build_utils.run(
        f"java -jar {swagger_codegen_cli} generate -i {swagger_path} -l javascript -o {output_path} --additional-properties useES6=true"
    )
    # shutil.move(f"{output_path}/src/", "./webapp/src/services/mllab-client")
    try:
        for file in pathlib.Path(f"{output_path}/src/").iterdir():
            file_name = str(file.parts[-1])
            new_file_name = file_name
            if file_name == "index.js":
                new_file_name = "lab-api.js"
            target_file_path = f"./webapp/src/services/client/{new_file_name}"
            # Delete existing client files to be replaced with the new ones
            if pathlib.Path(target_file_path).is_file():
                pathlib.Path(target_file_path).unlink()
            elif pathlib.Path(target_file_path).is_dir():
                shutil.rmtree(target_file_path)
            shutil.move(str(file), target_file_path)
    except FileNotFoundError as e:
        build_utils.log(str(e))
        return False
    return True


def main(args: Dict[str, Union[bool, str]]):
    # Move libraries to
    build_utils.run("rm -r -f services/lab-workspace/docker-res/duplicated-resources/")
    build_utils.run("mkdir services/lab-workspace/docker-res/duplicated-resources/")
    build_utils.run(
        "cp -R libraries/* services/lab-workspace/docker-res/duplicated-resources/"
    )

    # build base images
    # For just testing, the lab-workspace does not have to be built as it is not covered by tests yet
    # TODO: in GitHub actions add workspace to --skip-path to ignore it
    build_utils.build("services/lab-workspace", args)
    build_utils.build("services/simple-workspace-service", args)

    build_utils.build("services/lab-model-service", args)

    # build demo services/jobs
    build_utils.build("services/simple-demo-job", args)
    build_utils.build("services/simple-demo-service", args)
    build_utils.build("services/simple-fastapi-service", args)

    # build webapp and move build into backend service
    # TODO: MOVE SWAGGER API TO WEB APP
    # build main application first time to generate swagger config
    backend_args = {**args}
    backend_args[build_utils.FLAG_TEST] = False
    build_utils.build("backend", backend_args)

    if args[build_utils.FLAG_MAKE]:
        is_successful = generate_and_copy_js_client()
        if not is_successful:
            build_utils.log("Error in generating the JavaScript client library")
            build_utils.exit_process(1)
        # format the just generated JavaScript client to make it conform with the project and prevent showing format-related changes in Git
        build_utils.run("cd webapp; npm run prettier ./src/services/client/; cd ..")

    build_utils.build("webapp", args)
    if args[build_utils.FLAG_MAKE]:
        # Move webapp build into resources
        build_utils.run("rm -r -f backend/lab-service/src/main/resources/app/")
        build_utils.run("mkdir backend/lab-service/src/main/resources/app/")
        build_utils.run(
            "cp -R webapp/build/* backend/lab-service/src/main/resources/app/",
            exit_on_error=True,
        )

    # build documentation
    build_utils.build("docs", args)
    if args[build_utils.FLAG_MAKE]:
        # Move documentation build into resources
        build_utils.run("rm -r -f backend/lab-service/src/main/resources/docs/")
        build_utils.run("mkdir backend/lab-service/src/main/resources/docs/")
        build_utils.run(
            "cp -R docs/site/* backend/lab-service/src/main/resources/docs/"
        )

    # build main application second time to bundle webapp
    build_utils.build("backend", args)


if __name__ == "__main__":
    # Check for valid arguments
    args = build_utils.parse_arguments()

    if args[build_utils.FLAG_RELEASE]:
        # Run main without release to see whether everthing can be built and all tests run through
        arguments = dict(args)
        arguments[build_utils.FLAG_RELEASE] = False
        main(arguments)
        # Run main again without building and testing the components again
        arguments = {
            **arguments,
            build_utils.FLAG_MAKE: False,
            build_utils.FLAG_TEST: False,
            build_utils.FLAG_FORCE: True,
        }
        main(arguments)
    else:
        main(args)
