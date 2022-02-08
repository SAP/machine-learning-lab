# Start nginx
nginx -c /etc/nginx/nginx.conf

# Set the gunicorn / fastAPI port
export PORT=8090

# Start the backend server
/resources/start.sh
