#!/usr/bin/env sh
# Runs the given Maven goals against the project. Uses a local JDK 25 if one is on
# the PATH; otherwise falls back to the maven:3.9-eclipse-temurin-25 image so the
# git hooks work identically on a machine without JDK 25 installed (TESTCONTAINERS_*
# vars only matter for goals that touch Testcontainers, e.g. "verify").
set -e

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if command -v mvn >/dev/null 2>&1 && mvn -version 2>/dev/null | grep -q "Java version: 25"; then
    (cd "$REPO_ROOT" && mvn -B "$@")
else
    HOST_PATH="$(cd "$REPO_ROOT" && pwd -W 2>/dev/null || pwd)"
    echo "JDK 25 non detecte en local : execution via Docker (maven:3.9-eclipse-temurin-25)"
    MSYS_NO_PATHCONV=1 docker run --rm \
        -v "${HOST_PATH}:/build" \
        -v /var/run/docker.sock:/var/run/docker.sock \
        -v datacom-maven-cache:/root/.m2 \
        -e TESTCONTAINERS_RYUK_DISABLED=true \
        -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
        -w /build \
        maven:3.9-eclipse-temurin-25 \
        mvn -B "$@"
fi
