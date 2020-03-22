#!/usr/bin/env bash

set -e
set -x

env

android create avd --force -n test -t android-22 --abi armeabi-v7a
emulator -avd test -no-audio -no-window &

./gradlew clean build connectedCheck -x checkstyleTest --stacktrace --max-workers=2
./gradlew -p composite build

./gradlew publishIfNeeded -Pgradle.publish.key="$GRADLE_PUBLISH_KEY" -Pgradle.publish.secret="$GRADLE_PUBLISH_SECRET"
