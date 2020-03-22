#!/bin/bash
#
# Deploy a jar, source jar, and javadoc jar to Sonatype and/or Bintray

set -e

#
# Deploy the snapshots to Sonatype's repo if the build happens on master
#
SLUG="apollographql/apollo-android"
JDK="openjdk8"
SNAPSHOT_BRANCH="master"

if [ "$TRAVIS_REPO_SLUG" != "$SLUG" ]; then
  echo "Skipping snapshot deployment: wrong repository. Expected '$SLUG' but was '$TRAVIS_REPO_SLUG'."
elif [ "$TRAVIS_JDK_VERSION" != "$JDK" ]; then
  echo "Skipping snapshot deployment: wrong JDK. Expected '$JDK' but was '$TRAVIS_JDK_VERSION'."
elif [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
  echo "Skipping snapshot deployment: was pull request."
elif [ "$TRAVIS_BRANCH" != "$SNAPSHOT_BRANCH" ]; then
  echo "Skipping snapshot deployment: wrong branch. Expected '$SNAPSHOT_BRANCH' but was '$TRAVIS_BRANCH'."
else
  echo "Deploying snapshot..."
  ./gradlew publishAllPublicationsToOssRepository -PSONATYPE_NEXUS_USERNAME="${SONATYPE_NEXUS_USERNAME}" -PSONATYPE_NEXUS_PASSWORD="${SONATYPE_NEXUS_PASSWORD}"
  echo "Snapshot deployed!"
fi

#
# Deploy the release to Bintray if the build happens on a tag
#
if [ "$TRAVIS_TAG" == "" ]; then
  echo "Skipping release deployment: not a tag"
else
  echo "Deploy to bintray..."
  ./gradlew publishAllPublicationsToBintrayRepository -Pbintray.user="${BINTRAY_USER}" -Pbintray.apikey="${BINTRAY_API_KEY}"
  echo "Deployed to bintray!"
  echo "Deploy to Gradle portal..."
  ./gradlew :apollo-gradle-plugin:publishPlugin -Pgradle.publish.key="$GRADLE_PUBLISH_KEY" -Pgradle.publish.secret="$GRADLE_PUBLISH_SECRET"
  echo "Deployed to Gradle portal!"
  echo "Deploy Gradle plugin marker to bintray..."
  # We override the artifact_id for the marker else it is uploaded at the same coordinates as apollo-gradle-plugin
  ./gradlew publishApolloGradlePluginPluginMarkerMavenPublicationToBintrayRepository -Pbintray.user="${BINTRAY_USER}" -Pbintray.apikey="${BINTRAY_API_KEY}" -PPOM_ARTIFACT_ID=com.apollographql.apollo.gradle.plugin
  echo "Deployed Gradle plugin marker to bintray!"
fi

