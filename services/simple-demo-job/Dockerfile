FROM ubuntu:16.04

# Install basics
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends wget curl unzip \
                        software-properties-common \
                        python3-pip \
	                    apt-transport-https \
                        ca-certificates \
                        sudo && \
    ln -s /usr/bin/pip3 /usr/bin/pip && \
    pip install --upgrade pip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* && \
    ln -s /usr/bin/python3 /usr/bin/python

ENV DATA_ENVIRONMENT="/data" \
    _RESOURCES_PATH="/resources"

RUN \
    mkdir -p $_RESOURCES_PATH && \
    mkdir -p $DATA_ENVIRONMENT && \
    chmod -R a+rwx /data

COPY run.py "$_RESOURCES_PATH/"

ENV CUSTOM_EXIT_CODE="0"
ENV RUN_DURATION_MINUTES="1"

CMD ["python", "/resources/run.py"]