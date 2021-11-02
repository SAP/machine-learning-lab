# Create env-config.js
chmod +x /resources/create_env-config-js.sh

/resources/create_env-config-js.sh

cp ./env-config.js /usr/share/nginx/html/

# Start nginx server
nginx -g "daemon off;"
