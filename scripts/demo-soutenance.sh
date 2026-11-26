#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:+$JAVA_TOOL_OPTIONS }-Dfakemon.demo.seed=31"
exec ./mvnw "$@" javafx:run
