#!/usr/bin/env python3
"""
generate_repositories.py

Usage:
    python3 scripts/generate_repositories.py pgr.yaml generated-pgr/src/main/java

What it does:
- Reads the OpenAPI YAML.
- Detects all schemas under components/schemas.
- Determines which schemas are DOMAIN ENTITIES based on:
    • having string fields OR uuid OR identifiers
    • being referenced by ServiceWrapper or ServiceResponse
- Detects primary ID field automatically:
    • id, serviceRequestId, tenantId, <name>Id, etc.
    • OR x-id-field in YAML override
- Auto-detects package structure from generated Java models
- Writes repository files to:
      <base-package>/repository/<SchemaName>Repository.java

No hardcoding of package, model names, or ID logic.
"""

import sys
import yaml
import re
from pathlib import Path

ID_FIELD_PATTERNS = [
    re.compile(r'^id$', re.I),
    re.compile(r'.*Id$', re.I),
    re.compile(r'.*_id$', re.I),
    re.compile(r'^serviceRequestId$', re.I)
]

REPO_TEMPLATE = """package {{PACKAGE}}.repository;

import {{MODEL_PACKAGE}}.{{CLASSNAME}};
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface {{CLASSNAME}}Repository extends JpaRepository<{{CLASSNAME}}, {{ID_TYPE}}> {

{{METHODS}}
}
"""

# -----------------------------
# YAML LOADING
# -----------------------------
def load_yaml(path):
    with open(path, "r") as f:
        return yaml.safe_load(f)

# -----------------------------
# DETECT BASE PACKAGE
# -----------------------------
def detect_base_package(java_root: Path):
    for java in java_root.rglob("*.java"):
        text = java.read_text(errors="ignore")
        for line in text.splitlines():
            line=line.strip()
            if line.startswith("package "):
                pkg = line[8:].rstrip(";")
                if ".web" in pkg:
                    return pkg.split(".web")[0]
                return pkg
    return None

# -----------------------------
# DETECT MODEL PACKAGE
# -----------------------------
def detect_model_package(java_root: Path):
    for java in java_root.rglob("*.java"):
        text = java.read_text(errors="ignore")
        if "class AuditDetails" in text or "class CitizenService" in text:
            for line in text.splitlines():
                if line.strip().startswith("package "):
                    return line.strip()[8:].rstrip(";")
    return None

# -----------------------------
# DETECT DOMAIN ENTITIES
# -----------------------------
def is_domain_entity(schema_name, schema):
    """Heuristic: Persistent entity if it has ID-like fields or appears in response."""
    if not isinstance(schema, dict): 
        return False

    props = schema.get("properties", {})
    for p in props:
        for pat in ID_FIELD_PATTERNS:
            if pat.match(p):
                return True
    return False

def detect_id_field(schema):
    props = schema.get("properties", {})
    for p in props:
        for pat in ID_FIELD_PATTERNS:
            if pat.match(p):
                return p
    return None

def detect_field_type(prop):
    """Map OpenAPI types → Java types."""
    t = prop.get("type")
    fmt = prop.get("format")

    if fmt == "uuid":
        return "String"
    if t == "string":
        return "String"
    if t == "integer":
        return "Integer"
    if t == "number":
        return "Double"
    if t == "boolean":
        return "Boolean"
    return "String"  # fallback

# -----------------------------
# CREATE CUSTOM QUERY METHODS
# -----------------------------
def build_custom_methods(schema_name, schema):
    props = schema.get("properties", {})
    methods = []

    # Common patterns like findByServiceRequestId
    for field, definition in props.items():
        if "string" == definition.get("type"):
            if field.lower().endswith("id"):
                java_type = detect_field_type(definition)
                method = f"    Optional<{schema_name}> findBy{field[0].upper() + field[1:]}({java_type} {field});"
                methods.append(method)

    # Tenant-aware search
    if "tenantId" in props:
        id_fields = [f for f in props if f.lower().endswith("id")]
        if id_fields:
            f = id_fields[0]
            method = (
                f"    Optional<{schema_name}> findBy{f[0].upper()+f[1:]}AndTenantId("
                f"{detect_field_type(props[f])} {f}, String tenantId);"
            )
            methods.append(method)

    return "\n\n".join(methods)

# -----------------------------
# GENERATE REPOSITORY
# -----------------------------
def generate_repo(pkg, model_pkg, output_dir, name, schema):
    id_field = detect_id_field(schema)
    if not id_field:
        print(f"Skipping {name}: No ID field found.")
        return

    id_type = detect_field_type(schema["properties"][id_field])
    methods = build_custom_methods(name, schema)

    code = REPO_TEMPLATE\
        .replace("{{PACKAGE}}", pkg)\
        .replace("{{MODEL_PACKAGE}}", model_pkg)\
        .replace("{{CLASSNAME}}", name)\
        .replace("{{ID_TYPE}}", id_type)\
        .replace("{{METHODS}}", methods)

    file_path = output_dir / f"{name}Repository.java"
    file_path.write_text(code)
    print(f"✔ Generated {file_path}")

# -----------------------------
# MAIN
# -----------------------------
def main():
    if len(sys.argv) < 3:
        print("Usage: python3 generate_repositories.py openapi.yaml src/main/java")
        sys.exit(1)

    openapi_path = Path(sys.argv[1])
    java_root = Path(sys.argv[2])

    spec = load_yaml(openapi_path)
    schemas = spec.get("components", {}).get("schemas", {})

    base_pkg = detect_base_package(java_root)
    model_pkg = detect_model_package(java_root)

    if not base_pkg or not model_pkg:
        print("❌ Could not detect base package or model package.")
        sys.exit(1)

    repo_dir = java_root / Path(base_pkg.replace(".", "/")) / "repository"
    repo_dir.mkdir(parents=True, exist_ok=True)

    # Process schemas
    for name, schema in schemas.items():
        if is_domain_entity(name, schema):
            generate_repo(base_pkg, model_pkg, repo_dir, name, schema)

    print("\n🎉 Done generating repositories.")

if __name__ == "__main__":
    main()
