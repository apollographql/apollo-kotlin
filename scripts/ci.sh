#!/usr/bin/env bash

set -e
set -x

export PATH="$ANDROID_HOME"/tools/bin:$PATH

./gradlew clean build --stacktrace --max-workers=2
# run with -i to debug an issue in Github Actions randomly hanging in :apollo-integration:test
# remove when it's working again
./gradlew -p composite build -i
# check that the public API did not change with Metalava
./gradlew metalavaCheckCompatibility

./gradlew publishSnapshotsIfNeeded  --parallel
