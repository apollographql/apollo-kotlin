#!/usr/bin/env bash

set -e
set -x

export PATH="$ANDROID_HOME"/tools/bin:$PATH

./gradlew clean build --stacktrace --max-workers=2
./gradlew -p composite build
# check that the public API did not change with Metalava
# apollo-compiler is for now considered an internal artifact consumed by the Gradle plugin so we allow API changes there.
# apollo-runtime-kotlin is still under development. Include the check once it is stable enough.
./gradlew checkMetalava -x apollo-compiler:checkMetalava -x apollo-runtime-kotlin:checkMetalava

./gradlew publishIfNeeded -Pgradle.publish.key="$GRADLE_PUBLISH_KEY" -Pgradle.publish.secret="$GRADLE_PUBLISH_SECRET" --parallel
# this is a separate task because sonatype does not support --parallel
./gradlew publishToOssStagingIfNeeded
