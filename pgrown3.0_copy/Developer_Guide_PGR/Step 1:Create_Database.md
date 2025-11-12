# Create and Configure Database

## **Steps**
1. Configure the below properties in the application.properties file to enable flyway migration:

```properties
#DATABASE CONFIGURATION

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

```

2. Add the Flyway SQL scripts in the following structure under `resources/db/migration/main`:

![](https://3868804918-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2FegsIWleSdyH9rMLJ8ShI%2Fuploads%2Fgit-blob-0cc0e9ee0f3e0cff7b0322e958c9e6d3291701d7%2Fimage.png?alt=media)

3. Add the migration files to the *main* folder. Follow the specified nomenclature while naming the file. The file name should be in the following format:

```
V[YEAR][MONTH][DAY][HR][MIN][SEC]__modulecode_ …_ddl.sql

```

Example: **V20251106123100\_\_pgr\_publicgrievanceredressal\_ddl.sql**

For this PGR service, use the following SQL script to create the required tables.

```plsql
-- Create enum types
DO $$
BEGIN
    -- Status enum from OpenAPI spec
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'pgr_status') THEN
        CREATE TYPE pgr_status AS ENUM ('OPEN', 'ASSIGNED', 'PROCESSING', 'RESOLVED', 'REJECTED', 'CLOSED');
    END IF;
    
    -- Priority enum
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'pgr_priority') THEN
        CREATE TYPE pgr_priority AS ENUM ('HIGH', 'MEDIUM', 'LOW');
    END IF;
    
    -- Gender enum from Citizen schema
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'gender_enum') THEN
        CREATE TYPE gender_enum AS ENUM ('MALE', 'FEMALE', 'OTHER', 'UNKNOWN');
    END IF;
END
$$;

-- Main service request table
CREATE TABLE IF NOT EXISTS pgr_service_requests (
    id VARCHAR(64) PRIMARY KEY,
    service_code VARCHAR(64) NOT NULL,
    service_request_id VARCHAR(64) GENERATED ALWAYS AS (id) STORED,
    tenant_id VARCHAR(64) NOT NULL,
    account_id VARCHAR(64) NOT NULL,
    description TEXT NOT NULL,
    source VARCHAR(100),
    status pgr_status NOT NULL DEFAULT 'OPEN',
    priority pgr_priority DEFAULT 'MEDIUM',
    application_status VARCHAR(100),
    
    -- Citizen details (denormalized for performance)
    citizen_name VARCHAR(255),
    citizen_mobile VARCHAR(15) NOT NULL,
    citizen_email VARCHAR(255),
    citizen_gender gender_enum,
    
    -- Address details (denormalized from address object)
    door_no VARCHAR(50),
    plot_no VARCHAR(50),
    landmark VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100),
    region VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100) DEFAULT 'India',
    pincode VARCHAR(10) NOT NULL,
    building_name VARCHAR(255),
    street VARCHAR(255),
    locality_code VARCHAR(100),
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    
    -- Audit fields
    created_by VARCHAR(64) NOT NULL,
    created_time BIGINT NOT NULL,
    last_modified_by VARCHAR(64),
    last_modified_time BIGINT,
    
    -- Additional details as JSONB for flexible schema
    additional_details JSONB,
    address_details JSONB,
    
    -- Constraints
    UNIQUE(tenant_id, id)
);

-- Workflow history table
CREATE TABLE IF NOT EXISTS pgr_workflow_history (
    id VARCHAR(64) PRIMARY KEY,
    service_request_id VARCHAR(64) NOT NULL,
    action VARCHAR(100) NOT NULL,
    status pgr_status NOT NULL,
    assignees TEXT[],
    comments TEXT,
    created_by VARCHAR(64) NOT NULL,
    created_time BIGINT NOT NULL,
    additional_details JSONB,
    FOREIGN KEY (service_request_id) REFERENCES pgr_service_requests(id) ON DELETE CASCADE
);

-- Documents table
CREATE TABLE IF NOT EXISTS pgr_documents (
    id VARCHAR(64) PRIMARY KEY,
    service_request_id VARCHAR(64) NOT NULL,
    document_type VARCHAR(100) NOT NULL,
    file_store_id VARCHAR(255) NOT NULL,
    document_uid VARCHAR(255),
    created_by VARCHAR(64) NOT NULL,
    created_time BIGINT NOT NULL,
    additional_details JSONB,
    FOREIGN KEY (service_request_id) REFERENCES pgr_service_requests(id) ON DELETE CASCADE
);

-- Indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_pgr_tenant_id ON pgr_service_requests(tenant_id);
CREATE INDEX IF NOT EXISTS idx_pgr_status ON pgr_service_requests(status);
CREATE INDEX IF NOT EXISTS idx_pgr_application_status ON pgr_service_requests(application_status);
CREATE INDEX IF NOT EXISTS idx_pgr_citizen_mobile ON pgr_service_requests(citizen_mobile);
CREATE INDEX IF NOT EXISTS idx_pgr_locality_code ON pgr_service_requests(locality_code);
CREATE INDEX IF NOT EXISTS idx_pgr_created_time ON pgr_service_requests(created_time);
CREATE INDEX IF NOT EXISTS idx_pgr_workflow_service_request ON pgr_workflow_history(service_request_id);
CREATE INDEX IF NOT EXISTS idx_pgr_documents_service_request ON pgr_documents(service_request_id);

-- Comments for documentation
COMMENT ON TABLE pgr_service_requests IS 'Stores all public grievance redressal service requests';
COMMENT ON COLUMN pgr_service_requests.additional_details IS 'Stores any additional dynamic fields as JSON';
COMMENT ON TABLE pgr_workflow_history IS 'Tracks workflow state changes for service requests';
COMMENT ON TABLE pgr_documents IS 'Stores document references for service requests';
```
3. Create the Database
Run this command in your PostgreSQL client (like psql or pgAdmin):

Note: If using psql: psql -U postgres

```sql
CREATE DATABASE eg_pgr;
```
4. Run the application and verify the database to check if the tables were generated correctly.

To run:
mvn clean install
mvn spring-boot:run

To verify:
```bash
# Connect to PostgreSQL
psql -U postgres -d eg_pgr

# List all tables
\dt

# Check flyway_schema_history
SELECT * FROM flyway_schema_history;

# Exit psql
\q
```

