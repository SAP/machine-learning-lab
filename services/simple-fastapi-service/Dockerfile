FROM python:3.7

RUN pip install flask && \
    pip install fastapi && \
    pip install uvicorn

COPY docker-res/app.py /app/app.py

RUN chmod 644 /app/app.py

WORKDIR /app

EXPOSE 8080

ENTRYPOINT ["python"]
CMD ["app.py"]
