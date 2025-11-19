# Generate Project Using API Specs - In 1 command! (Almost)

## Overview

This page provides detailed steps for generating projects using the given API specifications via OpenAPI generator. 

## Steps

### 0. Clone this repo.

### 1. Prepare Swagger contracts

Prepare Swagger contracts that define all the APIs that the service will expose for external consumption.

Use this contract for this PGR tutorial:  
https://github.com/srujana-egov/pgr_on_digit3.0/blob/developer-guide/pgr3.0.yaml

### 2. Download the codegen tool

Download the codegen tool and move it to your created folder:  
https://github.com/egovernments/Digit-Core/blob/codegen-openapi-3.0-Core-2.9-lts/accelerators/codegen/codegen-2.0-SNAPSHOT-jar-with-dependencies.jar

### 3. Navigate to the folder

In your terminal, go to the folder where you’ve placed both the codegen JAR and your Swagger contract.

### 4. Generate the API skeleton

Use the generic command below to create an API skeleton for any Swagger contract:

```bash
java -jar codegen-2.0-SNAPSHOT-jar-with-dependencies.jar \
  -l -t \
  -u /path/to/your/swagger-contract.yaml \
  -a {project_name} \
  -b digit.{project_name}
````

* `-a` sets the artifact name.
* `-b` tells the generator which base templates to use under `src/main/resources/templates/`.
  That means: "Generate this service using the DIGIT framework conventions."

**Example:**

```bash
java -jar codegen-2.0-SNAPSHOT-jar-with-dependencies.jar \
  -l -t \
  -u /Users/srujana/Desktop/pgr3.0.yaml \
  -a pgr3_0 \
  -b digit.pgr3_0
```

### 5. Rename the output folder

Rename the generated `output` folder to `PGR3.0`.

### 6. Import into your IDE

Open the renamed project (`PGR3.0`) in your preferred IDE.

### 7. Update `pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.6</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>pgrown3.0</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>pgrown3.0</name>
    <description>Demo project for Spring Boot</description>

    <properties>
        <java.version>17</java.version>
        <lombok.version>1.18.32</lombok.version>
    </properties>

    <!-- ✅ Testcontainers BOM for version alignment -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>1.20.1</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Jakarta Persistence API -->
        <dependency>
            <groupId>jakarta.persistence</groupId>
            <artifactId>jakarta.persistence-api</artifactId>
            <version>3.1.0</version>
        </dependency>

        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-security</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>42.7.3</version>
        </dependency>

        <!-- Flyway (PostgreSQL) -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
            <version>11.7.2</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Testcontainers -->
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>kafka</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- In-memory DB for tests -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Digit Client Library -->
        <dependency>
            <groupId>com.digit</groupId>
            <artifactId>digit-client</artifactId>
            <version>1.0.0</version>
            <scope>system</scope>
            <systemPath>${project.basedir}/../digit-client-1.0.0.jar</systemPath>
        </dependency>
        <!-- Security + JWT resource server (validates tokens, fetches JWKs, etc.) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>
        <!-- Optional: if not pulled transitively -->
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-oauth2-jose</artifactId>
        </dependency>
        
        <!-- Minimal OpenTelemetry API (no-op implementation) -->
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-api</artifactId>
            <version>1.32.0</version>
        </dependency>
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-context</artifactId>
            <version>1.32.0</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Flyway Maven Plugin -->
            <plugin>
                <groupId>org.flywaydb</groupId>
                <artifactId>flyway-maven-plugin</artifactId>
                <version>11.7.2</version>
                <configuration>
                    <url>jdbc:postgresql://localhost:5432/pgrown</url>
                    <user>postgres</user>
                    <password>password</password>
                    <schemas>
                        <schema>public</schema>
                    </schemas>
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>org.postgresql</groupId>
                        <artifactId>postgresql</artifactId>
                        <version>42.7.3</version>
                    </dependency>
                </dependencies>
            </plugin>

            <!-- Compiler Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>

            <!-- Spring Boot Plugin -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>

```
### 8. Add Client Library jar file in your initial created folder.
Link here: https://github.com/srujana-egov/pgr_on_digit3.0/blob/demo/digit-client-1.0.0.jar

Note: Preferably delete all files generated by codegen at this point to avoid dependency errors. We will build the project from scratch.

### 9. Perform a maven update once the spring boot version is updated.
Command: mvn -U clean install
