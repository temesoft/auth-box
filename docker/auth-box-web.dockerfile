FROM azul/zulu-openjdk-debian:11

MAINTAINER Dmitriy Temesov <dt@temesoft.com>

WORKDIR /
COPY README.md /
COPY LICENSE /
COPY auth-box-web/target/auth-box-web.jar /app.jar

RUN useradd -ms /bin/bash auth-box
USER auth-box

EXPOSE 8888/tcp

CMD java -jar app.jar