#!/bin/bash
## Set up documentation so README.md is reflected onto apollographql.com/docs/android

set -e

echo -e "---\ntitle: Introduction\ndescription: A strongly-typed, caching GraphQL client for the JVM, Android and Kotlin multiplatform\n---\n\n$(cat ../README.md | grep -v 'the official docs')" > source/index.md

gatsby build --prefix-paths
mkdir -p docs/android
mv public/* docs/android
mv docs public/
mv public/docs/android/_redirects public