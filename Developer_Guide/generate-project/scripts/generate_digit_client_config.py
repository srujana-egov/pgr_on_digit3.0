#!/usr/bin/env python3
"""
generate_digit_client_config.py

Usage:
    python3 scripts/generate_digit_client_config.py generated-pgr/src/main/java

Auto-detects the base package by scanning for "*.java" under the given root.
Creates:
    <base-package>/config/DigitClientConfig.java
"""

import sys
import os
from pathlib import Path

TEMPLATE = """package {{PACKAGE}}.config;

import com.digit.config.ApiConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-generated Digit Client configuration.
 * Provides RestTemplate with header propagation.
 */
@Configuration
@Import(ApiConfig.class)
public class DigitClientConfig {
}
"""

def detect_base_package(root: Path):
    """Find the package by searching a model/controller file inside the tree."""
    for java in root.rglob("*.java"):
        text = java.read_text(errors="ignore")
        for line in text.splitlines():
            if line.strip().startswith("package "):
                pkg = line.strip()[8:].rstrip(";").strip()
                # Return the prefix before ".web" or ".controllers" etc.
                if ".web" in pkg:
                    return pkg.split(".web")[0]
                return pkg
    return None

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 generate_digit_client_config.py <src/main/java>")
        sys.exit(1)

    root = Path(sys.argv[1])
    base_pkg = detect_base_package(root)

    if not base_pkg:
        print("❌ Could not detect base package.")
        sys.exit(1)

    target_dir = root / Path(base_pkg.replace(".", "/")) / "config"
    target_dir.mkdir(parents=True, exist_ok=True)

    target_file = target_dir / "DigitClientConfig.java"
    content = TEMPLATE.replace("{{PACKAGE}}", base_pkg)

    target_file.write_text(content)
    print("✅ Generated:", target_file)

if __name__ == "__main__":
    main()
