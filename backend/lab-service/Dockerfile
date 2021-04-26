FROM ubuntu:20.04

# Layer cleanup script
COPY docker-res/clean-layer.sh  /usr/bin/clean-layer.sh

# Make clean-layer executable
RUN chmod a+rwx /usr/bin/clean-layer.sh

# Generate and Set locals
# https://stackoverflow.com/questions/28405902/how-to-set-the-locale-inside-a-debian-ubuntu-docker-container#38553499
RUN \
    apt-get update && \
    apt-get install locales --yes && \
    # install locales-all?
    sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen && \
    locale-gen && \
    dpkg-reconfigure --frontend=noninteractive locales && \
    update-locale LANG=en_US.UTF-8 && \
    # Cleanup
    clean-layer.sh

ENV LC_ALL="en_US.UTF-8" \
    LANG="en_US.UTF-8" \
    LANGUAGE="en_US:en" \
    TZ="Europe/Berlin" \
    DEBIAN_FRONTEND="noninteractive"

# Install basics
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
                        wget \
                        curl \
                        unzip \
                        lsof \
                        vim \
                        iputils-ping \
                        software-properties-common \
                        python3-pip \
	                    apt-transport-https \
                        ca-certificates \
                        build-essential \
                        sudo && \
    ln -s /usr/bin/pip3 /usr/bin/pip && \
    pip install --upgrade pip && \
    ln -s /usr/bin/python3 /usr/bin/python && \
    ldconfig && \
    # Cleanup
    clean-layer.sh

# Set commonly used environment variables
ENV DATA_ENVIRONMENT="/data" \
    _NGINX_CONFIG_PATH="/etc/nginx/nginx.conf" \
    _RESOURCES_PATH="/resources" \
    _SSL_RESOURCES_PATH="/resources/ssl"

RUN \
    mkdir -p $_RESOURCES_PATH && \
    mkdir -p $_SSL_RESOURCES_PATH && \
    mkdir -p $DATA_ENVIRONMENT && \
    chmod -R a+rwx /data

# Add tini
RUN wget --quiet https://github.com/krallin/tini/releases/download/v0.19.0/tini -O /tini && \
    chmod +x /tini

# Define commonly used environment variables
ENV JAVA_HOME="/usr/lib/jvm/open-jdk"

### Install Java.
RUN \
    add-apt-repository ppa:openjdk-r/ppa && \
    apt-get update && \
    apt-get install -y openjdk-8-jre && \
    dpkg-query -l && \
    # Cleanup
    clean-layer.sh

ENV JAVA_OPTS ""

# Fixes issue with sudo, see https://unix.stackexchange.com/questions/578949/sudo-setrlimitrlimit-core-operation-not-permitted
RUN echo "Set disable_coredump false" >> /etc/sudo.conf

RUN apt-get update && apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg-agent \
    software-properties-common && \
    clean-layer.sh

# Install Docker in Container
RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add - && \
    sudo add-apt-repository \
        "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
        $(lsb_release -cs) \
        stable" && \
    apt-get update -y && \
    apt-get install -y docker-ce && \
    # Cleanup
    clean-layer.sh

# Kubernetes Support
## Install kubectl inside container so ML Lab can deploy Kubernetes artifacts
## On deployment, the kube config has to be mounted into the ML Lab Container
RUN \
    apt-get update && apt-get install -y apt-transport-https curl && \
    curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add - && \
    echo "deb http://apt.kubernetes.io/ kubernetes-xenial main" >> /etc/apt/sources.list.d/kubernetes.list && \
    #cat <<EOF >/etc/apt/sources.list.d/kubernetes.list \
    #    deb http://apt.kubernetes.io/ kubernetes-xenial main \
    #    EOF && \
    apt-get update && \
    apt-get install -y kubectl && \
    apt-mark hold kubectl && \
    # Cleanup
    clean-layer.sh

# Install Openresty for nginx proxy
RUN \
    apt-get update && \
    apt-get purge -y nginx nginx-common && \
    # libpcre required, otherwise you get a 'the HTTP rewrite module requires the PCRE library' error
    # Install apache2-utils to generate user:password file for nginx.
    # zlib1g-dev is required to build the http-module
    apt-get install -y libssl-dev libpcre3 libpcre3-dev apache2-utils zlib1g-dev && \
    mkdir $_RESOURCES_PATH"/openresty" && \
    cd $_RESOURCES_PATH"/openresty" && \
    wget --quiet https://openresty.org/download/openresty-1.17.8.1.tar.gz  -O ./openresty.tar.gz && \
    tar xfz ./openresty.tar.gz && \
    rm ./openresty.tar.gz && \
    cd ./openresty-1.17.8.1/ && \
    # Surpress output - if there is a problem remove  > /dev/null
    ./configure --with-http_stub_status_module --with-http_sub_module > /dev/null && \
    cd $_RESOURCES_PATH"/openresty"/openresty-1.17.8.1/ && \
    make -j2 > /dev/null && \
    make install > /dev/null && \
    # create log dir and file - otherwise openresty will throw an error
    mkdir -p /var/log/nginx/ && \
    touch /var/log/nginx/upstream.log && \
    cd $_RESOURCES_PATH && \
    rm -r $_RESOURCES_PATH"/openresty" && \
    # Fix permissions
    chmod -R a+rwx $_RESOURCES_PATH && \
    # Cleanup
    clean-layer.sh

RUN \
    mkdir $_RESOURCES_PATH/nginx_plugins && \
    mkdir $_RESOURCES_PATH/nginx && \
    mkdir $_RESOURCES_PATH/nginx/conf && \
    mkdir $_RESOURCES_PATH/nginx/logs

## Compile the OpenSSH module manually
## Download from here: https://cdn.openbsd.org/pub/OpenBSD/OpenSSH/portable/
RUN \
    mkdir /var/run/sshd && \
    mkdir /root/.ssh && \
    mkdir /var/lib/sshd && \
    chmod -R 700 /var/lib/sshd/ && \
    chown -R root:sys /var/lib/sshd/ && \
    useradd -r -U -d /var/lib/sshd/ -c "sshd privsep" -s /bin/false sshd && \
    apt-get update && \
    apt-get install -y libssl-dev zlib1g-dev && \
    cd $_RESOURCES_PATH && \
    wget "https://cdn.openbsd.org/pub/OpenBSD/OpenSSH/portable/openssh-8.3p1.tar.gz" && \
    tar xfz openssh-8.3p1.tar.gz && \
    cd $_RESOURCES_PATH/openssh-8.3p1/ && \
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
    pip install --no-cache-dir docker && \
    # Cleanup
    clean-layer.sh

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

# Make supervisor logging dir
RUN mkdir -p /var/log/supervisor/

# Install supervisor for process supervision
RUN \
    apt-get update && \
    # Create sshd run directory - required for starting process via supervisor
    mkdir -p /var/run/sshd && chmod 400 /var/run/sshd && \
    # Install rsyslog for syslog logging
    apt-get install --yes --no-install-recommends rsyslog && \
    pip install --no-cache-dir --upgrade supervisor supervisor-stdout && \
    # Cleanup
    clean-layer.sh

COPY docker-res/ssh/* /etc/ssh/

COPY docker-res/setup_certs.sh docker-res/run.py docker-res/5xx.html docker-res/401.html $_RESOURCES_PATH/

COPY docker-res/supervisord.conf /etc/supervisor/supervisord.conf

# Download jwt-lua libs for the jwt functionality in the nginx proxy
# Override the hmac.lua version bundled in the library with a newer one that fixes some issues
RUN \
	workdir=/lua-jwt-auth && \
	wget -q -P $workdir "https://github.com/SkyLothar/lua-resty-jwt/releases/download/v0.1.11/lua-resty-jwt-0.1.11.tar.gz" && \
	mkdir $workdir/lua-resty-jwt && \
	tar -xf $workdir/lua-resty-jwt-0.1.11.tar.gz --directory $workdir/ && \
	mkdir "$_RESOURCES_PATH/nginx_plugins/jwt_auth/" && \
	cp -r $workdir/lua-resty-jwt-0.1.11/lib/resty "$_RESOURCES_PATH/nginx_plugins/jwt_auth/resty/" && \
	wget -q -P $workdir "https://github.com/auth0/nginx-jwt/releases/download/v1.0.1/nginx-jwt.tar.gz" && \
	mkdir -p $workdir/nginx-jwt && tar -xf $workdir/nginx-jwt.tar.gz --directory $workdir/nginx-jwt && \
	cp $workdir/nginx-jwt/basexx.lua "$_RESOURCES_PATH/nginx_plugins/jwt_auth/" && \
	cp $workdir/nginx-jwt/nginx-jwt.lua "$_RESOURCES_PATH/nginx_plugins/jwt_auth/" && \
	rm -r $workdir && \
	wget -O "$_RESOURCES_PATH/nginx_plugins/jwt_auth/resty/hmac.lua" "https://raw.githubusercontent.com/jkeys089/lua-resty-hmac/989f601acbe74dee71c1a48f3e140a427f2d03ae/lib/resty/hmac.lua"
# Add / override patched nginx-lua files to make them work with our nginx proxy configuration
COPY docker-res/jwt_auth/* "$_RESOURCES_PATH/nginx_plugins/jwt_auth/"
# lua-resty-http is required for the jwt-lua lib
RUN \
	wget -q -P /lua-resty-http "https://github.com/ledgetech/lua-resty-http/archive/v0.12.zip" && \
	unzip -q /lua-resty-http/v0.12.zip -d /lua-resty-http && \
	mkdir -p $_RESOURCES_PATH/nginx_plugins/lua-resty-http/resty/ && \
	cp -R /lua-resty-http/lua-resty-http-0.12/lib/resty/ "$_RESOURCES_PATH/nginx_plugins/lua-resty-http/" && \
	rm -r /lua-resty-http

ARG service_version
ENV SERVICE_VERSION=$service_version \
    PATH=/usr/local/openresty/nginx/sbin:$PATH

# Copy standard key into the container. Can only be used as jump host as the key has as standard command "/bin/true"; see 'authorized_keys' file
#COPY docker-res/ssh/* /root/.ssh/
COPY docker-res/nginx.conf "$_NGINX_CONFIG_PATH"
COPY target/service.jar "$_RESOURCES_PATH/service.jar"

# Copy TOS file
ADD docker-res/terms-of-service.txt "$_RESOURCES_PATH/tos/terms-of-service.txt"

# TODO only required for kubernetes mode
# For Kubernetes: Copy k8s yaml files into ML Lab container which define how other services are started
COPY docker-res/kubernetes/* "$_RESOURCES_PATH/kubernetes/"

ENV SERVICES_RUNTIME="local" \
    LAB_NAMESPACE="lab" \
    LAB_DEBUG="false" \
    LAB_ACTION="install" \
    SERVICES_MEMORY_LIMIT="100" \
    SERVICES_CPU_LIMIT="8" \
    SERVICES_STORAGE_LIMIT="100" \
    MAX_CONTAINER_SIZE="100" \
    WORKSPACE_BACKUP="true" \
    JWT_SECRET="please-change-please-change-please-change" \
    LAB_BASE_URL="" \
    SSH_TARGET_LABELS="lab.feature.type=workspace" \
    SSH_TARGET_PUBLICKEY_API_PORT="8091" \
    SSH_PERMIT_TARGET_HOST="*-workspace-*" \
    SSH_TARGET_KEY_PATH="~/.ssh/id_ed25519.pub"

EXPOSE 8091

# use global option with tini to kill full process groups: https://github.com/krallin/tini#process-group-killing
ENTRYPOINT ["/tini", "-g", "--", "python", "/resources/run.py"]
