

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
resolver=127.0.0.11

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
# When SSL is enabled, the Stream port is used as the entry port and for the main port ssl is enabled (the stream port forwards https to the ssl-enabled main port and ssh traffic to the OpenSSH server). In this case, switch the ports so that the user does not have to consider this.
main_port=8080
stream_port=8081
main_port_ssl=$main_port
if [[ "${SERVICE_SSL_ENABLED,,}" == true ]]; then
    temp=$stream_port
    stream_port=$main_port
    main_port_ssl="$temp ssl"
    main_port=$temp

    sed -i "s|# ssl_certificate \${SSL_CERTIFICATE_PATH}| ssl_certificate ${_SSL_RESOURCES_PATH}/cert.crt;|g" /etc/nginx/nginx.conf;
    sed -i "s|# ssl_certificate_key \${SSL_CERTIFICATE_KEY_PATH}| ssl_certificate_key ${_SSL_RESOURCES_PATH}/cert.key;|g" /etc/nginx/nginx.conf;
fi
sed -i "s/\${MAIN_PORT_SSL}/$main_port_ssl/g" /etc/nginx/nginx.conf;
sed -i "s/\${MAIN_PORT}/$main_port/g" /etc/nginx/nginx.conf;
sed -i "s/\${STREAM_PORT}/$stream_port/g" /etc/nginx/nginx.conf;

# Start nginx
nginx -c /etc/nginx/nginx.conf

# Set the gunicorn / fastAPI port
export PORT=8090
# Start the backend server
/resources/start.sh
