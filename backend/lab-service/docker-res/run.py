"""
Container init script
"""
#!/usr/bin/python

from subprocess import call, Popen, getoutput
import os
import time
import sys

ENV_LAB_ACTION = os.environ["LAB_ACTION"]
ENV_RESOURCES_PATH = os.environ["_RESOURCES_PATH"]
ENV_LAB_BASE_URL = os.environ["LAB_BASE_URL"]

# create / copy certificates (has to be done here as the setup container in Kubernetes mode setups the ssl certificate)
call(ENV_RESOURCES_PATH + "/setup_certs.sh", shell=True)

if ENV_LAB_ACTION and ENV_LAB_ACTION.lower() != "serve".lower():
    # Run java application without supervisor - e.g. for installation or update...
    failed = call("java $JAVA_OPTS -jar " + ENV_RESOURCES_PATH + "/service.jar", shell=True)
    if failed:
        print("Failed to start service.jar. Please make sure that you have copied the correct executable jar in the Dockerfile "
            "into the resource folder ($_RESOURCES_PATH).")
    sys.exit(failed)

ENV_SSL_RESOURCES_PATH = os.environ["_SSL_RESOURCES_PATH"]
ENV_NGINX_FILE = os.environ["_NGINX_CONFIG_PATH"]

# PREPARE SSL SERVING
ENV_NAME_SERVICE_SSL_ENABLED = "SERVICE_SSL_ENABLED"
if ENV_NAME_SERVICE_SSL_ENABLED in os.environ \
        and (os.environ[ENV_NAME_SERVICE_SSL_ENABLED] is True \
                or os.environ[ENV_NAME_SERVICE_SSL_ENABLED] == "true" \
                or os.environ[ENV_NAME_SERVICE_SSL_ENABLED] == "on"):

    # For HTTPS mode, switch the ports so that traffic its the multiplexer to differentiate between
    # HTTPS and ssh traffic
    call("sed -i -r 's/listen 8091;/listen temp;/g' " + ENV_NGINX_FILE, shell=True)
    call("sed -i -r 's/listen 8092;/listen 8091;/g' " + ENV_NGINX_FILE, shell=True)
    call("sed -i -r 's/listen temp;/listen 8092;/g' " + ENV_NGINX_FILE, shell=True)

    call("sed -i 's@#ssl_certificate_key@ssl_certificate_key " + ENV_SSL_RESOURCES_PATH + "/cert.key;@g' " + ENV_NGINX_FILE, shell=True)
    call("sed -i 's@#ssl_certificate@ssl_certificate " + ENV_SSL_RESOURCES_PATH + "/cert.crt;@g' " + ENV_NGINX_FILE, shell=True)

    call("sed -i -r 's/listen ([0-9]+);#ssl/listen \\1 ssl;/g' " + ENV_NGINX_FILE, shell=True)

###

# Set Lab Namespace in Nginx
ENV_LAB_NAMESPACE = os.environ["LAB_NAMESPACE"]
call("sed -i 's@set \$lab_namespace lab;@set \$lab_namespace " + ENV_LAB_NAMESPACE + ";@g' " + ENV_NGINX_FILE, shell=True)
call("sed -i 's@{LAB_BASE_URL}@" + ENV_LAB_BASE_URL + "@g' " + ENV_NGINX_FILE, shell=True)

# PREPARE DNS IN NGINX
ENV_NAME_SERVICES_RUNTIME = "SERVICES_RUNTIME"
if ENV_NAME_SERVICES_RUNTIME in os.environ \
        and (os.environ[ENV_NAME_SERVICES_RUNTIME].lower() == "k8s"
        or os.environ[ENV_NAME_SERVICES_RUNTIME].lower() == "kubernetes"):

    # replace the variable service suffix so that the fully qualified service name is used
    # TODO: replace .lab with the LAB_NAMESPACE env variable? Is this the network?
    kubernetes_namespace = getoutput("cat /var/run/secrets/kubernetes.io/serviceaccount/namespace")
    call("sed -i \"s/set \$service_suffix ''/set \$service_suffix ." + kubernetes_namespace + ".svc.cluster.local/g\" " + ENV_NGINX_FILE, shell=True)
    # replace the Docker default resolver with the Kubernetes kube-dns resolver
    call("sed -i 's/resolver 127.0.0.11/resolver kube-dns.kube-system.svc.cluster.local valid=10s/g' " + ENV_NGINX_FILE, shell=True)

# Configure ssh server
call("sed -i \"s@{SSH_TARGET_LABELS}@" + os.environ["SSH_TARGET_LABELS"] + "@g\" /etc/ssh/authorize.sh", shell=True)
call("sed -i \"s@{SSH_PERMIT_TARGET_HOST}@" + os.environ["SSH_PERMIT_TARGET_HOST"] + "@g\" /etc/ssh/authorize.sh", shell=True)
call("sed -i \"s@{SSH_TARGET_KEY_PATH}@" + os.environ["SSH_TARGET_KEY_PATH"] + "@g\" /etc/ssh/authorize.sh", shell=True)
call("sed -i \"s@{SSH_TARGET_PUBLICKEY_API_PORT}@" + os.environ["SSH_TARGET_PUBLICKEY_API_PORT"] + "@g\" /etc/ssh/authorize.sh", shell=True)

# access logs needs to be available in order for nginx to use it
call("mkdir -p " + ENV_RESOURCES_PATH + "/nginx/logs/ && touch " + ENV_RESOURCES_PATH + "/nginx/logs/host.access.log", shell=True)

# Execute script to collect workspace keys if action is serve
call("nohup python /etc/ssh/update_authorized_keys.py full&>/dev/null &", shell=True)

# Run supervisor process - main container process
call('supervisord -n -c /etc/supervisor/supervisord.conf', shell=True)
