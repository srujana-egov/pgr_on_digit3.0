# Configure Application Properties

## **Overview**

The application.properties file is already populated with default values. Read on to learn how to customise and add extra values for your application (if required).

## **Steps**
Include all the necessary service host URLs and API endpoints in the "application.properties" file.
This guide specifically references the IDGen, Filestore, Boundary and Workflow services that are operational within the DIGIT development environment.

1. Add the following properties to the `application.properties` file. (After your Database Configuration properties)

```properties
# ===============================
# Database Configuration
# ===============================
spring.datasource.url=jdbc:postgresql://localhost:5432/pgrown
spring.datasource.username=postgres
spring.datasource.password=1234
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.properties.hibernate.default_schema=public

# ===============================
# JPA / Hibernate
# ===============================
spring.jpa.hibernate.ddl-auto=update
spring.jpa.generate-ddl=true
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# ===============================
# Kafka (disabled for now)
# ===============================
spring.kafka.enabled=false

server.port = 8083

idgen.templateCode=pgr

pgr.workflow.processCode=PGR6


# ===============================
# Digit Client Library Configuration
# ===============================
# Digit services base URLs for digit-client library
digit.services.boundary.base-url=https://digit-lts.digit.org
digit.services.workflow.base-url=https://digit-lts.digit.org
digit.services.idgen.base-url=https://digit-lts.digit.org
digit.services.notification.base-url=https://digit-lts.digit.org
digit.services.filestore.base-url=https://digit-lts.digit.org
digit.services.timeout.read=30000

# Header propagation settings for digit-client
digit.propagate.headers.allow=authorization,x-correlation-id,x-request-id,x-tenant-id,x-client-id
digit.propagate.headers.prefixes=x-ctx-,x-trace-

# Digit client logging (optional - for debugging)
logging.level.com.digit.services=DEBUG
logging.level.com.digit.http=DEBUG
logging.level.com.digit.config=DEBUG

```
