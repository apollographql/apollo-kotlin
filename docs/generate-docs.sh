#!/bin/bash
## Set up documentation so README.md is reflected onto apollographql.com/docs/android

set -e

gatsby build --prefix-paths
mkdir -p docs/android
mv public/* docs/android
mv docs public/
mv public/docs/android/_redirects public