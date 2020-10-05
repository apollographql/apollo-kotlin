#!/usr/bin/env bash

set -e
set -x

export PATH="$ANDROID_HOME"/tools/bin:$PATH

./gradlew clean build --stacktrace --max-workers=2
./gradlew -p composite build
# check that the public API did not change with Metalava
# reenable when the 3.x API is more stable
# ./gradlew checkMetalava

./gradlew publishSnapshotsIfNeeded  --parallel
