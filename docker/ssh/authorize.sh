#!/bin/bash
# Command to be called by the OpenSSH Server AuthorizedKeysCommand to check whether a valid Runtime container exists

# Must be replaced during container startup with the value of the environment variable.
# This script is run in an SSH session and, thus, the environment variable does not exist
#export LAB_NAMESPACE="{LAB_NAMESPACE}"
export KUBERNETES_SERVICE_HOST="{KUBERNETES_SERVICE_HOST}"
export KUBERNETES_SERVICE_PORT="{KUBERNETES_SERVICE_PORT}"
export SSH_TARGET_LABELS="{SSH_TARGET_LABELS}"
export SSH_PERMIT_TARGET_HOST="{SSH_PERMIT_TARGET_HOST}"
export SSH_TARGET_KEY_PATH="{SSH_TARGET_KEY_PATH}"
export SSH_TARGET_PUBLICKEY_API_PORT="{SSH_TARGET_PUBLICKEY_API_PORT}"

public_key=$1
echo "$(date) $public_key" >> /etc/ssh/access.log
# Renew cache every x hours
CACHE_TIME=$((60 * 60 * 4))

[ ! -f /etc/ssh/authorized_keys_cache ] && python /etc/ssh/update_authorized_keys.py full

current_date=$(date +%s)
cache_last_modified=$(date -r /etc/ssh/authorized_keys_cache +%s)
time_difference=$(($current_date - $cache_last_modified))

# check if the key is in the authorized_keys_cache file.
# If not, perform a synchronous update (the user has to wait) and return the content then.
# Use still a shorter chache validation time to prevent brute-force try (when a user offers a key that does not match
# any runtime key then the test will always fail and a costly key update is triggered)
# Check for " $public_key " with spaces before and after to prevent matching parts of a key
SHORTER_CACHE_TIME=$((60 * 2))
if ! grep -q " $public_key " /etc/ssh/authorized_keys_cache; then
    if [ $time_difference -ge $SHORTER_CACHE_TIME ]; then
        python /etc/ssh/update_authorized_keys.py
    fi
elif [ $time_difference -ge $CACHE_TIME ]; then
    # Run the python script to update the cache in the background so the command can directly return
    # All targets are queried for the key (full update)
    nohup python /etc/ssh/update_authorized_keys.py full > /dev/null 2>&1 &
fi

# Return the collected keys to the OpenSSH server
cat /etc/ssh/authorized_keys_cache
