from subprocess import call

call(["python build.py"], shell=True)
call(["mkdocs serve"], shell=True)