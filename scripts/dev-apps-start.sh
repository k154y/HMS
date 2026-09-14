#!/usr/bin/env bash

set -Eeuo pipefail


ROOT="/workspace"
WEB_DIR="$ROOT/apps/web"
RUN_DIR="$ROOT/.run"

BACKEND_PID_FILE="$RUN_DIR/backend.pid"
BACKEND_LOG="$RUN_DIR/backend.log"

FRONTEND_PID_FILE="$RUN_DIR/frontend.pid"
FRONTEND_LOG="$RUN_DIR/frontend.log"

FRONTEND_LOCK_HASH_FILE="$RUN_DIR/frontend-package-lock.sha256"


mkdir -p "$RUN_DIR"


echo ""
echo "========================================"
echo " HMS Development Startup"
echo "========================================"


# ---------------------------------------------------------
# Environment verification
# ---------------------------------------------------------

BACKEND_PORT="${HMS_BACKEND_PORT:-8081}"
FRONTEND_PORT="${HMS_FRONTEND_PORT:-3001}"


if [ -z "${JAVA_HOME:-}" ]; then
    echo "ERROR: JAVA_HOME is not configured."
    exit 1
fi


if [ ! -x "$JAVA_HOME/bin/java" ]; then
    echo "ERROR: JAVA_HOME does not point to a valid Java installation:"
    echo "$JAVA_HOME"
    exit 1
fi


if [ -z "${HMS_JWT_SECRET:-}" ]; then
    echo "ERROR: HMS_JWT_SECRET is not available in the container."
    exit 1
fi


echo ""
echo "Java:"
java -version 2>&1 | head -n 1

echo ""
echo "Maven:"
mvn -version | head -n 1

echo ""
echo "Node:"
node --version

echo ""
echo "npm:"
npm --version


# ---------------------------------------------------------
# Infrastructure verification
# ---------------------------------------------------------

echo ""
echo "Checking PostgreSQL..."

for attempt in $(seq 1 60); do

    if pg_isready \
        -h "${HMS_DB_HOST:-postgres}" \
        -p "${HMS_DB_PORT:-5432}" \
        >/dev/null 2>&1; then

        echo "PostgreSQL is ready."
        break
    fi

    if [ "$attempt" -eq 60 ]; then
        echo "ERROR: PostgreSQL did not become ready."
        exit 1
    fi

    sleep 1

done


echo "Checking Redis..."

for attempt in $(seq 1 60); do

    if redis-cli \
        -h "${HMS_REDIS_HOST:-redis}" \
        -p "${HMS_REDIS_PORT:-6379}" \
        ping \
        2>/dev/null \
        | grep -q "PONG"; then

        echo "Redis is ready."
        break
    fi

    if [ "$attempt" -eq 60 ]; then
        echo "ERROR: Redis did not become ready."
        exit 1
    fi

    sleep 1

done


echo "Checking RabbitMQ..."

for attempt in $(seq 1 60); do

    if bash -c \
        "exec 3<>/dev/tcp/${HMS_RABBITMQ_HOST:-rabbitmq}/${HMS_RABBITMQ_PORT:-5672}" \
        >/dev/null 2>&1; then

        echo "RabbitMQ is ready."
        break
    fi

    if [ "$attempt" -eq 60 ]; then
        echo "ERROR: RabbitMQ did not become ready."
        exit 1
    fi

    sleep 1

done


# ---------------------------------------------------------
# Helper
# ---------------------------------------------------------

process_is_running() {

    local pid_file="$1"

    if [ ! -f "$pid_file" ]; then
        return 1
    fi

    local pid
    pid="$(cat "$pid_file")"

    if [ -z "$pid" ]; then
        return 1
    fi

    kill -0 "$pid" 2>/dev/null

}


# ---------------------------------------------------------
# Backend
# ---------------------------------------------------------

if process_is_running "$BACKEND_PID_FILE"; then

    echo ""
    echo "Backend is already running."
    echo "PID: $(cat "$BACKEND_PID_FILE")"

else

    rm -f "$BACKEND_PID_FILE"

    echo ""
    echo "Starting Spring Boot backend on port $BACKEND_PORT..."

    cd "$ROOT"

    nohup setsid \
        make backend-run \
        > "$BACKEND_LOG" \
        2>&1 \
        < /dev/null &

    BACKEND_PID=$!

    echo "$BACKEND_PID" > "$BACKEND_PID_FILE"

    echo "Backend PID: $BACKEND_PID"
    echo "Backend log: $BACKEND_LOG"

fi


# ---------------------------------------------------------
# Frontend dependencies
# ---------------------------------------------------------

cd "$WEB_DIR"


if [ -f package-lock.json ]; then

    CURRENT_LOCK_HASH="$(
        sha256sum package-lock.json \
        | awk '{print $1}'
    )"

    PREVIOUS_LOCK_HASH="$(
        cat "$FRONTEND_LOCK_HASH_FILE" \
        2>/dev/null \
        || true
    )"


    if [ ! -d node_modules ] \
        || [ "$CURRENT_LOCK_HASH" != "$PREVIOUS_LOCK_HASH" ]; then

        echo ""
        echo "Installing/updating frontend dependencies..."

        npm install

        echo "$CURRENT_LOCK_HASH" \
            > "$FRONTEND_LOCK_HASH_FILE"

    fi

elif [ ! -d node_modules ]; then

    echo ""
    echo "Installing frontend dependencies..."

    npm install

fi


# ---------------------------------------------------------
# Frontend
# ---------------------------------------------------------

if process_is_running "$FRONTEND_PID_FILE"; then

    echo ""
    echo "Frontend is already running."
    echo "PID: $(cat "$FRONTEND_PID_FILE")"

else

    rm -f "$FRONTEND_PID_FILE"

    echo ""
    echo "Starting Next.js frontend on port $FRONTEND_PORT..."

    cd "$WEB_DIR"

    nohup setsid \
        npm run dev -- \
        --hostname 0.0.0.0 \
        --port "$FRONTEND_PORT" \
        > "$FRONTEND_LOG" \
        2>&1 \
        < /dev/null &

    FRONTEND_PID=$!

    echo "$FRONTEND_PID" > "$FRONTEND_PID_FILE"

    echo "Frontend PID: $FRONTEND_PID"
    echo "Frontend log: $FRONTEND_LOG"

fi


echo ""
echo "========================================"
echo " HMS Development Applications Started"
echo "========================================"
echo ""
echo "Frontend:"
echo "  http://localhost:$FRONTEND_PORT"
echo ""
echo "Backend:"
echo "  http://localhost:$BACKEND_PORT"
echo ""
echo "Backend health:"
echo "  http://localhost:$BACKEND_PORT/actuator/health"
echo ""
echo "RabbitMQ Management:"
echo "  http://localhost:15672"
echo ""
echo "Logs:"
echo ""
echo "  Backend:"
echo "    tail -f /workspace/.run/backend.log"
echo ""
echo "  Frontend:"
echo "    tail -f /workspace/.run/frontend.log"
echo ""