FROM azul/zulu-openjdk-debian:11

MAINTAINER Dmitriy Temesov <dt@temesoft.com>

WORKDIR /
COPY README.md /
COPY LICENSE /
COPY auth-box-server/target/auth-box-server.jar /app.jar

RUN useradd -ms /bin/bash auth-box
USER auth-box

EXPOSE 9999/tcp

CMD java -jar app.jar