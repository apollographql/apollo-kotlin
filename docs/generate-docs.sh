#!/bin/bash

set -e

## Install dependencies for dokka
curl -s "https://get.sdkman.io" | bash
source "$HOME"/.sdkman/bin/sdkman-init.sh

sdk install java 11.0.11.hs-adpt

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

## Build the Kdoc
../gradlew -p ../ dokkaGfm

## Install Kotlin to run kscripts
sdk install kotlin 1.5.21

./install_kdoc.main.kts

gatsby build --prefix-paths
mkdir -p docs/android
mv public/* docs/android
mv docs public/
mv public/docs/android/_redirects public