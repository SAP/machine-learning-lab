FROM mltooling/build-environment:0.4.1

# Install Java
ENV JAVA_HOME="/usr/lib/jvm/java-1.14.0-openjdk-amd64"
RUN \
    add-apt-repository ppa:openjdk-r/ppa \
    && apt-get update \
    && apt-get install -y openjdk-14-jdk \
    && dpkg-query -l

# Install Maven
RUN \
    curl -sSL -o /tmp/apache-maven.tar.gz https://mirrors.ae-online.de/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz \
    && curl -sSL -o /tmp/apache-maven.tar.gz.sha512 https://downloads.apache.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz.sha512 \
    && echo "$(cat /tmp/apache-maven.tar.gz.sha512) /tmp/apache-maven.tar.gz" | sha512sum -c - \
    && mkdir /usr/local/maven && tar -xzf /tmp/apache-maven.tar.gz -C /usr/local/maven --strip-components=1 \
    && rm -f /tmp/apache-maven.tar.gz /tmp/apache-maven.tar.gz.sha512 \
    && ln -s /usr/local/maven/bin/mvn /usr/local/bin/mvn

# Install kind for Kubernetes testing
RUN \
    curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.9.0/kind-linux-amd64 \
    && chmod +x ./kind \
    && mv ./kind /usr/bin/kind
COPY kind-config.yaml /kind-config.yaml

COPY extended-entrypoint.sh /extended-entrypoint.sh

RUN chmod +x /extended-entrypoint.sh

ENTRYPOINT ["/tini", "-g", "--", "/extended-entrypoint.sh"]
