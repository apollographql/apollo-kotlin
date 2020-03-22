#!/usr/bin/env bash

set -e
set -x

env
#find "$ANDROID_HOME"/tools

export PATH="$ANDROID_HOME"/tools/bin:$PATH

sdkmanager --install 'system-images;android-22;default;armeabi-v7a' 'emulator'
echo "no" |avdmanager create avd --force -n test -k 'system-images;android-22;default;armeabi-v7a'
emulator -avd test -no-audio -no-window &

./gradlew clean build connectedCheck -x checkstyleTest --stacktrace --max-workers=2
./gradlew -p composite build

./gradlew publishIfNeeded -Pgradle.publish.key="$GRADLE_PUBLISH_KEY" -Pgradle.publish.secret="$GRADLE_PUBLISH_SECRET"
