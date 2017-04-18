#!/bin/bash
# converts the apollo-integration into a composite build and substitue the
# module dependencies with the project version of those

# This is used for the integration tests to run against the project version of
# the gradle plugin

cp -fr ../gradle/composite/apollo-integration-build.gradle ../apollo-integration/build.gradle
cp -fr ../gradle/composite/apollo-integration-settings.gradle ../apollo-integration/settings.gradle
cp -fr ../gradle/composite/root-settings.gradle ../settings.gradle

