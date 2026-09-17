#!/usr/bin/env bash

set -Eeuo pipefail

ROOT="/workspace"
RUN_DIR="$ROOT/.run"

BACKEND_PID_FILE="$RUN_DIR/backend.pid"
FRONTEND_PID_FILE="$RUN_DIR/frontend.pid"

START_SCRIPT="$ROOT/scripts/dev-apps-start.sh"

BACKEND_PORT="${HMS_BACKEND_PORT:-8081}"
FRONTEND_PORT="${HMS_FRONTEND_PORT:-3001}"


echo ""
echo "========================================"
echo " HMS Development Restart"
echo "========================================"
echo ""


# ---------------------------------------------------------
# Helper: stop a process group safely
# ---------------------------------------------------------

stop_process() {

    local name="$1"
    local pid_file="$2"

    if [ ! -f "$pid_file" ]; then
        echo "$name PID file does not exist."
        return
    fi

    local pid
    pid="$(cat "$pid_file" 2>/dev/null || true)"

    if [ -z "$pid" ]; then
        echo "$name PID file is empty."
        rm -f "$pid_file"
        return
    fi

    if ! kill -0 "$pid" 2>/dev/null; then
        echo "$name is not running."
        rm -f "$pid_file"
        return
    fi

    echo "Stopping $name..."
    echo "PID: $pid"

    #
    # Applications are started with setsid.
    #
    # Therefore the stored PID normally represents the process-group
    # leader. Stopping the process group also stops child Maven/Java
    # or Node.js processes.
    #
    if ! kill -- "-$pid" 2>/dev/null; then
        kill "$pid" 2>/dev/null || true
    fi

    #
    # Give the application time to shut down cleanly.
    #
    for attempt in $(seq 1 20); do

        if ! kill -0 "$pid" 2>/dev/null; then
            break
        fi

        sleep 0.5
    done

    #
    # Force termination only if graceful shutdown did not work.
    #
    if kill -0 "$pid" 2>/dev/null; then

        echo "$name did not stop gracefully. Forcing shutdown..."

        kill -KILL -- "-$pid" 2>/dev/null \
            || kill -KILL "$pid" 2>/dev/null \
            || true
    fi

    rm -f "$pid_file"

    echo "$name stopped."
}


# ---------------------------------------------------------
# Stop applications
# ---------------------------------------------------------

stop_process \
    "Backend" \
    "$BACKEND_PID_FILE"

echo ""

stop_process \
    "Frontend" \
    "$FRONTEND_PID_FILE"


# ---------------------------------------------------------
# Give ports a moment to become available
# ---------------------------------------------------------

echo ""
echo "Waiting for application ports to be released..."

sleep 2


# ---------------------------------------------------------
# Start applications again
# ---------------------------------------------------------

if [ ! -f "$START_SCRIPT" ]; then

    echo "ERROR: Startup script not found:"
    echo "$START_SCRIPT"
    exit 1
fi


echo ""
echo "Starting HMS applications again..."
echo ""

bash "$START_SCRIPT"


echo ""
echo "========================================"
echo " HMS Development Restart Complete"
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
echo "Logs:"
echo ""
echo "  Backend:"
echo "    tail -f /workspace/.run/backend.log"
echo ""
echo "  Frontend:"
echo "    tail -f /workspace/.run/frontend.log"
echo ""