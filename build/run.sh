# Pass the container run arguments to the build script
echo "$@"
cd /resources
python -u /resources/build.py "$@"
