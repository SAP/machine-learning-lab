

# Create / copy certificates (has to be done here as the setup container in Kubernetes mode setups the ssl certificate)
if [[ "${SERVICE_SSL_ENABLED}" == true ]]; then
    /resources/setup-certs.sh;
fi

# ${LAB_ACTION,,} is the lower-cased LAB_ACTION variable
if [[ "${LAB_ACTION,,}" != serve ]]; then
    # TODO: run the setup script / container
    echo "";
fi

# Configure variables in nginx
system_namespace=${SYSTEM_NAMESPACE}
contaxy_base_url=${CONTAXY_BASE_URL}
service_suffix="''"
# Read dns resolver address from /etc/resolv.conf
resolver=$(awk '/nameserver/{a=(a?a" "$2:$2)} END{print a}' /etc/resolv.conf 2> /dev/null)

if [[ "${DEPLOYMENT_MANAGER,,}" == k8s || "${DEPLOYMENT_MANAGER,,}" == kubernetes ]]; then
    service_suffix=".$(cat /var/run/secrets/kubernetes.io/serviceaccount/namespace).svc.cluster.local";
    resolver="kube-dns.kube-system.svc.cluster.local valid=10s"
fi

# Substitute variables in all nginx config files including subdirectories
find /etc/nginx/ -name "*.conf" -exec sed -s -i "s/\${SYSTEM_NAMESPACE}/${system_namespace}/g" {} +
find /etc/nginx/ -name "*.conf" -exec sed -s -i "s@\${CONTAXY_BASE_URL}@${contaxy_base_url}@g" {} +
find /etc/nginx/ -name "*.conf" -exec  sed -s -i "s@\${SERVICE_SUFFIX}@${service_suffix}@g" {} +
find /etc/nginx/ -name "*.conf" -exec  sed -s -i "s/\${RESOLVER}/${resolver}/g" {} +

# Configure SSL variables in nginx
if [[ "${SERVICE_SSL_ENABLED,,}" == true ]]; then
    sed -i "s|# listen 443 ssl;|listen 443 ssl;|g" /etc/nginx/nginx.conf;
    sed -i "s|# ssl_certificate \${SSL_CERTIFICATE_PATH};|ssl_certificate ${_SSL_RESOURCES_PATH}/cert.crt;|g" /etc/nginx/nginx.conf;
    sed -i "s|# ssl_certificate_key \${SSL_CERTIFICATE_KEY_PATH};|ssl_certificate_key ${_SSL_RESOURCES_PATH}/cert.key;|g" /etc/nginx/nginx.conf;
fi

# Configure ssh server
sed -i "s@{KUBERNETES_SERVICE_HOST}@${KUBERNETES_SERVICE_HOST}@g" /etc/ssh/authorize.sh
sed -i "s@{KUBERNETES_SERVICE_PORT}@${KUBERNETES_SERVICE_PORT}@g" /etc/ssh/authorize.sh
sed -i "s@{SSH_TARGET_LABELS}@${SSH_TARGET_LABELS}@g" /etc/ssh/authorize.sh
sed -i "s@{SSH_PERMIT_TARGET_HOST}@${SSH_PERMIT_TARGET_HOST}@g" /etc/ssh/authorize.sh
sed -i "s@{SSH_TARGET_KEY_PATH}@${SSH_TARGET_KEY_PATH}@g" /etc/ssh/authorize.sh
sed -i "s@{SSH_TARGET_PUBLICKEY_API_PORT}@${SSH_TARGET_PUBLICKEY_API_PORT}@g" /etc/ssh/authorize.sh
# Execute script to collect workspace keys if action is serve
#nohup python /etc/ssh/update_authorized_keys.py full&>/dev/null &
python /etc/ssh/update_authorized_keys.py full

# Create env-config.js which is loaded by the webapp
# It stores makes environment variables defined in default_env.txt available to the webapp
/resources/create_env-config-js.sh

# Start nginx
nginx -c /etc/nginx/nginx.conf

# Start the ssh server
nohup env -i /usr/local/sbin/sshd -D -e -f /etc/ssh/sshd_config &>/var/log/ssh.log &

# Start the sslh server
if [[ "${SERVICE_SSL_ENABLED,,}" == true ]]; then
  /usr/sbin/sslh-select --background -p 0.0.0.0:8080 --ssh 127.0.0.1:22 --ssl 127.0.0.1:443
else
  /usr/sbin/sslh-select --background -p 0.0.0.0:8080 --ssh 127.0.0.1:22 --http 127.0.0.1:80
fi

# Set the gunicorn / fastAPI port
export PORT=8090
# Start the backend server
/resources/start.sh
