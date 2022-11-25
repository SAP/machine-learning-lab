FROM python:3-slim

WORKDIR /mlflow

RUN apt update && apt install -y gcc git
RUN pip install --no-cache-dir mlflow==1.25.0
ENV PROTOCOL_BUFFERS_PYTHON_IMPLEMENTATION python

ADD mlflow-mllab-plugin mlflow-mllab-plugin
RUN cd mlflow-mllab-plugin && pip install .

EXPOSE 5001

CMD mlflow server --artifacts-destination ml-lab://${HOST}/${PROJECT_ID} --serve-artifacts --backend-store-uri ml-lab://${HOST}/${PROJECT_ID} --port 5001 --static-prefix ${CONTAXY_SERVICE_URL}  --host 0.0.0.0 --default-artifact-root "mlflow-artifacts:/projects/${PROJECT_ID}/services/${CONTAXY_DEPLOYMENT_NAME}/access/5001"

