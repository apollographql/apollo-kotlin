#!/bin/bash
## Set up documentation so README.md is reflected onto apollographql.com/docs/android

set -e

curl -s "https://get.sdkman.io" | bash
source ~/.sdkman/bin/sdkman-init.sh

sdk install java 11.0.11.hs-adpt
sdk install kotlin 1.5.21

../gradlew dokkaGfm

gatsby build --prefix-paths
mkdir -p docs/android
mv public/* docs/android
mv docs public/
mv public/docs/android/_redirects public