#!/bin/bash
## Set up documentation so README.md is reflected onto apollographql.com/docs/android

set -e

curl -s "https://get.sdkman.io" | bash
source "$HOME"/.sdkman/bin/sdkman-init.sh

sdk install java 11.0.11.hs-adpt
sdk install kotlin 1.5.21

export ANDROID_SDK_ROOT="$HOME"/android
mkdir -p "$ANDROID_SDK_ROOT"/cmdline-tools/

curl https://dl.google.com/android/repository/commandlinetools-linux-6858069_latest.zip > android-commandline-tools.zip
unzip android-commandline-tools.zip
mv cmdline-tools "$ANDROID_SDK_ROOT"/cmdline-tools/latest
export PATH="$ANDROID_SDK_ROOT"/cmdline-tools/latest/bin:$PATH

yes | sdkmanager --install 'patcher;v4'
yes | sdkmanager --install 'platforms;android-30'
yes | sdkmanager --install 'emulator'
yes | sdkmanager --install 'tools'
yes | sdkmanager --install 'build-tools;29.0.2'
yes | sdkmanager --install 'platform-tools'

../gradlew -p ../ dokkaGfm

mkdir -p source/kdoc
cp -rf ../apollo-api/build/dokka/gfm source/kdoc/apollo-api

gatsby build --prefix-paths
mkdir -p docs/android
mv public/* docs/android
mv docs public/
mv public/docs/android/_redirects public