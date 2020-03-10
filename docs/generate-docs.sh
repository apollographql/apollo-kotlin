#!/bin/bash
## Set up documentation so README.md is reflected onto apollographql.com/docs/android

set -e

echo -e "---\ntitle: Get started\ndescription: Learn how to quickly set up Apollo Client in your Android app\n---\n\n$(cat ../README.md | grep -v 'the official docs')" > source/essentials/get-started.md

gatsby build --prefix-paths
mkdir -p docs/android
mv public/* docs/android
mv docs public/
mv public/docs/android/_redirects public