FROM azul/zulu-openjdk-debian:11

MAINTAINER dt@temesoft.com

RUN apt-get update && apt-get install -y iputils-ping curl telnet

WORKDIR /
COPY README.md /
COPY auth-box-server/target/auth-box-server.jar /app.jar

RUN useradd -ms /bin/bash auth-box
USER auth-box

EXPOSE 9999

CMD java -jar app.jar