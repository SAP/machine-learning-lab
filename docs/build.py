from universal_build import build_utils

args = build_utils.parse_arguments()


def install_requirements():
    build_utils.run("pip install --upgrade mkdocs")
    build_utils.run("pip install mkdocs-material==4.6.3")
    build_utils.run("pip install --upgrade pygments")
    build_utils.run("pip install --upgrade pymdown-extensions")


if args[build_utils.FLAG_MAKE] or args[build_utils.FLAG_RELEASE]:
    install_requirements()

if args[build_utils.FLAG_MAKE]:
    build_utils.run("mkdocs build")

if args[build_utils.FLAG_RELEASE]:
    build_utils.run("mkdocs gh-deploy --clean")
