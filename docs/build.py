from universal_build import build_utils

args = build_utils.get_sanitized_arguments()

if args[build_utils.FLAG_MAKE]:
    build_utils.run("pip install --upgrade mkdocs")
    build_utils.run("pip install mkdocs-material==4.6.3")
    build_utils.run("pip install --upgrade pygments")
    build_utils.run("pip install --upgrade pymdown-extensions")

    build_utils.run("mkdocs build")

if args[build_utils.FLAG_RELEASE]:
    build_utils.run("mkdocs gh-deploy --clean")
