from universal_build import build_utils

args = build_utils.parse_arguments()

if args[build_utils.FLAG_VERSION]:
    # The version is also needed when just tests are executed, so set it independent of the passed flags
    completed_process = build_utils.run(
        "mvn versions:set -DnewVersion=" + args[build_utils.FLAG_VERSION]
    )
    if completed_process.returncode > 0:
        build_utils.log("Failed to apply version " + args[build_utils.FLAG_VERSION])
        build_utils.run("mvn versions:revert")
        build_utils.exit_process(1)
    build_utils.run("mvn versions:commit")

if args[build_utils.FLAG_MAKE]:
    # Check if all project can be build, otherwise exit build script
    completed_process = build_utils.run("mvn clean package")
    if completed_process.returncode > 0:
        build_utils.log("Failed to build project")
        build_utils.exit_process(1)

    build_utils.run("mvn -N clean install")

# libraries
build_utils.build("environment-lib", args)
build_utils.build("service-lib", args)

# services
build_utils.build("lab-service", args)
