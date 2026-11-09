#!/usr/bin/env bash
# Brings up MySQL + service-admin + api-admin + web-admin for local development.
# Usage: bash scripts/dev-up.sh   (from any directory)
# Port overrides: MYSQL_PORT, SERVICE_ADMIN_PORT, API_ADMIN_PORT, WEB_ADMIN_PORT
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_DIR="$(dirname "$SCRIPT_DIR")"
WORKSPACE_DIR="$(dirname "$SERVICE_DIR")"
COMMON_DIR="$WORKSPACE_DIR/octopus-demo-java-common"
API_DIR="$WORKSPACE_DIR/octopus-demo-api-admin"
WEB_DIR="$WORKSPACE_DIR/octopus-demo-web-admin"
RUN_DIR="$SCRIPT_DIR/.run"

export MYSQL_PORT="${MYSQL_PORT:-13306}"
export SERVICE_ADMIN_PORT="${SERVICE_ADMIN_PORT:-18080}"
export API_ADMIN_PORT="${API_ADMIN_PORT:-18081}"
export WEB_ADMIN_PORT="${WEB_ADMIN_PORT:-13000}"

log() { echo "[dev-up] $*"; }
fail() { echo "[dev-up] FAILED: $*" >&2; exit 1; }

# MySQL runs its init script for tens of seconds after `docker compose up -d` returns;
# service-admin fails fast if its DataSource cannot connect, so wait for the ping first.
wait_for_mysql() {
    local deadline=$((SECONDS + 120))
    while ((SECONDS < deadline)); do
        if ( cd "$SERVICE_DIR" && docker compose exec -T mysql \
                mysqladmin ping -h 127.0.0.1 -u root -p"${MYSQL_ROOT_PASSWORD:-root}" --silent >/dev/null 2>&1 ); then
            return 0
        fi
        sleep 2
    done
    fail "MySQL did not become ready within 120s"
}

mkdir -p "$RUN_DIR"

log "1/5 installing octopus-demo-java-common"
mvn -B -f "$COMMON_DIR/pom.xml" clean install -DskipTests || fail "mvn clean install octopus-demo-java-common"

log "2/5 starting MySQL on host port $MYSQL_PORT"
( cd "$SERVICE_DIR" && docker compose up -d mysql ) || fail "docker compose up mysql"
wait_for_mysql
log "MySQL ready on host port $MYSQL_PORT"

log "3/5 packaging service-admin"
mvn -B -f "$SERVICE_DIR/pom.xml" -DskipTests package || fail "service-admin package"

log "4/5 packaging api-admin"
mvn -B -f "$API_DIR/pom.xml" -DskipTests package || fail "api-admin package"

log "5/5 starting services"
SERVICE_JAR="$(ls "$SERVICE_DIR"/target/octopus-demo-service-admin-*.jar 2>/dev/null | head -n 1)"
[[ -n "$SERVICE_JAR" ]] || fail "service-admin jar not found"
API_JAR="$(ls "$API_DIR"/target/octopus-demo-api-admin-*.jar 2>/dev/null | head -n 1)"
[[ -n "$API_JAR" ]] || fail "api-admin jar not found"

nohup java -jar "$SERVICE_JAR" > "$RUN_DIR/service-admin.log" 2>&1 &
echo $! > "$RUN_DIR/service-admin.pid"
log "service-admin started (pid $(cat "$RUN_DIR/service-admin.pid")) -> :$SERVICE_ADMIN_PORT"

SERVER_PORT="$API_ADMIN_PORT" SERVICE_ADMIN_PORT="$SERVICE_ADMIN_PORT" \
    nohup java -jar "$API_JAR" > "$RUN_DIR/api-admin.log" 2>&1 &
echo $! > "$RUN_DIR/api-admin.pid"
log "api-admin started (pid $(cat "$RUN_DIR/api-admin.pid")) -> :$API_ADMIN_PORT"

log "installing web-admin npm dependencies"
( cd "$WEB_DIR" && npm ci --no-audit --no-fund ) || fail "web-admin npm ci"

(
    cd "$WEB_DIR"
    nohup npm run dev -- --port "$WEB_ADMIN_PORT" > "$RUN_DIR/web-admin.log" 2>&1 &
    echo $! > "$RUN_DIR/web-admin.pid"
)
log "web-admin started (pid $(cat "$RUN_DIR/web-admin.pid")) -> :$WEB_ADMIN_PORT"

log "all processes launched. Run: bash scripts/dev-ready.sh"
