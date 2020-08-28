FROM python:2.7

RUN pip install flask && \
    pip install flask_restful

COPY docker-res/app.py /app/app.py
WORKDIR /app

EXPOSE 1234

ENTRYPOINT ["python"]
CMD ["app.py"]
