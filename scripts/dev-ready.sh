#!/usr/bin/env bash
# Waits until service-admin, api-admin and web-admin answer, failing after READY_TIMEOUT seconds.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_ADMIN_PORT="${SERVICE_ADMIN_PORT:-18080}"
API_ADMIN_PORT="${API_ADMIN_PORT:-18081}"
WEB_ADMIN_PORT="${WEB_ADMIN_PORT:-13000}"
READY_TIMEOUT="${READY_TIMEOUT:-300}"

wait_for_http() {
    local name="$1" url="$2"
    local deadline=$((SECONDS + READY_TIMEOUT))
    while ((SECONDS < deadline)); do
        if curl -fsS -o /dev/null "$url" 2>/dev/null; then
            echo "[dev-ready] $name ready: $url"
            return 0
        fi
        sleep 2
    done
    echo "[dev-ready] TIMEOUT after ${READY_TIMEOUT}s waiting for $name at $url" >&2
    return 1
}

wait_for_code_200() {
    local name="$1" url="$2"
    local deadline=$((SECONDS + READY_TIMEOUT))
    while ((SECONDS < deadline)); do
        if curl -fsS "$url" 2>/dev/null | grep -q '"code": *200'; then
            echo "[dev-ready] $name ready: $url (code=200)"
            return 0
        fi
        sleep 2
    done
    echo "[dev-ready] TIMEOUT after ${READY_TIMEOUT}s waiting for $name at $url" >&2
    return 1
}

wait_for_code_200 "service-admin" "http://localhost:$SERVICE_ADMIN_PORT/health"
wait_for_code_200 "api-admin" "http://localhost:$API_ADMIN_PORT/health"
wait_for_http "web-admin" "http://localhost:$WEB_ADMIN_PORT/"

echo "[dev-ready] all services ready"
