FROM ubuntu:20.04

# Generate and Set locals
# https://stackoverflow.com/questions/28405902/how-to-set-the-locale-inside-a-debian-ubuntu-docker-container#38553499
RUN \
    apt-get update && \
    apt-get install locales --yes && \
    # install locales-all?
    sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen && \
    locale-gen && \
    dpkg-reconfigure --frontend=noninteractive locales && \
    update-locale LANG=en_US.UTF-8

ENV LC_ALL="en_US.UTF-8" \
    LANG="en_US.UTF-8" \
    LANGUAGE="en_US:en" \
    TZ="Europe/Berlin" \
    DEBIAN_FRONTEND="noninteractive"

### Install Python
RUN \
    apt-get update && apt-get install -y python3 python3-pip curl software-properties-common && \
    ln -s /usr/bin/python3 /usr/bin/python && \
    ln -s /usr/bin/pip3 /usr/bin/pip

### Install node & npm (14.6)
RUN \ 
    curl -sSL https://deb.nodesource.com/setup_14.x | bash - && \
    apt-get install -y nodejs

### Install Java
ENV JAVA_HOME="/usr/lib/jvm/java-1.14.0-openjdk-amd64"
RUN \
    add-apt-repository ppa:openjdk-r/ppa && \
    apt-get update && \
    apt-get install -y openjdk-14-jdk && \
    dpkg-query -l

# Install Maven
RUN \
    curl -sSL -o /tmp/apache-maven.tar.gz https://mirrors.ae-online.de/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz && \
    curl -sSL -o /tmp/apache-maven.tar.gz.sha512 https://downloads.apache.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz.sha512 && \
    echo "$(cat /tmp/apache-maven.tar.gz.sha512) /tmp/apache-maven.tar.gz" | sha512sum -c - && \
    mkdir /usr/local/maven && tar -xzf /tmp/apache-maven.tar.gz -C /usr/local/maven --strip-components=1 && \
    rm -f /tmp/apache-maven.tar.gz /tmp/apache-maven.tar.gz.sha512 && \
    ln -s /usr/local/maven/bin/mvn /usr/local/bin/mvn

# Install Docker in Container
RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add - && \
    add-apt-repository \
        "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
        $(lsb_release -cs) \
        stable" && \
    apt-get update -y && \
    apt-get install -y docker-ce=5:19.03.13~3-0~ubuntu-focal

COPY run.sh /run.sh

#WORKDIR "/resources"
# ENTRYPOINT ["python"]
# CMD [ "-u", "/resources/build.py" ]
ENTRYPOINT [ "/bin/bash", "/run.sh" ]
