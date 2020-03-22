#!/usr/bin/env bash

set -e
set -x

export PATH="$ANDROID_HOME"/tools/bin:$PATH

which emulator

sdkmanager --install 'system-images;android-28;default;x86_64' 'emulator'
echo "no" |avdmanager create avd --force -n test -k 'system-images;android-28;default;x86_64'

"$ANDROID_HOME"/emulator/emulator -avd test -no-window -gpu swiftshader_indirect -no-snapshot -noaudio -no-boot-anim &

# theorically, we should wait here for the emulator to boot but compilation is slow enough
# that the emulator will be up by the time we reach the connectedTests. This saves writing
# some bash code and a few seconds of total build time.

./gradlew clean connectedCheck
./gradlew build -x checkstyleTest --stacktrace --max-workers=2
./gradlew -p composite build

./gradlew publishIfNeeded -Pgradle.publish.key="$GRADLE_PUBLISH_KEY" -Pgradle.publish.secret="$GRADLE_PUBLISH_SECRET"
