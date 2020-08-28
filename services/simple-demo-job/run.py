import os, sys, time, errno

print("Starting job")

run_duration = int(os.environ["RUN_DURATION_MINUTES"]) # allows to change the run duration
exit_code = int(os.environ["CUSTOM_EXIT_CODE"]) # allows to change the exit code

for x in range(run_duration * 6):
    print("This prints every 10 seconds.")
    time.sleep(10)

sys.exit(exit_code)