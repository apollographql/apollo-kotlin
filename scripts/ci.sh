#!/usr/bin/env bash

set -e
set -x

export PATH="$ANDROID_HOME"/tools/bin:$PATH

# Workaround for `Process 'Resolving NPM dependencies using yarn' returns 137`
# Looks like starting node takes too many resources and fails give it its own self-contained
# Gradle instance
# See also https://youtrack.jetbrains.com/issue/KT-47215#focus=Comments-27-5298779.0-0
./gradlew  kotlinNpmCachesSetup kotlinNpmInstall
./gradlew --stop

./gradlew -p tests fullCheck

# check that the public API did not change with Metalava
# reenable when the 3.x API is more stable
# ./gradlew metalavaCheckCompatibility

./gradlew publishSnapshotsIfNeeded  --parallel

./gradlew publishToOssStagingIfNeeded
./gradlew publishToGradlePortalIfNeeded -Pgradle.publish.key="$GRADLE_PUBLISH_KEY" -Pgradle.publish.secret="$GRADLE_PUBLISH_SECRET"