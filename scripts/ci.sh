#!/usr/bin/env bash

set -e

./gradlew clean build connectedCheck -x checkstyleTest --stacktrace --max-workers=2
./gradlew -p composite build

./scripts/deploy.sh