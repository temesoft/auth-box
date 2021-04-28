# AuthBox - Oauth2 server and management panel

AuthBox is a free, open-source Oauth2 server implemented using Java. 
AuthBox consists of 2 applications: Oauth2 server `auth-box-server` and management portal `auth-box-web`.
Management portal provides a UI and restful API for querying and management of Oauth2 server.

## Demo
Full deployment of AuthBox (Oauth2 server and management panel) are running on 
[https://oauth2.cloud](https://oauth2.cloud).

Please create an account to see complete functionality. Registration process will create the following:

* Oauth2 management panel Admin account.
* Client for service-to-service auth (`client_credentials`) which uses standard Oauth2 token.
* Client for user auth (`password`, `authorization_code`, `refresh_token`) which uses JWT (custom RSA private key signed) Oauth2 token.
* One scope which is assigned to both clients.
* Oauth2 user (username: `test`; password: `test`) to demo user authentication or/and authorization.

## Features
AuthBox is [RFC 6749](https://tools.ietf.org/html/rfc6749) compliant Oauth2 server implementation.
It features the following available grant types: `password`, `client_credentials`, `authorization_code`, and `refresh_token`.
As part of `authorization_code` it provides ability to use Two Factor Authentication (2FA) using 
[Google Authenticator](https://support.google.com/accounts/answer/1066447).

By default, Oauth2 server `auth-box-server` and management portal `auth-box-web` utilize 
[MySql](https://www.mysql.com/) for data storage, and optionally [Redis](https://redis.io/) for 
DAO caching and web session store, and optionally [Grafana](https://grafana.com/) / [Graphite](https://graphiteapp.org/) 
for metrics collection and visualization.

`auth-box-server` and `auth-box-web` are docker/k8s ready and come with [Dockerfile(s)](docker/) and [docker-compose](docker/) scripts.

## Application configuration setup
Oauth2 server `auth-box-server` and management portal `auth-box-web` use [spring-boot](https://spring.io/projects/spring-boot)
internally and therefore can be configured using following methods:

Injecting custom properties using `custom.properties` file

```shell script
# When running application from command line using Java executable
java -jar auth-box-server.jar --spring.config.location=/some/where/custom.properties
# or
java -Dspring.config.location=/some/where/custom.properties -jar auth-box-server.jar
```    

Injecting individual custom properties, for example `server.port=12345`

```shell script
# When running application from command line using Java executable
java -jar auth-box-server.jar --server.port=12345
# or
java -Dserver.port=12345 -jar auth-box-server.jar
# or using environment variables (note: config property names should be in all caps and "_" instead of ".")
export SERVER_PORT=12345 
java -jar auth-box-server.jar
```    

## Oauth2 server `auth-box-server` configuration
| Configuration property | Description | Default value |
| :--- | :--- | :--- |
| server.port | Server listening port | 9999 |
| spring.datasource.url | Database JDBC url | jdbc:mysql://${MYSQL_HOST:localhost}:3306/authbox?serverTimezone=UTC&useLegacyDatetimeCode=false |
| spring.datasource.username | Database username | root |
| spring.datasource.password | Database password | r00t |
| spring.flyway.enabled | Flyway database migration flag | true |
| spring.cache.type | DAO cache type (possible values are: caffeine/redis/none) | none |
| spring.cache.cache-names | Cache names to enable in csv list (possible values are OauthClient,OauthScope,OauthToken,OauthUser,Organization,User) | N/A |
| spring.redis.host | Redis cache host (disabled when not specified) | N/A |


## Build and run

    # Run maven clean package
    mvn clean package
    
    # Build auth-box-server docker container
    docker build -f docker/auth-box-server.dockerfile -t auth-box-server .
    
    # Build auth-box-web docker container
    docker build -f docker/auth-box-web.dockerfile -t auth-box-web .
    
    # Remove old container data
    docker-compose -f docker/demo-docker-compose.yml rm -f
    
    # Start demo: mysql, redis, auth-box-web, auth-box-server 
    docker-compose -f docker/demo-docker-compose.yml up
    
    # MySql container (standalone)
    docker run -p 3306:3306 --rm --name mysql -e MYSQL_ROOT_PASSWORD=r00t -e MYSQL_DATABASE=authbox -it mysql:latest
    
    # Redis container (standalone)
    docker run --name some-redis -p 6379:6379 --rm -it redis
    
    # Graphite/Grafana docker-compose containers (standalone)
    docker-compose -f docker/metrics-docker-compose.yml up
    

### RSA 2048 key pair creation

    openssl genrsa -out private.pem 2048
    openssl rsa -in private.pem -outform PEM -pubout -out public.pem

