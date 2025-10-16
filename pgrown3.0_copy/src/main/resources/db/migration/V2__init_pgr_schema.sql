-- Drop old tables if you don’t need them anymore
DROP TABLE IF EXISTS eg_pgr_audit CASCADE;
DROP TABLE IF EXISTS eg_pgr_document CASCADE;
DROP TABLE IF EXISTS eg_pgr_workflow CASCADE;
DROP TABLE IF EXISTS eg_pgr_address CASCADE;
DROP TABLE IF EXISTS eg_pgr_service CASCADE;

-- Create the correct citizen_service schema
CREATE TABLE citizen_service (
    service_request_id  VARCHAR(128) PRIMARY KEY,
    tenant_id           VARCHAR(64) NOT NULL,
    service_code        VARCHAR(64) NOT NULL,
    description         VARCHAR(256),
    account_id          VARCHAR(64),
    application_status  VARCHAR(64),
    source              VARCHAR(64),
    additional_detail   JSONB,
    created_by          VARCHAR(64),
    last_modified_by    VARCHAR(64),
    created_time        BIGINT,
    last_modified_time  BIGINT
);

CREATE TABLE citizen_address (
    id                  VARCHAR(64) PRIMARY KEY,
    service_request_id  VARCHAR(128) NOT NULL,
    address             VARCHAR(256),
    city                VARCHAR(64),
    pincode             VARCHAR(64),
    latitude            DECIMAL(10,8),
    longitude           DECIMAL(11,8),
    created_by          VARCHAR(64),
    last_modified_by    VARCHAR(64),
    created_time        BIGINT,
    last_modified_time  BIGINT,
    CONSTRAINT fk_citizen_address_service FOREIGN KEY (service_request_id) REFERENCES citizen_service (service_request_id) ON DELETE CASCADE
);

CREATE TABLE citizen_workflow (
    id                  VARCHAR(64) PRIMARY KEY,
    service_request_id  VARCHAR(128) NOT NULL,
    action              VARCHAR(64),
    assignees           TEXT[],
    comments            VARCHAR(256),
    verification_docs   JSONB,
    created_by          VARCHAR(64),
    last_modified_by    VARCHAR(64),
    created_time        BIGINT,
    last_modified_time  BIGINT,
    CONSTRAINT fk_citizen_workflow_service FOREIGN KEY (service_request_id) REFERENCES citizen_service (service_request_id) ON DELETE CASCADE
);

CREATE TABLE citizen_document (
    id                  VARCHAR(64) PRIMARY KEY,
    service_request_id  VARCHAR(128) NOT NULL,
    document_type       VARCHAR(64),
    file_store_id       VARCHAR(128),
    document_uid        VARCHAR(128),
    created_by          VARCHAR(64),
    last_modified_by    VARCHAR(64),
    created_time        BIGINT,
    last_modified_time  BIGINT,
    CONSTRAINT fk_citizen_document_service FOREIGN KEY (service_request_id) REFERENCES citizen_service (service_request_id) ON DELETE CASCADE
);

CREATE TABLE citizen_audit (
    id                  VARCHAR(64) PRIMARY KEY,
    service_request_id  VARCHAR(128) NOT NULL,
    action              VARCHAR(64),
    status              VARCHAR(64),
    performed_by        VARCHAR(64),
    performed_time      BIGINT,
    remarks             VARCHAR(256),
    CONSTRAINT fk_citizen_audit_service FOREIGN KEY (service_request_id) REFERENCES citizen_service (service_request_id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_citizen_service_tenant ON citizen_service (tenant_id);
CREATE INDEX idx_citizen_service_account ON citizen_service (account_id);
CREATE INDEX idx_citizen_service_status ON citizen_service (application_status);
CREATE INDEX idx_citizen_workflow_service ON citizen_workflow (service_request_id);
CREATE INDEX idx_citizen_document_service ON citizen_document (service_request_id);
CREATE INDEX idx_citizen_audit_service ON citizen_audit (service_request_id);
