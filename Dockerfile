FROM ubuntu:20.04

# Install nginx (see http://nginx.org/en/linux_packages.html#Ubuntu)
RUN \
    apt-get update \
    && apt-get install -y curl gnupg2 ca-certificates lsb-release \
    && echo "deb http://nginx.org/packages/mainline/ubuntu `lsb_release -cs` nginx" | tee /etc/apt/sources.list.d/nginx.list \
    && curl -fsSL https://nginx.org/keys/nginx_signing.key | apt-key add - \
    && apt-get update \
    && apt-get install nginx \
    && apt-get install -y nginx-module-njs

# Install python3 and pip
RUN \
    apt-get update \
    && apt-get install -y python3.8 python3-pip \
    && ln -s /usr/bin/python3.8 /usr/bin/python

# Install lib required for psycopg2
RUN \
  apt-get update \
  && apt-get install -y libpq-dev

# Install gunicorn and uvicorn to run FastAPI optimized
RUN pip install --no-cache-dir "uvicorn[standard]" gunicorn fastapi faker

RUN mkdir /resources

## Compile the OpenSSH module manually
## Download from here: https://cdn.openbsd.org/pub/OpenBSD/OpenSSH/portable/
RUN mkdir /var/run/sshd && \
    mkdir /root/.ssh && \
    mkdir /var/lib/sshd && \
    chmod -R 700 /var/lib/sshd/ && \
    chown -R root:sys /var/lib/sshd/ && \
    useradd -r -U -d /var/lib/sshd/ -c "sshd privsep" -s /bin/false sshd && \
    apt-get update && \
    apt-get install -y libssl-dev zlib1g-dev wget && \
    cd /resources && \
    wget "https://cdn.openbsd.org/pub/OpenBSD/OpenSSH/portable/openssh-8.3p1.tar.gz" && \
    tar xfz openssh-8.3p1.tar.gz && \
    cd /resources/openssh-8.3p1/ && \
    # modify the code where the 'PermitOpen' host is checked so that it supports regexes
    sed -i "s@strcmp(allowed_open->host_to_connect, requestedhost) != 0@strcmp(allowed_open->host_to_connect, requestedhost) != 0 \&\& match_hostname(requestedhost, allowed_open->host_to_connect) == 0@g" ./channels.c && \
    # Surpress output - if there is a problem remove to see logs > /dev/null
    ./configure > /dev/null  && \
    make > /dev/null  && \
    make install > /dev/null  && \
    # filelock is needed for our custom AuthorizedKeysCommand script in the OpenSSH server
    pip install --no-cache-dir filelock && \
    # Python docker / kubernetes client is needed for caching the authorized keys in Docker or Kubernetes mode
    apt-get install -y python3-setuptools && \
    pip install --no-cache-dir kubernetes && \
    pip install --no-cache-dir docker

## Create user for ssh
# https://gist.github.com/smoser/3e9430c51e23e0c0d16c359a2ca668ae
# https://www.tecmint.com/restrict-ssh-user-to-directory-using-chrooted-jail/
# http://www.ab-weblog.com/en/creating-a-restricted-ssh-user-for-ssh-tunneling-only/
RUN useradd -d /home/limited-user -m -s /bin/true --gid nogroup --skel /dev/null --create-home limited-user && \
    #chmod 755 /home/limited-user && \
    #chmod g+rwx /home/limited-user && \
    echo 'PATH=""' >> /home/limited-user/.profile && \
    echo 'limited-user:limited' |chpasswd && \
    chmod 555 /home/limited-user/ && \
    cd /home/limited-user/ && \
    # .bash_logout .bashrc
    chmod 444 .profile && \
    chown root:root /home/limited-user/

## Install sslh server to multiplex http/s and ssh traffic
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y sslh

COPY docker/ssh/* /etc/ssh/

ENV PYTHONPATH=/resources/app \
    MODULE_NAME=contaxy.api \
    IS_CONTAXY_CONTAINER=true \
    SYSTEM_NAMESPACE=pylab \
    _SSL_RESOURCES_PATH=/resources/ssl \
    JWT_TOKEN_SECRET=please-change-this-secret \
    WEB_CONCURRENCY="1" \
    SSH_TARGET_LABELS="ctxy.deploymentType=service" \
    SSH_TARGET_PUBLICKEY_API_PORT="8080" \
    SSH_PERMIT_TARGET_HOST="*" \
    SSH_TARGET_KEY_PATH="~/.ssh/id_ed25519.pub"

RUN mkdir ${_SSL_RESOURCES_PATH}

# Install Contaxy
RUN pip install "contaxy[server]==0.0.22"
# Uncomment lines below if you want to install your local contaxy code (useful when developing contaxy features)
# By only copying the setup.py first, only the dependencies are installed which leads to faster docker builds on code changes
# COPY ./contaxy/backend/setup.py /resources/app/contaxy/
# RUN pip install /resources/app/contaxy[server]
# COPY ./contaxy/backend /resources/app/contaxy
# RUN pip install /resources/app/contaxy[server]

COPY ./docker/server/start.sh /resources/start.sh
RUN chmod +x /resources/start.sh

COPY ./docker/entrypoint.sh /resources/entrypoint.sh
RUN chmod +x /resources/entrypoint.sh


COPY ./docker/default_env.txt /resources/default_env.txt
COPY ./docker/create_env-config-js.sh /resources/create_env-config-js.sh
RUN chmod +x /resources/create_env-config-js.sh

COPY ./docker/server/gunicorn_conf.py /gunicorn_conf.py

COPY docker/nginx /etc/nginx
COPY docker/setup-certs.sh /resources/setup-certs.sh
RUN chmod +x /resources/setup-certs.sh
COPY webapp/build /resources/webapp

LABEL org.opencontainers.image.source https://github.com/SAP/machine-learning-lab

ENTRYPOINT ["/bin/bash"]
CMD ["/resources/entrypoint.sh"]
