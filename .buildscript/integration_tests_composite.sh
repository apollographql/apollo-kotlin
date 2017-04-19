#!/bin/bash
# converts the apollo-integration into a composite build and substitue the
# module dependencies with the project version of those

# This is used for the integration tests to run against the project version of
# the gradle plugin

cd "${0%/*}" && cd ..

# backup old build files
echo "Backing up build scripts..."
mv apollo-integration/build.gradle gradle/composite/apollo-integration-build-backup.gradle
mv settings.gradle gradle/composite/root-settings-backup.gradle

# switch apollo-integration to a composite build
echo "Converting apollo-integration to a composite build..."
cp gradle/composite/apollo-integration-build.gradle apollo-integration/build.gradle
cp gradle/composite/apollo-integration-settings.gradle apollo-integration/settings.gradle
cp gradle/composite/root-settings.gradle settings.gradle

echo "Running integration tests"
./gradlew -p apollo-integration build

# restore files
echo "Restoring old build scripts"
mv gradle/composite/root-settings-backup.gradle settings.gradle
mv gradle/composite/apollo-integration-build-backup.gradle apollo-integration/build.gradle
rm apollo-integration/settings.gradle
