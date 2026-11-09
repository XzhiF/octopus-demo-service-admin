#!/usr/bin/env bash
# Stops the processes started by dev-up.sh and shuts down the MySQL container (data volume kept).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_DIR="$(dirname "$SCRIPT_DIR")"
RUN_DIR="$SCRIPT_DIR/.run"

SERVICE_ADMIN_PORT="${SERVICE_ADMIN_PORT:-18080}"
API_ADMIN_PORT="${API_ADMIN_PORT:-18081}"
WEB_ADMIN_PORT="${WEB_ADMIN_PORT:-13000}"

stop_pid() {
    local name="$1"
    local file="$RUN_DIR/$name.pid"
    if [[ ! -f "$file" ]]; then
        return 0
    fi
    local pid
    pid="$(cat "$file")"
    if [[ -n "$pid" ]]; then
        if command -v taskkill >/dev/null 2>&1; then
            taskkill //PID "$pid" //T //F >/dev/null 2>&1 || true
        else
            kill "$pid" 2>/dev/null || true
        fi
    fi
    rm -f "$file"
}

# MSYS/Git-Bash $! values are not Windows PIDs, so also reclaim the listening ports.
kill_port() {
    local name="$1" port="$2" pids pid
    if command -v taskkill >/dev/null 2>&1; then
        pids="$(netstat -ano | grep -E "[:.]${port}[[:space:]]" | grep -i LISTENING | awk '{print $NF}' | sort -u || true)"
        for pid in $pids; do
            [[ -n "$pid" && "$pid" != "0" ]] || continue
            taskkill //PID "$pid" //T //F >/dev/null 2>&1 || true
        done
    elif command -v lsof >/dev/null 2>&1; then
        pids="$(lsof -ti tcp:"$port" 2>/dev/null || true)"
        for pid in $pids; do kill "$pid" 2>/dev/null || true; done
    elif command -v fuser >/dev/null 2>&1; then
        fuser -k "$port"/tcp >/dev/null 2>&1 || true
    fi
    echo "[dev-down] $name port $port released"
}

stop_pid service-admin
stop_pid api-admin
stop_pid web-admin

kill_port service-admin "$SERVICE_ADMIN_PORT"
kill_port api-admin "$API_ADMIN_PORT"
kill_port web-admin "$WEB_ADMIN_PORT"

echo "[dev-down] stopping MySQL container (volumes kept)"
( cd "$SERVICE_DIR" && docker compose down ) || echo "[dev-down] WARN: docker compose down failed" >&2

echo "[dev-down] done"
