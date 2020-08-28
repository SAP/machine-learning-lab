FROM docker.wdf.sap.corp:51150/com.sap.sapai.studio/simple-demo-job:latest as base

RUN whereis python

RUN find / -name "libexpat.so.1"

FROM gcr.io/distroless/base-debian10

COPY --from=base /resources /resources

# Copy pip-installed packages
COPY --from=base /usr/lib/python3/dist-packages /usr/lib/python3/dist-packages
COPY --from=base /usr/bin/pip /usr/bin/pip

# Copy python
COPY --from=base /usr/bin/python /usr/bin/python
COPY --from=base /usr/bin/python3.5 /usr/bin/python3.5
COPY --from=base /usr/lib/python3.5 /usr/lib/python3.5
# /etc/python3.5 /usr/local/lib/python3.5

# Copy shared libraries
COPY --from=base /lib/x86_64-linux-gnu/libexpat.so.1 /lib/x86_64-linux-gnu/libexpat.so.1
COPY --from=base /lib/x86_64-linux-gnu/libz.so.1 /lib/x86_64-linux-gnu/libz.so.1
# COPY --from=base /lib/x86_64-linux-gnu/libc-bin /lib/x86_64-linux-gnu/libc-bin.so.1
# COPY --from=base /lib/x86_64-linux-gnu/libdb5.3 /lib/x86_64-linux-gnu/libdb5.3.so.1
# COPY --from=base /lib/x86_64-linux-gnu/liblzma5 /lib/x86_64-linux-gnu/liblzma5.so.1
# COPY --from=base /lib/x86_64-linux-gnu/libmpdec2 /lib/x86_64-linux-gnu/libmpdec2.so.1
# COPY --from=base /lib/x86_64-linux-gnu/libreadline7 /lib/x86_64-linux-gnu/libreadline7.so.1
# COPY --from=base /lib/x86_64-linux-gnu/libsqlite3-0 /lib/x86_64-linux-gnu/libsqlite3-0.so.1
# COPY --from=base /lib/x86_64-linux-gnu/libssl1.1 /lib/x86_64-linux-gnu/libssl1.1.so.1
# COPY --from=base /lib/x86_64-linux-gnu/zlib1g /lib/x86_64-linux-gnu/zlib1g.so.1

#RUN python --version

ENTRYPOINT [ "python" ]
#CMD [ "-c", "print('foo')" ]
CMD ["/resources/run.py"]
