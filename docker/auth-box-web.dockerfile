FROM azul/zulu-openjdk-debian:11

MAINTAINER dt@temesoft.com

RUN apt-get update && apt-get install -y iputils-ping curl telnet

WORKDIR /
COPY README.md /
COPY auth-box-web/target/auth-box-web.jar /app.jar

RUN useradd -ms /bin/bash auth-box
USER auth-box

EXPOSE 8888

CMD java -jar app.jar