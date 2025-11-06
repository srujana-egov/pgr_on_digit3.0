# Configure Application Properties

## **Overview**

The application.properties file is already populated with default values. Read on to learn how to customise and add extra values for your application (if required).

## **Steps**
Include all the necessary service host URLs and API endpoints in the "application.properties" file.
This guide specifically references the IDGen, Filestore, Boundary and Workflow services that are operational within the DIGIT development environment.

1. Add the following properties to the `application.properties` file. (After your Database Configuration properties)

```properties
server.port = 8083

# IdGen configs
idgen.host=http://localhost:8100
idgen.generate.endpoint=idgen/v1/generate
idgen.idname=service_request
idgen.templateCode=pgr
idgen.template.endpoint=/template

# Use the correct endpoint for fileStoreId-based lookup
filestore.host=http://localhost:8102
filestore.file.endpoint=/filestore/v1/files/metadata

# Use the correct endpoint for boundary 
boundary.host=http://localhost:8093
boundary.search.endpoint=/boundary/v1

# Notification Service (disabled - using digit-client instead)
# notification.host=http://localhost:8091
# notification.email.endpoint=/notification/v1/email/send
# notification.sms.endpoint=/notification/sms/send

#Workflow service
workflow.host=http://localhost:8085
workflow.transition.post=/workflow/v1/transition
workflow.process.base=workflow/v1/process
pgr.workflow.processId=24198871-ebbc-41ce-857f-dc95ee4c76d9

# ===============================
# Digit Client Library Configuration
# ===============================
# Digit services base URLs for digit-client library
digit.services.boundary.base-url=https://digit-lts.digit.org
digit.services.account.base-url=https://digit-lts.digit.org
digit.services.workflow.base-url=https://digit-lts.digit.org
digit.services.idgen.base-url=https://digit-lts.digit.org
digit.services.notification.base-url=https://digit-lts.digit.org
digit.services.timeout.read=30000

# Header propagation settings for digit-client
digit.propagate.headers.allow=authorization,x-correlation-id,x-request-id,x-tenant-id,x-client-id
digit.propagate.headers.prefixes=x-ctx-,x-trace-

# Digit client logging (optional - for debugging)
logging.level.com.digit.services=DEBUG
logging.level.com.digit.http=DEBUG
logging.level.com.digit.config=DEBUG
```
