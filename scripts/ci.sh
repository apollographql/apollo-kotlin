#!/usr/bin/env bash

set -e
set -x

export PATH="$ANDROID_HOME"/tools/bin:$PATH

./gradlew -p tests appleTest

