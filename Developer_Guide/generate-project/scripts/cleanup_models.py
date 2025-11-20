#!/usr/bin/env python3
"""
cleanup_models.py

Usage:
    python3 cleanup_models.py /path/to/generated/models

What it does:
- For each .java file in the given directory:
  - Reads package name.
  - Finds "private" field declarations and their types.
  - Finds @JsonProperty("...") usage on getters to preserve JSON property names when present.
  - Backs up original file to filename.java.bak
  - Overwrites the file with a compact Lombok-style class:
      - imports: JsonProperty + Lombok annotations
      - @Data @NoArgsConstructor @AllArgsConstructor @Builder
      - field declarations annotated with @JsonProperty("jsonName")
- Prints a summary of files processed.

Notes:
- This is a heuristic processor for the OpenAPI generator's typical output.
- Check generated code and adjust annotations (e.g., @Embeddable / @Entity) manually where domain-specific mapping is required.
"""

import sys
import re
import os
from pathlib import Path

FIELD_RE = re.compile(r'^\s*private\s+(?:@Nullable\s+)?([A-Za-z0-9_\<\>\.\[\]]+)\s+([A-Za-z0-9_]+)\s*;', re.MULTILINE)
JSONPROP_RE = re.compile(r'@JsonProperty\(\s*"([^"]+)"\s*\)\s*(?:public|protected)\s+[A-Za-z0-9_\<\>\.\[\]]+\s+([A-Za-z0-9_]+)\s*\(')
PACKAGE_RE = re.compile(r'^\s*package\s+([A-Za-z0-9_.]+)\s*;', re.MULTILINE)
CLASSNAME_RE = re.compile(r'public\s+class\s+([A-Za-z0-9_]+)')

LOMBOK_IMPORTS = [
    "lombok.Data",
    "lombok.NoArgsConstructor",
    "lombok.AllArgsConstructor",
    "lombok.Builder"
]

def getter_to_field_name(getter_name: str) -> str:
    """
    Convert getter name like getCreatedBy -> createdBy
    Also handles isX for boolean getters.
    """
    if getter_name.startswith("get") and len(getter_name) > 3:
        core = getter_name[3:]
    elif getter_name.startswith("is") and len(getter_name) > 2:
        core = getter_name[2:]
    else:
        return getter_name
    # lower-case first char
    return core[0].lower() + core[1:] if core else core

def parse_file(text: str):
    """
    Return tuple: (packageName or None, className or None, list of (type,name), jsonprop_map(fieldname->jsonname))
    """
    pkg_m = PACKAGE_RE.search(text)
    pkg = pkg_m.group(1) if pkg_m else None

    cls_m = CLASSNAME_RE.search(text)
    cls = cls_m.group(1) if cls_m else None

    fields = FIELD_RE.findall(text)
    # fields: list of tuples (type, name)

    # find any @JsonProperty occurrences on methods; map to field by converting getter name
    json_map = {}
    for jm in JSONPROP_RE.finditer(text):
        json_name = jm.group(1)
        method_name = jm.group(2)
        field_name = getter_to_field_name(method_name)
        json_map[field_name] = json_name

    return pkg, cls, fields, json_map

def render_class(package, classname, fields, json_map):
    """
    Render the simplified Lombok class as a string.
    """
    lines = []
    if package:
        lines.append(f"package {package};")
        lines.append("") 

    lines.append("import com.fasterxml.jackson.annotation.JsonProperty;")
    lines.append("import lombok.Data;")
    lines.append("import lombok.NoArgsConstructor;")
    lines.append("import lombok.AllArgsConstructor;")
    lines.append("import lombok.Builder;")
    lines.append("")
    lines.append("@Data")
    lines.append("@NoArgsConstructor")
    lines.append("@AllArgsConstructor")
    lines.append("@Builder")
    lines.append(f"public class {classname} " + "{")
    lines.append("")
    # fields is list of (type, name)
    for ftype, fname in fields:
        json_name = json_map.get(fname, fname)
        lines.append(f"    @JsonProperty(\"{json_name}\")")
        lines.append(f"    private {ftype} {fname};")
        lines.append("")
    lines.append("}")
    return "\n".join(lines)

def process_file(path: Path, dry_run=False):
    text = path.read_text(encoding='utf-8')
    package, classname, fields, json_map = parse_file(text)

    if not classname:
        print(f"[skip] {path.name}: could not detect class name")
        return False

    if not fields:
        print(f"[skip] {path.name}: no private fields found - leaving alone")
        return False

    newcode = render_class(package, classname, fields, json_map)

    if dry_run:
        print(f"[DRY] Would overwrite {path} with simplified class (class {classname}, {len(fields)} fields).")
        return True

    # backup original
    bak_path = path.with_suffix(path.suffix + ".bak")
    if not bak_path.exists():
        path.rename(bak_path)
        # write new file
        bak_path.write_text(bak_path.read_text() , encoding='utf-8')  # ensure exists (we just renamed)
        # Actually we renamed, so original path is freed. We will write newcode to path.
    else:
        # if bak exists, leave it and overwrite original
        pass

    # write new content
    path.write_text(newcode, encoding='utf-8')
    print(f"[ok]   {path.name} -> simplified ({len(fields)} fields). backup at {bak_path.name if bak_path.exists() else '(no bak)'}")
    return True

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 cleanup_models.py /path/to/models_dir")
        sys.exit(2)

    dirpath = Path(sys.argv[1])
    if not dirpath.exists() or not dirpath.is_dir():
        print("Path not found or not a directory:", dirpath)
        sys.exit(2)

    java_files = sorted(dirpath.glob("*.java"))
    if not java_files:
        print("No .java files found in", dirpath)
        sys.exit(0)

    processed = []
    skipped = []
    for jf in java_files:
        try:
            ok = process_file(jf)
            if ok:
                processed.append(jf.name)
            else:
                skipped.append(jf.name)
        except Exception as e:
            print(f"[error] {jf.name}: {e}")
            skipped.append(jf.name)

    print("\nSummary:")
    print("Processed:", len(processed))
    for p in processed:
        print("  ", p)
    print("Skipped:", len(skipped))
    for s in skipped:
        print("  ", s)

if __name__ == "__main__":
    main()
