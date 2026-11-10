#!/usr/bin/env bash
# One-command verification: common library build, both Java test suites, frontend build.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_DIR="$(dirname "$SCRIPT_DIR")"
WORKSPACE_DIR="$(dirname "$SERVICE_DIR")"
COMMON_DIR="$WORKSPACE_DIR/octopus-demo-java-common"
API_DIR="$WORKSPACE_DIR/octopus-demo-api-admin"
WEB_DIR="$WORKSPACE_DIR/octopus-demo-web-admin"

log() { echo "[verify] $*"; }
fail() { echo "[verify] FAILED: $*" >&2; exit 1; }

trap 'fail "aborted at line $LINENO"' ERR

log "1/4 octopus-demo-java-common: mvn clean install"
mvn -B -f "$COMMON_DIR/pom.xml" clean install

log "2/4 octopus-demo-service-admin: mvn test"
mvn -B -f "$SERVICE_DIR/pom.xml" test -Dtest="*Test,*IT"

log "3/4 octopus-demo-api-admin: mvn test"
mvn -B -f "$API_DIR/pom.xml" test -Dtest="*Test,*IT"

log "4/4 octopus-demo-web-admin: npm ci && npm run build"
( cd "$WEB_DIR" && npm ci --no-audit --no-fund && npm run build )

log "all checks passed"
