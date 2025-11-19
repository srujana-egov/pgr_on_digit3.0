#!/usr/bin/env bash
set -euo pipefail

ROOT="generated-pgr"
PKG_BASE_PATH="src/main/java/com/example/pgrown30"
# target path where we want Application.java
OUT_DIR="$ROOT/$PKG_BASE_PATH"
OUT_FILE="$OUT_DIR/Application.java"

# if an OpenApi generated app exists under web package, back it up and remove or keep.
GEN_WEB_APP="$ROOT/$PKG_BASE_PATH/web/OpenApiGeneratorApplication.java"
if [ -f "$GEN_WEB_APP" ]; then
  echo "Backing up generated app $GEN_WEB_APP -> ${GEN_WEB_APP}.bak"
  cp "$GEN_WEB_APP" "${GEN_WEB_APP}.bak"
  # optional: remove it so there's no duplicate
  rm -f "$GEN_WEB_APP"
fi

mkdir -p "$OUT_DIR"
if [ -f "$OUT_FILE" ]; then
  echo "Backing up existing $OUT_FILE -> ${OUT_FILE}.bak"
  cp "$OUT_FILE" "${OUT_FILE}.bak"
fi

cat > "$OUT_FILE" <<'JAVA'
package com.example.pgrown30;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EntityScan(basePackages = "{{ADD YOUR ENTITY SCAN BASE PACKAGES HERE}}")
@EnableJpaRepositories(basePackages = "{{ADD YOUR REPOSITORY BASE PACKAGES HERE}}")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
JAVA

echo "Wrote $OUT_FILE (backup created if previously present)"
